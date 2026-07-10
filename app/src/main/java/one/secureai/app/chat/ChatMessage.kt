package one.secureai.app.chat

import java.util.UUID

enum class ChatRole(val wireValue: String) {
    USER("user"),
    ASSISTANT("assistant")
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: ChatRole,
    val content: String
)
