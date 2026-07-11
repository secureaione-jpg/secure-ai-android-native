package one.secureai.app.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import one.secureai.app.data.model.AIModel
import one.secureai.app.data.store.ConversationStore
import one.secureai.app.data.store.ProjectStore
import one.secureai.app.network.ImageService
import one.secureai.app.network.RemoteAIService

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val remote = RemoteAIService(application)
    private val imageService = ImageService()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _selectedModel = MutableStateFlow(AIModel.AUTO)
    val selectedModel: StateFlow<AIModel> = _selectedModel.asStateFlow()

    private var currentJob: Job? = null
    private var currentConversationId: String? = null
    private val savedMessageIds = mutableSetOf<String>()

    fun selectModel(model: AIModel) {
        _selectedModel.value = model
    }

    fun send(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _isStreaming.value) return

        if (looksLikeImageRequest(trimmed)) {
            generateImage(trimmed)
            return
        }

        _errorMessage.value = null
        val model = _selectedModel.value
        val userMessage = ChatMessage(role = ChatRole.USER, content = trimmed)
        val assistantMessage = ChatMessage(role = ChatRole.ASSISTANT, content = "", model = model.wireValue, isStreaming = true)
        _messages.update { it + userMessage + assistantMessage }
        _isStreaming.value = true

        currentJob = viewModelScope.launch {
            try {
                remote.sendMessageStreaming(
                    history = _messages.value.dropLast(1),
                    model = model.wireValue
                ) { chunk ->
                    _messages.update { list ->
                        list.map { m ->
                            if (m.id == assistantMessage.id) m.copy(content = m.content + chunk) else m
                        }
                    }
                }
                _messages.update { list ->
                    list.map { m ->
                        if (m.id == assistantMessage.id) m.copy(isStreaming = false) else m
                    }
                }
                saveConversation()
            } catch (e: Exception) {
                _messages.update { list -> list.filterNot { it.id == assistantMessage.id && it.content.isEmpty() } }
                _errorMessage.value = e.message ?: "Something went wrong. Try again."
            } finally {
                _isStreaming.value = false
            }
        }
    }

    fun regenerate() {
        val msgs = _messages.value
        val lastAssistantIdx = msgs.indexOfLast { it.role == ChatRole.ASSISTANT }
        if (lastAssistantIdx < 0) return
        val lastUserIdx = msgs.subList(0, lastAssistantIdx).indexOfLast { it.role == ChatRole.USER }
        if (lastUserIdx < 0) return

        val userContent = msgs[lastUserIdx].content
        _messages.update { it.subList(0, lastUserIdx) }
        send(userContent)
    }

    fun setFeedback(messageId: String, isUp: Boolean) {
        _messages.update { list ->
            list.map { m ->
                if (m.id == messageId) {
                    m.copy(thumbsUp = if (m.thumbsUp == isUp) null else isUp)
                } else m
            }
        }
    }

    private fun generateImage(prompt: String) {
        _errorMessage.value = null
        val userMessage = ChatMessage(role = ChatRole.USER, content = prompt)
        val assistantMessage = ChatMessage(role = ChatRole.ASSISTANT, content = "", isStreaming = true)
        _messages.update { it + userMessage + assistantMessage }
        _isStreaming.value = true

        currentJob = viewModelScope.launch {
            try {
                val bytes = imageService.generate(prompt)
                _messages.update { list ->
                    list.map { m ->
                        if (m.id == assistantMessage.id) m.copy(imageBytes = bytes, isStreaming = false) else m
                    }
                }
                saveConversation()
            } catch (e: Exception) {
                _messages.update { list ->
                    list.map { m ->
                        if (m.id == assistantMessage.id) {
                            m.copy(content = "Couldn't create that image. ${e.message.orEmpty()}", isStreaming = false)
                        } else m
                    }
                }
                _errorMessage.value = e.message ?: "Something went wrong. Try again."
            } finally {
                _isStreaming.value = false
            }
        }
    }

    fun stop() {
        currentJob?.cancel()
        _isStreaming.value = false
        _messages.update { list ->
            list.map { m -> if (m.isStreaming) m.copy(isStreaming = false) else m }
        }
    }

    fun clearHistory() {
        currentJob?.cancel()
        _isStreaming.value = false
        _messages.value = emptyList()
        _errorMessage.value = null
        currentConversationId = null
        savedMessageIds.clear()
    }

    fun sendWithAttachment(text: String, data: ByteArray, mimeType: String) {
        val trimmed = text.trim().ifEmpty { "Describe this" }
        if (_isStreaming.value) return
        _errorMessage.value = null

        val attachment = ChatAttachment(name = "attachment", mimeType = mimeType, data = data)
        val userMessage = ChatMessage(role = ChatRole.USER, content = trimmed, attachments = listOf(attachment))
        val assistantMessage = ChatMessage(role = ChatRole.ASSISTANT, content = "", isStreaming = true)
        _messages.update { it + userMessage + assistantMessage }
        _isStreaming.value = true

        currentJob = viewModelScope.launch {
            try {
                remote.sendMessageStreaming(
                    history = _messages.value.filter { !it.isStreaming },
                    model = _selectedModel.value.wireValue
                ) { chunk ->
                    _messages.update { list ->
                        list.map { m ->
                            if (m.id == assistantMessage.id) m.copy(content = m.content + chunk) else m
                        }
                    }
                }
                _messages.update { list ->
                    list.map { m ->
                        if (m.id == assistantMessage.id) m.copy(isStreaming = false) else m
                    }
                }
                saveConversation()
            } catch (e: Exception) {
                _messages.update { list ->
                    list.map { m ->
                        if (m.id == assistantMessage.id) m.copy(content = e.message ?: "Error", isStreaming = false) else m
                    }
                }
                _errorMessage.value = e.message ?: "Something went wrong."
            } finally {
                _isStreaming.value = false
            }
        }
    }

    fun toggleSaved(messageId: String) {
        _messages.update { list ->
            list.map { m ->
                if (m.id == messageId) m.copy(saved = !m.saved) else m
            }
        }
    }

    fun loadConversation(conversationId: String) {
        viewModelScope.launch {
            val msgs = ConversationStore.loadMessages(conversationId)
            if (msgs.isNotEmpty()) {
                currentConversationId = conversationId
                savedMessageIds.clear()
                savedMessageIds.addAll(msgs.map { it.id })
                _messages.value = msgs
            }
        }
    }

    private fun saveConversation() {
        val msgs = _messages.value.filter { it.content.isNotEmpty() || it.imageBytes != null }
        if (msgs.isEmpty()) return
        viewModelScope.launch {
            val title = msgs.firstOrNull { it.role == ChatRole.USER }?.content?.take(60) ?: "Chat"
            val lastMsg = msgs.lastOrNull()?.content?.take(100) ?: ""
            val newIds = msgs.map { it.id }.filter { it !in savedMessageIds }
            val newMessages = msgs.filter { it.id in newIds }

            currentConversationId = ConversationStore.saveConversation(
                conversationId = currentConversationId,
                title = title,
                lastMessage = lastMsg,
                newMessages = newMessages,
                messageCount = msgs.size
            )
            savedMessageIds.addAll(newIds)
        }
    }

    companion object {
        private const val IMAGE_VERBS = "(draw|sketch|paint|generate|create|make|design|render|imagine|illustrate|produce)"
        private const val IMAGE_NOUNS = "(image|picture|photo|drawing|illustration|logo|art|artwork|painting|wallpaper|icon|poster|graphic|meme)"
        private val imageRequestVerbNoun = Regex("""\b$IMAGE_VERBS\b[^.?!]{0,30}\b$IMAGE_NOUNS\b""", RegexOption.IGNORE_CASE)
        private val imageRequestNounOf = Regex("""^\s*(an?\s+)?$IMAGE_NOUNS\s+of\b""", RegexOption.IGNORE_CASE)

        fun looksLikeImageRequest(text: String): Boolean =
            imageRequestVerbNoun.containsMatchIn(text) || imageRequestNounOf.containsMatchIn(text)
    }
}
