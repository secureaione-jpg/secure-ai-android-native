package one.secureai.app.chat

import java.util.UUID

enum class ChatRole(val wireValue: String) {
    USER("user"),
    ASSISTANT("assistant")
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: ChatRole,
    val content: String,
    val imageBytes: ByteArray? = null
) {
    // Data class equality/hashCode would compare imageBytes by reference by
    // default issues aside — Compose's LazyColumn keys off `id`, not equals,
    // so this override just keeps `==` sane for tests/debugging.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ChatMessage) return false
        return id == other.id && role == other.role && content == other.content &&
            (imageBytes?.contentEquals(other.imageBytes) ?: (other.imageBytes == null))
    }

    override fun hashCode(): Int = id.hashCode()
}
