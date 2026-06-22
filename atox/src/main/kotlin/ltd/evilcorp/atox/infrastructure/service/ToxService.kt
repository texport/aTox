// SPDX-FileCopyrightText: 2019-2025 Robin Lindén <dev@robinlinden.eu>
// SPDX-FileCopyrightText: 2021-2022 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.infrastructure.service

import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.MainActivity
import ltd.evilcorp.atox.infrastructure.util.PendingIntentCompat
import ltd.evilcorp.atox.infrastructure.util.PermissionManager
import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleService
import javax.inject.Inject
import dagger.hilt.android.AndroidEntryPoint
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.core.network.ITox
import ltd.evilcorp.domain.core.network.save.ToxSaveStatus
import ltd.evilcorp.domain.features.auth.usecase.InitializeToxUseCase
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first

import ltd.evilcorp.domain.core.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import ltd.evilcorp.domain.features.call.CallState
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.features.contacts.repository.IContactRepository
import ltd.evilcorp.atox.ui.notification.CallNotificationFactory

private const val TAG = "ToxService"
private const val NOTIFICATION_ID = 1984
private const val NOTIFICATION_ID_CALL = 1985

@AndroidEntryPoint
class ToxService : LifecycleService() {
    private val channelId = "ToxService"
    private var connectionStatus: ConnectionStatus? = null
    private var serviceSessionId: String? = null
    private var activeCallState: CallState = CallState.Idle

    @Inject
    lateinit var contactRepository: IContactRepository

    @Inject
    lateinit var callNotificationFactory: CallNotificationFactory
    private val notifier by lazy { NotificationManagerCompat.from(this) }

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    @Inject
    lateinit var tox: ITox

    @Inject
    lateinit var initializeToxUseCase: InitializeToxUseCase

    @Inject
    lateinit var restoreGroupsUseCase: ltd.evilcorp.domain.features.group.usecase.RestoreGroupsUseCase

    @Inject
    lateinit var permissionManager: PermissionManager

    @Inject
    lateinit var lifecycleController: ToxServiceLifecycleController

    private fun createNotificationChannel() {
        val channel = NotificationChannelCompat.Builder(channelId, NotificationManagerCompat.IMPORTANCE_LOW)
            .setName("Tox Service")
            .build()

        notifier.createNotificationChannel(channel)
    }

    private fun subTextFor(status: ConnectionStatus) = when (status) {
        ConnectionStatus.None -> getText(R.string.atox_offline)
        ConnectionStatus.TCP -> getText(R.string.atox_connected_with_tcp)
        ConnectionStatus.UDP -> getText(R.string.atox_connected_with_udp)
    }

    private fun notificationFor(status: ConnectionStatus?): Notification {
        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntentCompat.getActivity(this, 0, notificationIntent, 0)
            }

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setContentTitle(getString(R.string.tox_service_running))

        if (status != null) {
            builder.setContentText(subTextFor(status))
        }

        return builder.build()
    }

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()
        startInitialForegroundService()

        if (!permissionManager.canPostNotifications()) {
            Log.w(TAG, "Notifications disallowed")
        }

        // Execute heavy JNI and disk IO initialization asynchronously in background to prevent main thread ANR
        lifecycleScope.launch {
            if (!tox.started) {
                val status = withContext(ioDispatcher) {
                    initializeToxUseCase.execute(null)
                }
                if (status != ToxSaveStatus.Ok) {
                    Log.e(TAG, "Tox service started without a Tox save")
                    stopSelf()
                    return@launch
                }
            }

            // Capture the session ID
            serviceSessionId = tox.sessionId

            // Restore groups immediately after Tox engine starts to ensure background connection recovery
            try {
                restoreGroupsUseCase.execute()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restore groups on startup: ${e.message}", e)
            }

            // Start lifecycle controller only after successful JNI engine startup
            lifecycleController.start(
                lifecycleOwner = this@ToxService,
                onConnectionStatusChanged = { newStatus ->
                    connectionStatus = newStatus
                    if (activeCallState is CallState.Idle && permissionManager.canPostNotifications()) {
                        notifier.notify(NOTIFICATION_ID, notificationFor(connectionStatus))
                    }
                },
                onCallStateChanged = ::handleCallStateChanged
            )
        }
    }

    private fun startInitialForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notificationFor(connectionStatus),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notificationFor(connectionStatus),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(NOTIFICATION_ID, notificationFor(connectionStatus))
        }
    }

    private fun handleCallStateChanged(callState: CallState) {
        activeCallState = callState
        lifecycleScope.launch {
            val contact = when (callState) {
                is CallState.IncomingRinging -> callState.contact
                is CallState.OutgoingRequesting -> contactRepository.get(callState.publicKey.string()).first()
                is CallState.OutgoingWaiting -> contactRepository.get(callState.publicKey.string()).first()
                is CallState.OutgoingRinging -> contactRepository.get(callState.publicKey.string()).first()
                is CallState.Connecting -> contactRepository.get(callState.publicKey.string()).first()
                is CallState.Active -> contactRepository.get(callState.publicKey.string()).first()
                CallState.Idle -> null
            }
            updateForeground(callState, contact)
        }
    }

    @SuppressLint("InlinedApi")
    private fun updateForeground(callState: CallState, contact: Contact?) {
        val inCall = callState !is CallState.Idle
        val defaultType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        }
        val foregroundType = if (inCall) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or defaultType
            } else {
                0
            }
        } else {
            defaultType
        }

        if (inCall && contact != null) {
            startCallForeground(contact, foregroundType, defaultType)
        } else {
            startServiceForeground(defaultType)
        }
    }

    private fun startCallForeground(contact: Contact, foregroundType: Int, defaultType: Int) {
        val notification = callNotificationFactory.buildOngoingCallNotification(contact).build()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID_CALL,
                    notification,
                    foregroundType
                )
            } else {
                startForeground(NOTIFICATION_ID_CALL, notification)
            }
            notifier.cancel(NOTIFICATION_ID)
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to start foreground service with type microphone", e)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        notificationFor(connectionStatus),
                        defaultType
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notificationFor(connectionStatus))
                }
                notifier.cancel(NOTIFICATION_ID_CALL)
            } catch (ex: Exception) {
                Log.e(TAG, "Failed to start fallback foreground service", ex)
            }
        }
    }

    private fun startServiceForeground(defaultType: Int) {
        val notification = notificationFor(connectionStatus)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    defaultType
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            notifier.cancel(NOTIFICATION_ID_CALL)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start service foreground", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleController.stop()
        val currentSessionId = tox.sessionId
        if (currentSessionId != null && currentSessionId == serviceSessionId) {
            tox.stop()
        } else {
            Log.d(TAG, "Skipping tox.stop() in onDestroy: current session ID ($currentSessionId) differs from service session ID ($serviceSessionId)")
        }
    }
}
