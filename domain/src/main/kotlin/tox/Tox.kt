// SPDX-FileCopyrightText: 2019-2025 Robin Lindén <dev@robinlinden.eu>
// SPDX-FileCopyrightText: 2019-2020 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.tox

import android.util.Log

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.forEach as kForEach
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ltd.evilcorp.core.repository.ContactRepository
import ltd.evilcorp.core.repository.UserRepository
import ltd.evilcorp.core.vo.ConnectionStatus
import ltd.evilcorp.core.vo.Contact
import ltd.evilcorp.core.vo.FileKind
import ltd.evilcorp.core.vo.MessageType
import ltd.evilcorp.core.vo.PublicKey
import ltd.evilcorp.core.vo.UserStatus

private const val TAG = "Tox"
private const val SLOW_ITERATION_LIMIT_MS = 10
private const val TOX_SALT_LENGTH = 32
private const val STOP_DELAY_MS = 10L
private const val BOOTSTRAP_NODES_COUNT = 4

@Singleton
class Tox @Inject constructor(
    private val scope: CoroutineScope,
    private val contactRepository: ContactRepository,
    private val userRepository: UserRepository,
    private val saveManager: SaveManager,
    private val nodeRegistry: BootstrapNodeRegistry,
) {
    val toxId: ToxID get() = toxWrapper.getToxId()
    val publicKey: PublicKey by lazy { toxWrapper.getPublicKey() }
    var nospam: Int
    get() = toxWrapper.getNospam()
    set(value) = toxWrapper.setNospam(value)

    var started = false
    var isBootstrapNeeded = true

    private var running = false
    private var toxAvRunning = false

    private var passkey: ByteArray? = null
    var password: String? = null
        private set

    fun changePassword(new: String?) {
        passkey = if (new.isNullOrEmpty()) {
            null
        } else {
            val salt = ByteArray(TOX_SALT_LENGTH)
            Random.Default.nextBytes(salt)
            val nativeTox = NativeTox()
            nativeTox.passKeyDeriveWithSalt(new.toByteArray(), salt)
        }
        password = new
        save()
    }

    private lateinit var toxWrapper: ToxWrapper

    fun start(saveOption: SaveOptions, password: String?, listener: ToxEventListener, avListener: ToxAvEventListener) {
        val nativeTox = NativeTox()
        toxWrapper = if (password == null) {
            passkey = null
            ToxWrapper(listener, avListener, saveOption)
        } else {
            val salt = nativeTox.getSalt(saveOption.saveData ?: ByteArray(0))
            if (salt != null) {
                passkey = nativeTox.passKeyDeriveWithSalt(password.toByteArray(), salt)
            }
            ToxWrapper(
                listener,
                avListener,
                saveOption.copy(saveData = if (passkey != null) nativeTox.passDecrypt(saveOption.saveData ?: ByteArray(0), passkey!!) else saveOption.saveData),
            )
        }

        this.password = password
        started = true

        fun loadContacts() = scope.launch {
            contactRepository.resetTransientData()

            for ((publicKey, _) in toxWrapper.getContacts()) {
                if (!contactRepository.exists(publicKey.string())) {
                    contactRepository.add(Contact(publicKey.string()))
                }
            }
        }

        fun iterateForeverAv() = scope.launch {
            toxAvRunning = true
            while (running) {
                toxWrapper.iterateAv()
                delay(toxWrapper.iterationIntervalAv())
            }
            toxAvRunning = false
        }

        fun iterateForever() = scope.launch {
            running = true
            userRepository.updateConnection(publicKey.string(), ConnectionStatus.None)
            while (running || toxAvRunning) {
                if (isBootstrapNeeded) {
                    try {
                        bootstrap()
                        isBootstrapNeeded = false
                    } catch (e: Exception) {
                        Log.e(TAG, e.toString())
                    }
                }

                val before = System.currentTimeMillis()
                toxWrapper.iterate()
                val timeTaken = System.currentTimeMillis() - before
                val iterationInterval = toxWrapper.iterationInterval()
                if (timeTaken > SLOW_ITERATION_LIMIT_MS && timeTaken > iterationInterval) {
                    Log.w(TAG, "Tox thread overran: $timeTaken/$iterationInterval.")
                }
                delay(iterationInterval - timeTaken)
            }
            started = false
        }

        save()
        loadContacts()
        iterateForever()
        iterateForeverAv()
    }

    fun stop() = scope.launch {
        running = false
        while (started) delay(STOP_DELAY_MS)
        save().join()
        toxWrapper.stop()
        passkey = null
    }

    private val saveMutex = Mutex()
    private fun save() = scope.launch {
        saveMutex.withLock {
            if (!started) return@withLock
            val saveData = toxWrapper.getSaveData()
            val encryptedData = if (passkey != null) {
                val nativeTox = NativeTox()
                nativeTox.passEncrypt(saveData, passkey!!) ?: saveData
            } else {
                saveData
            }
            saveManager.save(
                publicKey,
                encryptedData
            )
        }
    }

    fun acceptFriendRequest(publicKey: PublicKey) {
        toxWrapper.acceptFriendRequest(publicKey)
        save()
    }

    fun startFileTransfer(pk: PublicKey, fileNumber: Int) {
        Log.i(TAG, "Starting file transfer $fileNumber from ${pk.fingerprint()}")
        toxWrapper.startFileTransfer(pk, fileNumber)
    }

    fun stopFileTransfer(pk: PublicKey, fileNumber: Int) {
        Log.i(TAG, "Stopping file transfer $fileNumber from ${pk.fingerprint()}")
        toxWrapper.stopFileTransfer(pk, fileNumber)
    }

    fun sendFile(pk: PublicKey, fileKind: FileKind, fileSize: Long, fileName: String) =
        toxWrapper.sendFile(pk, fileKind, fileSize, fileName)

    fun sendFileChunk(pk: PublicKey, fileNo: Int, pos: Long, data: ByteArray): Result<Unit> =
        toxWrapper.sendFileChunk(pk, fileNo, pos, data)

    fun getName() = toxWrapper.getName()
    fun setName(name: String) {
        toxWrapper.setName(name)
        save()
    }

    fun getStatusMessage() = toxWrapper.getStatusMessage()
    fun setStatusMessage(statusMessage: String) {
        toxWrapper.setStatusMessage(statusMessage)
        save()
    }

    fun addContact(toxId: ToxID, message: String) {
        toxWrapper.addContact(toxId, message)
        save()
    }

    fun deleteContact(publicKey: PublicKey) {
        toxWrapper.deleteContact(publicKey)
        save()
    }

    fun sendMessage(publicKey: PublicKey, message: String, type: MessageType) =
        toxWrapper.sendMessage(publicKey, message, type)

    fun getSaveData(): ByteArray {
        val pk = passkey
        return if (pk == null) {
            toxWrapper.getSaveData()
        } else {
            NativeTox().passEncrypt(toxWrapper.getSaveData(), pk) ?: toxWrapper.getSaveData()
        }
    }

    private fun bootstrap() {
        nodeRegistry.get(BOOTSTRAP_NODES_COUNT).kForEach { node ->
            Log.i(TAG, "Bootstrapping from $node")
            toxWrapper.bootstrap(node.address, node.port, node.publicKey.bytes())
        }
    }

    fun setTyping(publicKey: PublicKey, typing: Boolean) = toxWrapper.setTyping(publicKey, typing)

    fun getStatus() = toxWrapper.getStatus()
    fun setStatus(status: UserStatus) {
        toxWrapper.setStatus(status)
        save()
    }

    fun sendLosslessPacket(pk: PublicKey, packet: ByteArray) = toxWrapper.sendLosslessPacket(pk, packet)

    // ToxAv, probably move these.
    fun startCall(pk: PublicKey) = toxWrapper.startCall(pk)
    fun answerCall(pk: PublicKey) = toxWrapper.answerCall(pk)
    fun endCall(pk: PublicKey) = toxWrapper.endCall(pk)
    fun sendAudio(pk: PublicKey, pcm: ShortArray, channels: Int, samplingRate: Int) =
        toxWrapper.sendAudio(pk, pcm, channels, samplingRate)
}
