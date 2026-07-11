package one.secureai.app.chat

import android.net.Uri
import java.util.Date
import java.util.UUID

enum class ChatRole(val wireValue: String) {
    USER("user"),
    ASSISTANT("assistant"),
    SYSTEM("system")
}

data class ChatAttachment(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val mimeType: String = "",
    val data: ByteArray? = null,
    val uri: Uri? = null
) {
    val isImage: Boolean get() = mimeType.startsWith("image/")

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ChatAttachment) return false
        return id == other.id
    }
    override fun hashCode(): Int = id.hashCode()
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: ChatRole,
    val content: String,
    val model: String? = null,
    val imageBytes: ByteArray? = null,
    val attachments: List<ChatAttachment> = emptyList(),
    val isStreaming: Boolean = false,
    val thumbsUp: Boolean? = null,
    val saved: Boolean = false,
    val exchangeId: String? = null,
    val timestamp: Date = Date()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ChatMessage) return false
        return id == other.id && role == other.role && content == other.content &&
            model == other.model && isStreaming == other.isStreaming && thumbsUp == other.thumbsUp &&
            saved == other.saved && attachments == other.attachments &&
            (imageBytes?.contentEquals(other.imageBytes) ?: (other.imageBytes == null))
    }

    override fun hashCode(): Int = id.hashCode()
}
