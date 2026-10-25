package pl.elcaro.elcarobokapp.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service

@Service
class EmailService(
    private val mailSenderProvider: ObjectProvider<JavaMailSender>,
    @Value($$"${app.email.from:}") private val fromAddress: String,
    @Value($$"${app.email.enabled:false}") private val emailEnabled: Boolean = false,
) {
    private val log = LoggerFactory.getLogger(EmailService::class.java)

    fun sendDummyEmail(toEmail: String) {
        if (!emailEnabled) {
            log.debug("Email sending disabled; skipping dummy email to {}", toEmail)
            return
        }

        val mailSender = mailSenderProvider.ifAvailable
        if (mailSender == null) {
            log.warn("No JavaMailSender configured; skipping dummy email to {}", toEmail)
            return
        }

        try {
            val message = SimpleMailMessage().apply {
                from = fromAddress
                setTo(toEmail)
                subject = "Elcaro – test notification"
                text = "This is a dummy notification email from Elcaro. No action is required."
            }
            mailSender.send(message)
            log.info("Dummy email sent to {}", toEmail)
        } catch (e: Exception) {
            log.error("Failed to send dummy email to {}: {}", toEmail, e.message, e)
        }
    }
}
