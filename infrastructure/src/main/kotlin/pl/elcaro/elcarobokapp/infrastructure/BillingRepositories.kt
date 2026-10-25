package pl.elcaro.elcarobokapp.infrastructure

import org.springframework.data.repository.CrudRepository
import pl.elcaro.elcarobokapp.domain.payments.Invoice
import pl.elcaro.elcarobokapp.domain.payments.Payment
import pl.elcaro.elcarobokapp.domain.payments.Refund

interface InvoiceRepository : CrudRepository<Invoice, Long> {
    fun findAllByClientClientNumber(clientNumber: String): List<Invoice>
    fun findByIdAndClientClientNumber(id: Long, clientNumber: String): Invoice?
}

interface PaymentRepository : CrudRepository<Payment, Long> {
    fun findAllByClientClientNumber(clientNumber: String): List<Payment>
}

interface RefundRepository : CrudRepository<Refund, Long>
