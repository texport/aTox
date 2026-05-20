package ltd.evilcorp.domain.feature

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ltd.evilcorp.core.model.Group
import ltd.evilcorp.core.model.GroupMessage
import ltd.evilcorp.core.model.GroupPeer
import ltd.evilcorp.core.model.GroupPrivacyState
import ltd.evilcorp.core.model.MessageType
import ltd.evilcorp.core.model.PublicKey
import ltd.evilcorp.core.model.Sender
import ltd.evilcorp.core.repository.ContactRepository
import ltd.evilcorp.core.repository.GroupRepository
import ltd.evilcorp.core.tox.enums.ToxGroupPrivacyState
import ltd.evilcorp.core.tox.enums.ToxMessageType
import ltd.evilcorp.domain.tox.Tox

@Singleton
class GroupManager @Inject constructor(
    private val scope: CoroutineScope,
    private val groupRepository: GroupRepository,
    private val contactRepository: ContactRepository,
    private val tox: Tox,
) {
    var activeGroup = ""
        set(value) {
            field = value
            if (value.isNotEmpty()) {
                scope.launch {
                    groupRepository.setHasUnreadMessages(value, false)
                }
            }
        }

    fun getAll(): Flow<List<Group>> = groupRepository.getAll()

    fun get(chatId: String): Flow<Group?> = groupRepository.get(chatId)

    fun createGroup(
        privacyState: GroupPrivacyState,
        groupName: String,
        selfName: String,
    ): Int {
        val toxPrivacyState = when (privacyState) {
            GroupPrivacyState.Public -> ToxGroupPrivacyState.PUBLIC
            GroupPrivacyState.Private -> ToxGroupPrivacyState.PRIVATE
        }
        val groupNumber = tox.groupNew(
            toxPrivacyState,
            groupName.toByteArray(),
            selfName.toByteArray(),
        )

        if (groupNumber >= 0) {
            val chatIdBytes = tox.groupGetChatId(groupNumber)
            val chatId = chatIdBytes?.toHexString() ?: "unknown_$groupNumber"

            val selfPeerId = tox.groupSelfGetPeerId(groupNumber)
            val selfRole = tox.groupSelfGetRole(groupNumber)

            val group = Group(
                chatId = chatId,
                name = groupName,
                privacyState = privacyState,
                peerCount = 1,
                selfPeerId = selfPeerId,
                selfRole = selfRole.name,
                groupNumber = groupNumber,
                connected = true,
            )
            groupRepository.add(group)

            val ourPeer = GroupPeer(
                groupChatId = chatId,
                peerId = selfPeerId,
                name = selfName,
                publicKey = tox.publicKey.string(),
                role = selfRole.name,
                isOurselves = true,
            )
            groupRepository.addPeer(ourPeer)
        }

        return groupNumber
    }

    fun joinGroup(
        friendNo: Int,
        inviteData: ByteArray,
        selfName: String,
        password: String? = null,
    ): Int {
        val groupNumber = tox.groupJoin(
            friendNo,
            inviteData,
            selfName.toByteArray(),
            password?.toByteArray(),
        )

        if (groupNumber >= 0) {
            val chatIdBytes = tox.groupGetChatId(groupNumber)
            val chatId = chatIdBytes?.toHexString() ?: "unknown_$groupNumber"

            val groupNameBytes = tox.groupGetName(groupNumber)
            val groupName = groupNameBytes?.decodeToString() ?: "Unknown Group"

            val selfPeerId = tox.groupSelfGetPeerId(groupNumber)
            val selfRole = tox.groupSelfGetRole(groupNumber)

            val group = Group(
                chatId = chatId,
                name = groupName,
                selfPeerId = selfPeerId,
                selfRole = selfRole.name,
                groupNumber = groupNumber,
                connected = true,
            )
            groupRepository.add(group)

            val ourPeer = GroupPeer(
                groupChatId = chatId,
                peerId = selfPeerId,
                name = selfName,
                publicKey = tox.publicKey.string(),
                role = selfRole.name,
                isOurselves = true,
            )
            groupRepository.addPeer(ourPeer)
        }

        return groupNumber
    }

    fun leaveGroup(chatId: String) = scope.launch {
        val g = groupRepository.get(chatId).firstOrNull()
        g?.let {
            if (it.groupNumber >= 0) {
                tox.groupLeave(it.groupNumber)
            }
            groupRepository.deleteAllPeers(it.chatId)
            groupRepository.deleteByChatId(it.chatId)
        }
    }

    fun sendMessage(chatId: String, message: String, type: MessageType = MessageType.Normal) = scope.launch {
        val g = groupRepository.get(chatId).firstOrNull()
        g?.let {
            val msgId = tox.groupSendMessage(
                it.groupNumber,
                ToxMessageType.NORMAL,
                message.toByteArray(),
            )

            val groupMsg = GroupMessage(
                groupChatId = chatId,
                peerId = it.selfPeerId,
                senderName = tox.getName(),
                message = message,
                sender = Sender.Sent,
                type = type,
                correlationId = msgId,
            )
            groupRepository.addMessage(groupMsg)
        }
    }

    fun setTopic(chatId: String, topic: String) = scope.launch {
        val g = groupRepository.get(chatId).firstOrNull()
        g?.let {
            if (it.groupNumber >= 0) {
                tox.groupSetTopic(it.groupNumber, topic.toByteArray())
                groupRepository.setTopic(chatId, topic)
            }
        }
    }

    fun messagesFor(chatId: String): Flow<List<GroupMessage>> = groupRepository.getMessages(chatId)

    fun getPeers(chatId: String): Flow<List<GroupPeer>> = groupRepository.getPeers(chatId)

    fun clearHistory(chatId: String) = scope.launch {
        groupRepository.deleteMessages(chatId)
        groupRepository.setLastMessage(chatId, 0)
    }

    fun deleteMessage(id: Long) = scope.launch {
        groupRepository.deleteMessage(id)
    }

    fun setDraft(chatId: String, draft: String) = scope.launch {
        groupRepository.setDraftMessage(chatId, draft)
    }

    fun getChatId(chatId: String): String? {
        var result: String? = null
        runBlocking {
            result = groupRepository.get(chatId).firstOrNull()?.chatId
        }
        return result
    }

    fun inviteFriend(chatId: String, friendPublicKey: String): Boolean {
        var result = false
        runBlocking {
            val group = groupRepository.get(chatId).firstOrNull()
            if (group != null && group.groupNumber >= 0) {
                val friendNumber = tox.getFriendNumber(PublicKey(friendPublicKey))
                if (friendNumber >= 0) {
                    result = tox.groupInviteSend(group.groupNumber, friendNumber)
                }
            }
        }
        return result
    }

    fun joinByChatId(chatIdHex: String, selfName: String, password: String? = null): Int {
        if (groupRepository.exists(chatIdHex)) return -2

        val chatIdBytes = chatIdHex.hexToByteArray()
        val groupNumber = tox.groupJoinDirect(
            chatIdBytes,
            selfName.toByteArray(),
            password?.toByteArray(),
        )

        if (groupNumber >= 0) {
            val chatId = chatIdBytes.toHexString()

            val groupNameBytes = tox.groupGetName(groupNumber)
            val groupName = groupNameBytes?.decodeToString() ?: "Unknown Group"

            val selfPeerId = tox.groupSelfGetPeerId(groupNumber)
            val selfRole = tox.groupSelfGetRole(groupNumber)

            val group = Group(
                chatId = chatId,
                name = groupName,
                selfPeerId = selfPeerId,
                selfRole = selfRole.name,
                groupNumber = groupNumber,
                connected = true,
            )
            groupRepository.add(group)

            val ourPeer = GroupPeer(
                groupChatId = chatId,
                peerId = selfPeerId,
                name = selfName,
                publicKey = tox.publicKey.string(),
                role = selfRole.name,
                isOurselves = true,
            )
            groupRepository.addPeer(ourPeer)
        }

        return groupNumber
    }

    private fun String.hexToByteArray(): ByteArray {
        val result = ByteArray(length / 2)
        for (i in indices step 2) {
            result[i / 2] = ((substring(i, i + 2).toInt(16)) and 0xFF).toByte()
        }
        return result
    }

    private fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }
}
