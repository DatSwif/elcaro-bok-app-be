package pl.elcaro.elcarobokapp.domain.payments

enum class InvoiceStatus {
    UNPAID,
    PAID,
    PENDING,
    OVERDUE,
    CANCELLED,
}
