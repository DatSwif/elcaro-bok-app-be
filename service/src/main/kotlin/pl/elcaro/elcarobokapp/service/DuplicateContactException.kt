package pl.elcaro.elcarobokapp.service

enum class ContactField { PHONE, EMAIL }

class DuplicateContactException(
    val field: ContactField,
    val value: String,
) : RuntimeException("$field '$value' is already in use")
