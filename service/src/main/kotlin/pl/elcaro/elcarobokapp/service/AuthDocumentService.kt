package pl.elcaro.elcarobokapp.service

import org.springframework.stereotype.Service
import java.util.*

@Service
class AuthDocumentService {

    fun getTermsOfService(locale: Locale): DocumentContentResponse {
        val title = if (locale.language == "pl") "Regulamin rejestracji" else "Registration terms"
        val text = if (locale.language == "pl") "<h3>Regulamin</h3><p>Warunki korzystania z serwisu ebok...</p>" else "<h3>Terms</h3><p>Terms of service for ebok...</p>"
        return DocumentContentResponse("t1", title, text, "terms")
    }

    fun getPrivacyPolicy(locale: Locale): DocumentContentResponse {
        val title = if (locale.language == "pl") "Polityka prywatności rejestracji" else "Registration privacy policy"
        val text = if (locale.language == "pl") "<h3>Polityka prywatności</h3><p>Twoje dane będą przetwarzane zgodnie z...</p>" else "<h3>Privacy policy</h3><p>Your data will be processed according to...</p>"
        return DocumentContentResponse("p1", title, text, "privacy")
    }

    fun getMarketingConsents(locale: Locale): DocumentContentResponse {
        val title = if (locale.language == "pl") "Zgody marketingowe" else "Marketing consents"
        val text = if (locale.language == "pl") "<h3>Zgody marketingowe</h3><p>Wyrażam zgodę na komunikację marketingową...</p>" else "<h3>Marketing consents</h3><p>I consent to marketing communication...</p>"
        return DocumentContentResponse("m1", title, text, "marketing-consents")
    }

    fun getPromotionsRegulations(locale: Locale): DocumentContentResponse {
        val title = if (locale.language == "pl") "Regulamin promocji \"Oferta na bloki_lato 2024\"" else "Promotion terms \"Blocks offer_summer 2024\""
        val text = if (locale.language == "pl") "<h3>Regulamin promocji</h3><p>Opis szczegółowy regulaminu promocji dla pakietów usług telekomunikacyjnych.</p><p>Warunki promocji obowiązują zgodnie z zasadami określonymi w umowie oraz niniejszym regulaminie.</p>" else "<h3>Promotion terms</h3><p>Detailed description of promotion terms for telecommunication services packages.</p>"
        return DocumentContentResponse("d3", title, text, "telecommunications-services")
    }

    fun getDocumentContentById(id: String, locale: Locale): DocumentContentResponse {
        val text = when (id) {
            "d1", "d3" -> "<h3>I. KORZYŚCI PROMOCJI</h3><ol><li>Obniżone opłaty za usługi telekomunikacyjne w każdym Okresie Rozliczeniowym w Okresie Promocji (przez okres nie dłuższy niż 24 miesiące), tj. nabycie usług telekomunikacyjnych na preferencyjnych warunkach (z rabatem określonym w niniejszym Regulaminie Promocji).</li><li>Obniżone Opłaty Jednorazowe w przypadku aktywacji usług telekomunikacyjnych.</li><li>Otrzymanie dodatkowych rabatów za wyrażenie wszystkich Zgód marketingowych (rabat 5 zł miesięcznie) oraz wyrażenie zgody na e-Fakturę (rabat 5 zł miesięcznie) przyznawanych zgodnie z odrębnym Regulaminem Promocji „Zgody marketingowe/e faktura”.</li></ol><h3>II. DEFINICJE</h3><ol><li>Organizator Promocji – ELCARO Spółka z ograniczoną odpowiedzialnością z siedzibą w Warszawie (03-140), przy ul. Odkrytej 1A, wpisana pod numerem 0000623333 do rejestru przedsiębiorców KRS, prowadzonego przez Sąd Rejonowy dla m. st. Warszawy w Warszawie, XIII Wydział Gospodarczy KRS, której nadano NIP: 5242803567 oraz REGON: 364717322;</li><li>Abonent – nowy klient oraz pełnoletnia osoba fizyczna, która łącznie spełnia następujące warunki:<ol type=\"a\"><li>w okresie 3 (słownie: trzech) miesięcy przed Przystąpieniem do promocji nie był Abonentem Organizatora Promocji w zakresie jakiekolwiek z Usług stacjonarnych,</li><li>nie zalega względem Organizatora Promocji z płatnościami z jakiegokolwiek tytułu,</li><li>przystępuje do Promocji w celach nie związanych z działalnością gospodarczą;</li></ol></li><li>Promocja – kampania promocyjna prowadzona przez Organizatora Promocji pod hasłem „Oferta na bloki_lato 2024”, stanowiąca warunki, zasady nabywania i świadczenia Usług określonych w niniejszym Regulaminie Promocji.</li><li>Korzyści – korzyści o których mowa w pkt. I Regulaminu Promocji;</li><li>Umowa – Umowa o świadczenie Usług przez Organizatora Promocji zawarta na warunkach promocyjnych, na czas określony 24 miesięcy dla usług oferowanych przez Organizatora Promocji w niniejszym Regulaminie Promocji;</li><li>Przystąpienie do promocji – data zawarcia przez Uczestnika Promocji Umowy na warunkach określonych w Umowie oraz w niniejszym Regulaminie Promocji;</li><li>Uczestnik Promocji – Abonent który zawarł Umowę z Organizatorem Promocji;</li><li>Usługa – usługi telekomunikacyjne lub pakiet usług telekomunikacyjnych świadczonych przez Organizatora Promocji na podstawie Umowy.</li><li>Okres rozliczeniowy – okres za który dokonywane są rozliczenia należności Uczestnika Promocji wobec Organizatora Promocji z tytułu świadczenia Usług, rozpoczynający się i kończący we wskazanym przed Organizatora Promocji dniach miesiąca. O ile nie wskazano inaczej okres rozliczeniowy wynosi jeden miesiąc kalendarzowy.</li></ol>"
            "p2", "m1" -> "Wyrażam zgodę na otrzymywanie od ELCARO (\"Spółka z ograniczoną odpowiedzialnością\") z siedzibą w Warszawie, ul. Odkryta 1A, treści marketingowych obejmujących informacje o usługach, produktach, programach i promocjach Spółki, a także prowadzenie badań marketingowych i satysfakcji klienta poprzez połączenia telefoniczne, w tym połączenia z wykorzystaniem automatycznych systemów wywołujących na podany numer telefonu."
            "p1" -> getPrivacyPolicy(locale).text
            else -> "Document content not found."
        }
        return DocumentContentResponse(id, null, text, null)
    }
}
