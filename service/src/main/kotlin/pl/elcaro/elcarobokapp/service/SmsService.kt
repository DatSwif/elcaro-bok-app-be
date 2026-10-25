package pl.elcaro.elcarobokapp.service

import com.twilio.Twilio
import com.twilio.rest.api.v2010.account.Message
import com.twilio.type.PhoneNumber
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class SmsService(
    @Value($$"${twilio.account-sid:}") private val accountSid: String,
    @Value($$"${twilio.auth-token:}") private val authToken: String,
    @Value($$"${twilio.from-number:}") private val fromNumber: String,
    @Value($$"${app.sms.enabled:false}") private val smsEnabled: Boolean = false,
) {
    private val log = LoggerFactory.getLogger(SmsService::class.java)

    fun sendOtp(toPhoneNumber: String, code: String) {
        if (!smsEnabled) {
            log.debug("SMS sending disabled; skipping OTP SMS to {}", toPhoneNumber)
            return
        }

        if (accountSid.isBlank() || authToken.isBlank() || fromNumber.isBlank()) {
            log.warn("Twilio not configured (account SID, auth token or from number missing); skipping OTP SMS to {}", toPhoneNumber)
            return
        }

        try {
            Twilio.init(accountSid, authToken)
            val message = Message.creator(
                PhoneNumber(toPhoneNumber),
                PhoneNumber(fromNumber),
                "Your Elcaro verification code is: $code",
            ).create()
            log.info("OTP SMS sent to {} (Twilio message SID {})", toPhoneNumber, message.sid)
        } catch (e: Exception) {
            log.error("Failed to send OTP SMS to {}: {}", toPhoneNumber, e.message, e)
        }
    }
}
