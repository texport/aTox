// SPDX-FileCopyrightText: 2021 Robin Lindén <dev@robinlinden.eu>
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.infrastructure.util

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object PendingIntentCompat {
    fun getBroadcast(
        context: Context,
        requestCode: Int,
        intent: Intent,
        flags: Int,
        mutable: Boolean = false,
    ): PendingIntent {
        val mutabilityFlag =
            if (mutable) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
            } else {
                PendingIntent.FLAG_IMMUTABLE
            }
        return PendingIntent.getBroadcast(context, requestCode, intent, flags or mutabilityFlag)
    }

    fun getActivity(context: Context, requestCode: Int, intent: Intent, flags: Int): PendingIntent =
        PendingIntent.getActivity(context, requestCode, intent, flags or PendingIntent.FLAG_IMMUTABLE)
}
