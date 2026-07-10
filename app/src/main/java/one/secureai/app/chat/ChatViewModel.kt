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
import one.secureai.app.network.RemoteAIService

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val remote = RemoteAIService(application)

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var currentJob: Job? = null

    fun send(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _isStreaming.value) return

        _errorMessage.value = null
        val userMessage = ChatMessage(role = ChatRole.USER, content = trimmed)
        val assistantMessage = ChatMessage(role = ChatRole.ASSISTANT, content = "")
        _messages.update { it + userMessage + assistantMessage }
        _isStreaming.value = true

        currentJob = viewModelScope.launch {
            try {
                remote.sendMessageStreaming(history = _messages.value.dropLast(1)) { chunk ->
                    _messages.update { list ->
                        list.map { m ->
                            if (m.id == assistantMessage.id) m.copy(content = m.content + chunk) else m
                        }
                    }
                }
            } catch (e: Exception) {
                _messages.update { list -> list.filterNot { it.id == assistantMessage.id && it.content.isEmpty() } }
                _errorMessage.value = e.message ?: "Something went wrong. Try again."
            } finally {
                _isStreaming.value = false
            }
        }
    }

    fun stop() {
        currentJob?.cancel()
        _isStreaming.value = false
    }
}
