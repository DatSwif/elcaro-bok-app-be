package pl.elcaro.elcarobokapp.service

import org.springframework.stereotype.Service
import java.util.*

@Service
class TermsService {

    fun getTerms(locale: Locale): List<TermResponse> {
        // Mock response matching the JSON array in api.json
        return listOf(
            TermResponse(
                title = "Document 1",
                content = "Contents of Document 1 from the mock server\r\nSecond line of Document 1"
            ),
            TermResponse(
                title = "Document 2",
                content = "Contents of Document 2 from the mock server"
            )
        )
    }
}
