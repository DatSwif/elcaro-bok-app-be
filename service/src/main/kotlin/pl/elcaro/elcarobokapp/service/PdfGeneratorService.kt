package pl.elcaro.elcarobokapp.service

import com.lowagie.text.Document
import com.lowagie.text.Element
import com.lowagie.text.Font
import com.lowagie.text.Paragraph
import com.lowagie.text.Phrase
import com.lowagie.text.html.simpleparser.HTMLWorker
import com.lowagie.text.pdf.PdfPCell
import com.lowagie.text.pdf.PdfPTable
import com.lowagie.text.pdf.PdfWriter
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.io.StringReader
import java.util.Locale

@Service
class PdfGeneratorService {

    fun generateInvoicePdf(invoice: InvoiceDetailResponse, locale: Locale): ByteArray {
        val outputStream = ByteArrayOutputStream()
        val document = Document()
        PdfWriter.getInstance(document, outputStream)
        document.open()

        val titleFont = Font(Font.HELVETICA, 18f, Font.BOLD)
        val boldFont = Font(Font.HELVETICA, 12f, Font.BOLD)

        val isEn = locale.language == "en"
        val tFaktura = if (isEn) "Invoice:" else "Faktura:"
        val tIssueDate = if (isEn) "Issue Date:" else "Data wystawienia:"
        val tDueDate = if (isEn) "Due Date:" else "Termin płatności:"
        val tSeller = if (isEn) "Seller:" else "Sprzedawca:"
        val tBuyer = if (isEn) "Buyer:" else "Nabywca:"
        val tDesc = if (isEn) "Description" else "Opis"
        val tQty = if (isEn) "Qty" else "Ilość"
        val tGross = if (isEn) "Gross Price" else "Kwota brutto"
        val tNetTotal = if (isEn) "Total Net:" else "Kwota netto:"
        val tGrossTotal = if (isEn) "Total Gross:" else "Kwota brutto:"
        val tPayMethod = if (isEn) "Payment Method:" else "Metoda płatności:"
        val tBankAcc = if (isEn) "Bank Account:" else "Konto bankowe:"

        document.add(Paragraph("$tFaktura ${invoice.invoiceNumber}", titleFont))
        document.add(Paragraph("Status: ${invoice.status}"))
        document.add(Paragraph("$tIssueDate ${invoice.dates.issueDate}"))
        document.add(Paragraph("$tDueDate ${invoice.dates.dueDate}"))
        document.add(Paragraph("\n"))

        val sellerBuyerTable = PdfPTable(2)
        sellerBuyerTable.widthPercentage = 100f
        sellerBuyerTable.addCell(createNoBorderCell("$tSeller\n${invoice.seller.name}\n${invoice.seller.address}\nNIP: ${invoice.seller.nip}"))
        sellerBuyerTable.addCell(createNoBorderCell("$tBuyer\n${invoice.buyer.name}\n${invoice.buyer.address}"))
        document.add(sellerBuyerTable)
        document.add(Paragraph("\n"))

        val itemsTable = PdfPTable(4)
        itemsTable.widthPercentage = 100f
        itemsTable.setWidths(floatArrayOf(4f, 1f, 1f, 1.5f))

        itemsTable.addCell(PdfPCell(Phrase(tDesc, boldFont)))
        itemsTable.addCell(PdfPCell(Phrase(tQty, boldFont)))
        itemsTable.addCell(PdfPCell(Phrase("VAT", boldFont)))
        itemsTable.addCell(PdfPCell(Phrase(tGross, boldFont)))

        invoice.lineItems.forEach {
            itemsTable.addCell(it.description)
            itemsTable.addCell("${it.quantity} ${it.unit}")
            itemsTable.addCell("${it.vatRate}%")
            itemsTable.addCell("${it.grossPrice} PLN")
        }
        document.add(itemsTable)
        document.add(Paragraph("\n"))

        val totalsTable = PdfPTable(2)
        totalsTable.horizontalAlignment = Element.ALIGN_RIGHT
        totalsTable.widthPercentage = 50f
        totalsTable.addCell(PdfPCell(Phrase(tNetTotal, boldFont)))
        totalsTable.addCell("${invoice.totals.totalNet} PLN")
        totalsTable.addCell(PdfPCell(Phrase(tGrossTotal, boldFont)))
        totalsTable.addCell("${invoice.totals.totalGross} PLN")
        document.add(totalsTable)
        document.add(Paragraph("\n"))

        document.add(Paragraph("$tPayMethod ${invoice.paymentDetails.method}"))
        document.add(Paragraph("$tBankAcc ${invoice.paymentDetails.bankAccount}"))

        document.close()
        return outputStream.toByteArray()
    }

    private fun createNoBorderCell(text: String): PdfPCell {
        val cell = PdfPCell(Phrase(text))
        cell.border = PdfPCell.NO_BORDER
        return cell
    }

    fun generateDocumentPdf(title: String, htmlContent: String): ByteArray {
        val outputStream = ByteArrayOutputStream()
        val document = Document()
        PdfWriter.getInstance(document, outputStream)
        document.open()

        val titleFont = Font(Font.HELVETICA, 16f, Font.BOLD)
        document.add(Paragraph(title, titleFont))
        document.add(Paragraph("\n"))

        try {
            val wrappedHtml = "<html><body>$htmlContent</body></html>"
            val elements = HTMLWorker.parseToList(StringReader(wrappedHtml), null)

            if (elements.isEmpty()) {
                throw RuntimeException("HTML Parsing returned empty elements")
            }
            for (element in elements) {
                document.add(element as Element)
            }
        } catch (e: Exception) {
            val cleanText = htmlContent
                .replace(Regex("<h3>(.*?)</h3>", RegexOption.IGNORE_CASE), "\n$1\n")
                .replace(Regex("<p>(.*?)</p>", RegexOption.IGNORE_CASE), "$1\n")
                .replace(Regex("<li>(.*?)</li>", RegexOption.IGNORE_CASE), "• $1\n")
                .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
                .replace(Regex("<[^>]*>"), "")
                .replace(Regex("\n{3,}"), "\n\n")
                .trim()

            document.add(Paragraph(cleanText))
        }

        document.close()
        return outputStream.toByteArray()
    }
}