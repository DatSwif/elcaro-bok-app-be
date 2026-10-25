package pl.elcaro.elcarobokapp.api

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.servlet.LocaleResolver
import pl.elcaro.elcarobokapp.service.ClientService
import java.util.*

class UserLanguageLocaleResolver(
    private val clientService: ClientService,
) : LocaleResolver {

    override fun resolveLocale(request: HttpServletRequest): Locale {
        if (request.getHeader("Accept-Language") != null) {
            return request.locale
        }
        val clientNumber = SecurityContextHolder.getContext().authentication?.name
        if (clientNumber != null) {
            val languageCode = clientService.getLanguageCode(clientNumber)
            if (languageCode != null) return Locale.forLanguageTag(languageCode)
        }
        return Locale.ENGLISH
    }

    override fun setLocale(request: HttpServletRequest, response: HttpServletResponse?, locale: Locale?) {
        throw UnsupportedOperationException("UserLanguageLocaleResolver does not support locale changes")
    }
}
