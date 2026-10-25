package pl.elcaro.elcarobokapp.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.elcaro.elcarobokapp.domain.registration.*
import pl.elcaro.elcarobokapp.infrastructure.ClientPhoneRepository
import pl.elcaro.elcarobokapp.infrastructure.ClientRepository
import pl.elcaro.elcarobokapp.infrastructure.ConsentTypeRepository
import pl.elcaro.elcarobokapp.infrastructure.UserConsentRepository
import java.security.SecureRandom
import java.time.Instant

private const val MAX_FAILED_ATTEMPTS = 5 // TODO - temporary placeholder, to be adjusted later
private const val LOCK_DURATION_SECONDS = 180L * 60
private const val CODE_EXPIRATION_SECONDS = 5L * 60
private const val CODE_LENGTH = 4
private val DEV_TEST_LOGINS = mapOf(
    "C001" to "5555",
    "C002" to "6666",
    "C003" to "7777",
)

class AuthenticationException(message: String) : RuntimeException(message)
class AccountLockedException(message: String) : RuntimeException(message)

class InvalidOtpException(message: String) : RuntimeException(message)

private const val MARKETING_CONSENT_CODE = "MARKETING"

@Service
class AuthenticationService(
    private val clientRepository: ClientRepository,
    private val clientPhoneRepository: ClientPhoneRepository,
    private val consentTypeRepository: ConsentTypeRepository,
    private val userConsentRepository: UserConsentRepository,
    private val jwtTokenProvider: JwtTokenProvider,
    private val smsService: SmsService,
    @Value($$"${app.dev.test-login.enabled:false}") private val devTestLoginEnabled: Boolean = false,
) {
    private val log = LoggerFactory.getLogger(AuthenticationService::class.java)
    private val secureRandom = SecureRandom()

    @Transactional
    fun requestCode(request: LoginRequest) {
        val clientPhone = clientPhoneRepository.findByPhone(request.phoneNumber) ?: return
        checkAccountLock(clientPhone.client)
        generateAndSaveCode(clientPhone)
    }

    @Transactional
    fun requestOtp(phoneNumber: String) {
        val clientPhone = clientPhoneRepository.findByPhone(phoneNumber) ?: return
        checkAccountLock(clientPhone.client)
        generateAndSaveCode(clientPhone)
    }

    @Transactional(noRollbackFor = [AuthenticationException::class])
    fun verifyCode(request: VerifyCodeRequest): LoginResponse {
        val client = consumeValidCode(request)

        client.failedLoginCount = 0
        client.lockedUntil = null
        if (client.status == ClientStatus.NEW) {
            client.status = ClientStatus.ACTIVE
        }
        clientRepository.save(client)

        val token = jwtTokenProvider.generateToken(client.clientNumber!!)
        return LoginResponse(token = token)
    }

    @Transactional(noRollbackFor = [InvalidOtpException::class])
    fun verifyOtp(request: VerifyCodeRequest) {
        try {
            consumeValidCode(request)
        } catch (e: AuthenticationException) {
            throw InvalidOtpException("Invalid verification code")
        }
    }

    private fun consumeValidCode(request: VerifyCodeRequest): Client {
        val clientPhone = clientPhoneRepository.findByPhone(request.phoneNumber)
            ?: throw AuthenticationException("Invalid credentials")

        val client = clientPhone.client
        checkAccountLock(client)
        val now = Instant.now()

        val isDevTestLogin = devTestLoginEnabled && DEV_TEST_LOGINS[client.clientNumber] == request.code
        if (!isDevTestLogin) {
            if (clientPhone.code == null || clientPhone.code != request.code || clientPhone.expiresAt.isBefore(now)) {
                client.failedLoginCount += 1
                if (client.failedLoginCount >= MAX_FAILED_ATTEMPTS) {
                    client.lockedUntil = now.plusSeconds(LOCK_DURATION_SECONDS)
                    client.failedLoginCount = 0
                }
                clientRepository.save(client)
                throw AuthenticationException("Invalid credentials")
            }
        }

        clientPhone.code = null
        clientPhoneRepository.save(clientPhone)
        return client
    }

    @Transactional(readOnly = true)
    fun findCodeForPhone(phoneNumber: String): String? {
        if (!devTestLoginEnabled) return null
        return clientPhoneRepository.findByPhone(phoneNumber)?.code
    }

    @Transactional
    fun requestRegistrationCode(request: RegisterRequest) {
        val client = clientRepository.findByClientNumberAndPhonesPhoneAndEmailsEmail(
            request.clientNumber, request.phoneNumber, request.email
        ) ?: return

        checkAccountLock(client)
        if (client.status != ClientStatus.NEW) return

        saveMarketingConsent(client, request.marketingConsent)

        val clientPhone = client.phones.first { it.phone == request.phoneNumber }
        generateAndSaveCode(clientPhone)
    }

    private fun saveMarketingConsent(client: Client, isGranted: Boolean) {
        val consentType = consentTypeRepository.findByTranslationsCode(MARKETING_CONSENT_CODE) ?: run {
            log.warn("Consent type '{}' not found, skipping marketing consent", MARKETING_CONSENT_CODE)
            return
        }

        val consentId = UserConsentId(clientId = client.id, consentTypeId = consentType.id)
        val consent = userConsentRepository.findById(consentId).orElse(null)
            ?.apply {
                this.isGranted = isGranted
                this.grantedAt = Instant.now()
            }
            ?: UserConsent(
                id = consentId,
                client = client,
                consentType = consentType,
                isGranted = isGranted,
                grantedAt = Instant.now(),
            )

        userConsentRepository.save(consent)
    }

    private fun checkAccountLock(client: Client) {
        if (client.lockedUntil != null && client.lockedUntil!!.isAfter(Instant.now())) {
            throw AccountLockedException("Account locked until ${client.lockedUntil}")
        }
    }

    private fun generateAndSaveCode(clientPhone: ClientPhone) {
        val code = generateCode()
        clientPhone.code = code
        clientPhone.expiresAt = Instant.now().plusSeconds(CODE_EXPIRATION_SECONDS)
        clientPhoneRepository.save(clientPhone)
        log.info("OTP code for phone {}: {}", clientPhone.phone, code)
        smsService.sendOtp(clientPhone.phone, code)
    }

    private fun generateCode(): String {
        val bound = 10_000
        val number = secureRandom.nextInt(bound)
        return number.toString().padStart(CODE_LENGTH, '0')
    }
}
