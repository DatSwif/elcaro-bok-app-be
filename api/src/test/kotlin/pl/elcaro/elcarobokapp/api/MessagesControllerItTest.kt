package pl.elcaro.elcarobokapp.api

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.put
import pl.elcaro.elcarobokapp.domain.messages.GlobalMessage
import pl.elcaro.elcarobokapp.domain.messages.GlobalMessageTranslation
import pl.elcaro.elcarobokapp.domain.messages.GlobalMessageTranslationId
import pl.elcaro.elcarobokapp.domain.messages.PersonalMessage
import pl.elcaro.elcarobokapp.domain.registration.Client
import pl.elcaro.elcarobokapp.infrastructure.ClientGlobalMessageStateRepository
import pl.elcaro.elcarobokapp.infrastructure.ClientRepository
import pl.elcaro.elcarobokapp.infrastructure.GlobalMessageRepository
import pl.elcaro.elcarobokapp.infrastructure.PersonalMessageRepository
import pl.elcaro.elcarobokapp.service.JwtTokenProvider
import java.time.Instant

class MessagesControllerItTest : BaseIntegrationTest({

    val self = this as MessagesControllerItTest

    beforeTest {
        self.clientGlobalMessageStateRepository.deleteAll()
        self.personalMessageRepository.deleteAll()
        self.globalMessageRepository.deleteAll()
        self.clientRepository.deleteAll()
    }

    afterTest {
        self.clientGlobalMessageStateRepository.deleteAll()
        self.personalMessageRepository.deleteAll()
        self.globalMessageRepository.deleteAll()
        self.clientRepository.deleteAll()
    }

    fun savedClientWithToken(clientNumber: String = "C001"): Pair<Client, String> {
        val client = self.clientRepository.save(Client(clientNumber = clientNumber, languageCode = "pl"))
        val token = self.jwtTokenProvider.generateToken(clientNumber)
        return client to token
    }

    fun savedGlobalMessage(title: String = "Hello", content: String = "Body"): GlobalMessage {
        val msg = GlobalMessage(createdAt = Instant.now())
        msg.translations.add(
            GlobalMessageTranslation(
                id = GlobalMessageTranslationId(languageCode = "pl", globalMessageId = 0),
                globalMessage = msg,
                title = title,
                content = content
            )
        )
        return self.globalMessageRepository.save(msg)
    }

    fun savedPersonalMessage(client: Client, title: String = "Personal", level: Int = 1): PersonalMessage =
        self.personalMessageRepository.save(
            PersonalMessage(title = title, level = level, client = client)
        )

    describe("GET /api/messages/global") {
        it("returns 200 with all global messages, unread by default") {
            val (_, token) = savedClientWithToken()
            savedGlobalMessage("First", "First body")
            savedGlobalMessage("Second", "Second body")

            self.mockMvc.get("/api/messages/global") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(2) }
                jsonPath("$[0].is_read") { value(false) }
                jsonPath("$[0].read_at") { value(null) }
                jsonPath("$[0].title") { exists() }
                jsonPath("$[0].content") { exists() }
            }
        }

        it("returns 401 when no token provided") {
            self.mockMvc.get("/api/messages/global").andExpect {
                status { isUnauthorized() }
            }
        }
    }

    describe("PUT /api/messages/global/{id}") {
        it("marks message as read for the current client only") {
            val (_, tokenA) = savedClientWithToken("CLIENT-A")
            val (_, tokenB) = savedClientWithToken("CLIENT-B")
            val message = savedGlobalMessage()

            self.mockMvc.put("/api/messages/global/${message.id}") {
                header("Authorization", "Bearer $tokenA")
            }.andExpect { status { isOk() } }

            self.mockMvc.get("/api/messages/global") {
                header("Authorization", "Bearer $tokenA")
            }.andExpect {
                status { isOk() }
                jsonPath("$[0].is_read") { value(true) }
                jsonPath("$[0].read_at") { exists() }
            }

            self.mockMvc.get("/api/messages/global") {
                header("Authorization", "Bearer $tokenB")
            }.andExpect {
                status { isOk() }
                jsonPath("$[0].is_read") { value(false) }
            }
        }

        it("returns 404 for unknown message id") {
            val (_, token) = savedClientWithToken()
            self.mockMvc.put("/api/messages/global/9999") {
                header("Authorization", "Bearer $token")
            }.andExpect { status { isNotFound() } }
        }

        it("returns 401 when no token provided") {
            self.mockMvc.put("/api/messages/global/1").andExpect {
                status { isUnauthorized() }
            }
        }
    }

    describe("DELETE /api/messages/global/{id}") {
        it("hides the message for the current client only") {
            val (_, tokenA) = savedClientWithToken("CLIENT-A")
            val (_, tokenB) = savedClientWithToken("CLIENT-B")
            val message = savedGlobalMessage()

            self.mockMvc.delete("/api/messages/global/${message.id}") {
                header("Authorization", "Bearer $tokenA")
            }.andExpect { status { isOk() } }

            self.mockMvc.get("/api/messages/global") {
                header("Authorization", "Bearer $tokenA")
            }.andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(0) }
            }

            self.mockMvc.get("/api/messages/global") {
                header("Authorization", "Bearer $tokenB")
            }.andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(1) }
            }
        }

        it("returns 404 for unknown message id") {
            val (_, token) = savedClientWithToken()
            self.mockMvc.delete("/api/messages/global/9999") {
                header("Authorization", "Bearer $token")
            }.andExpect { status { isNotFound() } }
        }
    }

    describe("GET /api/messages/personal") {
        it("returns 200 with personal messages for the authenticated client only") {
            val (clientA, tokenA) = savedClientWithToken("CLIENT-A")
            val (clientB, _) = savedClientWithToken("CLIENT-B")
            savedPersonalMessage(clientA, "Mine", level = 1)
            savedPersonalMessage(clientB, "Theirs", level = 2)

            self.mockMvc.get("/api/messages/personal") {
                header("Authorization", "Bearer $tokenA")
            }.andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(1) }
                jsonPath("$[0].title") { value("Mine") }
                jsonPath("$[0].level") { value(1) }
                jsonPath("$[0].is_read") { value(false) }
            }
        }

        it("returns 401 when no token provided") {
            self.mockMvc.get("/api/messages/personal").andExpect {
                status { isUnauthorized() }
            }
        }
    }

    describe("PUT /api/messages/personal/{id}") {
        it("marks personal message as read") {
            val (client, token) = savedClientWithToken()
            val message = savedPersonalMessage(client)

            self.mockMvc.put("/api/messages/personal/${message.id}") {
                header("Authorization", "Bearer $token")
            }.andExpect { status { isOk() } }

            val updated = self.personalMessageRepository.findById(message.id!!).get()
            assert(updated.isRead)
            assert(updated.readAt != null)
        }

        it("returns 404 for someone else's message") {
            val (clientA, _) = savedClientWithToken("CLIENT-A")
            val (_, tokenB) = savedClientWithToken("CLIENT-B")
            val message = savedPersonalMessage(clientA)

            self.mockMvc.put("/api/messages/personal/${message.id}") {
                header("Authorization", "Bearer $tokenB")
            }.andExpect { status { isNotFound() } }
        }

        it("returns 404 for unknown id") {
            val (_, token) = savedClientWithToken()
            self.mockMvc.put("/api/messages/personal/9999") {
                header("Authorization", "Bearer $token")
            }.andExpect { status { isNotFound() } }
        }
    }

    describe("DELETE /api/messages/personal/{id}") {
        it("deletes the message") {
            val (client, token) = savedClientWithToken()
            val message = savedPersonalMessage(client)

            self.mockMvc.delete("/api/messages/personal/${message.id}") {
                header("Authorization", "Bearer $token")
            }.andExpect { status { isOk() } }

            assert(self.personalMessageRepository.findById(message.id!!).isEmpty)
        }

        it("returns 404 for someone else's message") {
            val (clientA, _) = savedClientWithToken("CLIENT-A")
            val (_, tokenB) = savedClientWithToken("CLIENT-B")
            val message = savedPersonalMessage(clientA)

            self.mockMvc.delete("/api/messages/personal/${message.id}") {
                header("Authorization", "Bearer $tokenB")
            }.andExpect { status { isNotFound() } }
        }
    }
}) {
    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var clientRepository: ClientRepository
    @Autowired lateinit var globalMessageRepository: GlobalMessageRepository
    @Autowired lateinit var personalMessageRepository: PersonalMessageRepository
    @Autowired lateinit var clientGlobalMessageStateRepository: ClientGlobalMessageStateRepository
    @Autowired lateinit var jwtTokenProvider: JwtTokenProvider
}
