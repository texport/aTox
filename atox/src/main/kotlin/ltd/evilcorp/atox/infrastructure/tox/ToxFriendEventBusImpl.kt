// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.infrastructure.tox

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import ltd.evilcorp.domain.features.contacts.IToxFriendEventBus
import ltd.evilcorp.domain.features.contacts.model.ToxFriendEvent

private const val TAG = "ToxFriendEventBus"

@Singleton
class ToxFriendEventBusImpl @Inject constructor() : IToxFriendEventBus {
    private val _events = MutableSharedFlow<ToxFriendEvent>(extraBufferCapacity = 128)
    override val events: SharedFlow<ToxFriendEvent> = _events.asSharedFlow()

    override fun tryEmit(event: ToxFriendEvent): Boolean {
        val success = _events.tryEmit(event)
        if (!success) {
            Log.w(TAG, "Failed to emit event (dropped due to buffer overflow): $event")
        }
        return success
    }
}
