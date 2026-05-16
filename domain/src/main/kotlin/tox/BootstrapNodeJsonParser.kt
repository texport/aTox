// SPDX-FileCopyrightText: 2021-2025 Robin Lindén <dev@robinlinden.eu>
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.tox

import android.util.Log
import javax.inject.Inject
import ltd.evilcorp.core.vo.PublicKey
import org.json.JSONObject

private const val TAG = "BootstrapNodeJsonParser"

// Parses a json string containing json formatted the way it is on https://nodes.tox.chat/json
class BootstrapNodeJsonParser @Inject constructor() {
    fun parse(jsonString: String): List<BootstrapNode> = try {
        val nodes = mutableListOf<BootstrapNode>()

        val json = JSONObject(jsonString)
        val jsonNodes = json.getJSONArray("nodes")
        for (i in 0 until jsonNodes.length()) {
            val jsonNode = jsonNodes.getJSONObject(i)
            val isOnline = jsonNode.getBoolean("status_udp") && jsonNode.getBoolean("status_tcp")
            val address = jsonNode.getString("ipv4")
            if (isOnline && address != "-") {
                nodes.add(
                    BootstrapNode(
                        address = address,
                        port = jsonNode.getInt("port"),
                        publicKey = PublicKey(jsonNode.getString("public_key")),
                    ),
                )
            }
        }

        nodes
    } catch (e: Exception) {
        Log.e(TAG, e.toString())
        listOf()
    }
}
