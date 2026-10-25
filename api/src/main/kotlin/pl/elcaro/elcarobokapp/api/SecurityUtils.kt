package pl.elcaro.elcarobokapp.api

import org.springframework.security.core.context.SecurityContextHolder

fun authenticatedClientNumber(): String =
    SecurityContextHolder.getContext().authentication?.name
        ?: throw IllegalStateException("No authenticated user in security context")
