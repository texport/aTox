package ltd.evilcorp.domain.fakes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import ltd.evilcorp.domain.features.chat.model.Message
import ltd.evilcorp.domain.features.chat.repository.IMessageRepository

class FakeMessageRepository : IMessageRepository {
    private val messages = MutableStateFlow<List<Message>>(emptyList())

    override suspend fun add(message: Message) {
        messages.value = messages.value + message
    }

    override fun get(conversation: String): Flow<List<Message>> {
        return messages.map { list -> list.filter { it.publicKey == conversation } }
    }

    override suspend fun getPending(conversation: String): List<Message> {
        return messages.value.filter { it.publicKey == conversation && it.timestamp == 0L }
    }

    override suspend fun setCorrelationId(id: Long, correlationId: Int) {
        messages.value = messages.value.map { msg ->
            if (msg.id == id) msg.copy(correlationId = correlationId) else msg
        }
    }

    override suspend fun delete(conversation: String) {
        messages.value = messages.value.filter { it.publicKey != conversation }
    }

    override suspend fun deleteMessage(id: Long) {
        messages.value = messages.value.filter { it.id != id }
    }

    override suspend fun setReceipt(conversation: String, correlationId: Int, timestamp: Long) {
        messages.value = messages.value.map { msg ->
            if (msg.publicKey == conversation && msg.correlationId == correlationId) {
                msg.copy(timestamp = timestamp)
            } else {
                msg
            }
        }
    }

    override suspend fun exists(conversation: String, message: String): Boolean {
        return messages.value.any { it.publicKey == conversation && it.message == message }
    }
}
