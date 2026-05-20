package ltd.evilcorp.atox.tox

import android.util.Log
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import ltd.evilcorp.atox.media.SystemSoundPlayer
import ltd.evilcorp.atox.settings.Settings
import ltd.evilcorp.atox.ui.NotificationHelper
import ltd.evilcorp.core.repository.ContactRepository
import ltd.evilcorp.core.repository.GroupRepository
import ltd.evilcorp.core.model.Group
import ltd.evilcorp.core.model.GroupMessage
import ltd.evilcorp.core.model.GroupPeer
import ltd.evilcorp.core.model.MessageType
import ltd.evilcorp.core.model.Sender
import ltd.evilcorp.core.tox.enums.ToxGroupExitType
import ltd.evilcorp.core.tox.enums.ToxGroupJoinFail
import ltd.evilcorp.core.tox.enums.ToxGroupModEvent
import ltd.evilcorp.core.tox.enums.ToxGroupPrivacyState
import ltd.evilcorp.core.tox.enums.ToxGroupRole
import ltd.evilcorp.core.tox.enums.ToxMessageType
import ltd.evilcorp.domain.feature.GroupManager
import ltd.evilcorp.domain.tox.Tox

private const val TAG = "GroupEventHandler"

class GroupEventHandler @Inject constructor(
    private val scope: CoroutineScope,
    private val groupRepository: GroupRepository,
    private val contactRepository: ContactRepository,
    private val groupManager: GroupManager,
    private val notificationHelper: NotificationHelper,
    private val systemSoundPlayer: SystemSoundPlayer,
    private val tox: Tox,
    private val settings: Settings,
) {
    fun onGroupInvite(friendNo: Int, inviteData: ByteArray, groupName: String) {
        scope.launch {
            val selfName = tox.getName()
            val groupNumber = groupManager.joinGroup(friendNo, inviteData, selfName)
            if (groupNumber < 0) {
                Log.e(TAG, "Failed to auto-join group from invite")
            }
        }
    }

    fun onGroupMessage(
        groupNo: Int,
        peerId: Int,
        type: ToxMessageType,
        message: String,
        messageId: Int,
    ) {
        scope.launch {
            val chatIdBytes = tox.groupGetChatId(groupNo)
            val chatId = chatIdBytes?.toHexString() ?: return@launch

            val peerNameBytes = tox.groupPeerGetName(groupNo, peerId)
            val peerName = peerNameBytes?.decodeToString() ?: "Unknown"

            val group = groupRepository.get(chatId).firstOrNull()
            if (group == null) {
                Log.e(TAG, "Group not found for chatId=$chatId")
                return@launch
            }

            val isOurPeer = peerId == group.selfPeerId

            if (groupRepository.existsByCorrelationId(chatId, messageId)) {
                return@launch
            }

            val groupMsg = GroupMessage(
                groupChatId = chatId,
                peerId = peerId,
                senderName = peerName,
                message = message,
                sender = if (isOurPeer) Sender.Sent else Sender.Received,
                type = type.toMessageType(),
                correlationId = messageId,
                timestamp = Date().time,
            )
            groupRepository.addMessage(groupMsg)

            if (!isOurPeer && groupManager.activeGroup != chatId) {
                systemSoundPlayer.playNotificationSound(settings.notificationSoundUri, settings.notificationSoundVolume)
                notificationHelper.showGroupMessageNotification(group.name, peerName, message)
                groupRepository.setHasUnreadMessages(chatId, true)
            } else {
                systemSoundPlayer.playNotificationSound(settings.activeChatSoundUri, settings.activeChatSoundVolume)
            }
        }
    }

    fun onGroupPeerJoin(groupNo: Int, peerId: Int) {
        scope.launch {
            val chatIdBytes = tox.groupGetChatId(groupNo)
            val chatId = chatIdBytes?.toHexString() ?: return@launch

            val peerNameBytes = tox.groupPeerGetName(groupNo, peerId)
            val peerName = peerNameBytes?.decodeToString() ?: "Unknown"

            val peerKeyBytes = tox.groupPeerGetPublicKey(groupNo, peerId)
            val peerKey = peerKeyBytes?.toHexString() ?: ""

            val peer = GroupPeer(
                groupChatId = chatId,
                peerId = peerId,
                name = peerName,
                publicKey = peerKey,
            )
            groupRepository.addPeer(peer)

            val count = groupRepository.peerCount(chatId).firstOrNull() ?: 0
            groupRepository.setPeerCount(chatId, count)

            Log.i(TAG, "Peer joined group: $peerName ($peerId) in $chatId")
        }
    }

    fun onGroupPeerExit(groupNo: Int, peerId: Int, exitType: ToxGroupExitType) {
        scope.launch {
            val chatIdBytes = tox.groupGetChatId(groupNo)
            val chatId = chatIdBytes?.toHexString() ?: return@launch

            groupRepository.deletePeerById(chatId, peerId)

            val count = groupRepository.peerCount(chatId).firstOrNull() ?: 0
            groupRepository.setPeerCount(chatId, count)

            Log.i(TAG, "Peer left group: peerId=$peerId, exitType=$exitType in $chatId")
        }
    }

    fun onGroupTopic(groupNo: Int, peerId: Int, topic: String) {
        scope.launch {
            val chatIdBytes = tox.groupGetChatId(groupNo)
            val chatId = chatIdBytes?.toHexString() ?: return@launch
            groupRepository.setTopic(chatId, topic)
            Log.i(TAG, "Group topic changed in $chatId")
        }
    }

    fun onGroupPeerName(groupNo: Int, peerId: Int, name: String) {
        scope.launch {
            val chatIdBytes = tox.groupGetChatId(groupNo)
            val chatId = chatIdBytes?.toHexString() ?: return@launch
            groupRepository.setPeerName(chatId, peerId, name)
        }
    }

    fun onGroupPassword(groupNo: Int, password: ByteArray) {
        scope.launch {
            val chatIdBytes = tox.groupGetChatId(groupNo)
            val chatId = chatIdBytes?.toHexString() ?: return@launch
            groupRepository.setPasswordProtected(chatId, password.isNotEmpty())
        }
    }

    fun onGroupPeerStatus(groupNo: Int, peerId: Int, status: ltd.evilcorp.core.tox.enums.ToxUserStatus) {
        scope.launch {
            val chatIdBytes = tox.groupGetChatId(groupNo)
            val chatId = chatIdBytes?.toHexString() ?: return@launch
            val userStatus = when (status) {
                ltd.evilcorp.core.tox.enums.ToxUserStatus.NONE -> ltd.evilcorp.core.model.UserStatus.None
                ltd.evilcorp.core.tox.enums.ToxUserStatus.AWAY -> ltd.evilcorp.core.model.UserStatus.Away
                ltd.evilcorp.core.tox.enums.ToxUserStatus.BUSY -> ltd.evilcorp.core.model.UserStatus.Busy
            }
            groupRepository.setPeerStatus(chatId, peerId, userStatus)
        }
    }

    fun onGroupPrivacyState(groupNo: Int, privacyState: ToxGroupPrivacyState) {
        scope.launch {
            val chatIdBytes = tox.groupGetChatId(groupNo)
            val chatId = chatIdBytes?.toHexString() ?: return@launch
            val state = when (privacyState) {
                ToxGroupPrivacyState.PUBLIC -> ltd.evilcorp.core.model.GroupPrivacyState.Public
                ToxGroupPrivacyState.PRIVATE -> ltd.evilcorp.core.model.GroupPrivacyState.Private
            }
            groupRepository.setPrivacyState(chatId, state)
        }
    }

    fun onGroupVoiceState(groupNo: Int, voiceState: ltd.evilcorp.core.tox.enums.ToxGroupVoiceState) {
        scope.launch {
            Log.i(TAG, "Group voice state changed in groupNo=$groupNo, state=$voiceState")
        }
    }

    fun onGroupTopicLock(groupNo: Int, topicLock: ltd.evilcorp.core.tox.enums.ToxGroupTopicLock) {
        scope.launch {
            Log.i(TAG, "Group topic lock changed in groupNo=$groupNo, lock=$topicLock")
        }
    }

    fun onGroupPeerLimit(groupNo: Int, peerLimit: Int) {
        scope.launch {
            Log.i(TAG, "Group peer limit changed in groupNo=$groupNo, limit=$peerLimit")
        }
    }

    fun onGroupPrivateMessage(groupNo: Int, peerId: Int, type: ToxMessageType, message: String) {
        scope.launch {
            val chatIdBytes = tox.groupGetChatId(groupNo)
            val chatId = chatIdBytes?.toHexString() ?: return@launch
            val peerNameBytes = tox.groupPeerGetName(groupNo, peerId)
            val peerName = peerNameBytes?.decodeToString() ?: "Unknown"
            Log.i(TAG, "Private message in group $chatId from $peerName: $message")
        }
    }

    fun onGroupConnected(groupNo: Int) {
        scope.launch {
            val chatIdBytes = tox.groupGetChatId(groupNo)
            val chatId = chatIdBytes?.toHexString() ?: return@launch
            groupRepository.setConnected(chatId, true)
            groupRepository.setGroupNumber(chatId, groupNo)
            Log.i(TAG, "Connected to group $chatId")
        }
    }

    fun onGroupJoinFail(groupNo: Int, joinFail: ToxGroupJoinFail) {
        scope.launch {
            Log.e(TAG, "Failed to join groupNo=$groupNo, reason=$joinFail")
        }
    }

    fun onGroupModeration(groupNo: Int, sourcePeerId: Int, targetPeerId: Int, modEvent: ToxGroupModEvent) {
        scope.launch {
            val chatIdBytes = tox.groupGetChatId(groupNo)
            val chatId = chatIdBytes?.toHexString() ?: return@launch

            val sourceNameBytes = tox.groupPeerGetName(groupNo, sourcePeerId)
            val sourceName = sourceNameBytes?.decodeToString() ?: "Unknown"

            Log.i(TAG, "Moderation in $chatId: $sourceName performed $modEvent on peer $targetPeerId")

            if (modEvent == ToxGroupModEvent.KICK || modEvent == ToxGroupModEvent.MODERATOR || modEvent == ToxGroupModEvent.USER) {
                val role = when (modEvent) {
                    ToxGroupModEvent.MODERATOR -> ToxGroupRole.MODERATOR.name
                    ToxGroupModEvent.USER -> ToxGroupRole.USER.name
                    else -> null
                }
                if (role != null) {
                    groupRepository.setPeerRole(chatId, targetPeerId, role)
                }
            }
        }
    }

    private fun ToxMessageType.toMessageType(): MessageType = when (this) {
        ToxMessageType.NORMAL -> MessageType.Normal
        ToxMessageType.ACTION -> MessageType.Action
        else -> MessageType.Normal
    }

    private fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }
}
