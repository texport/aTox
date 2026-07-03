// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.groupchat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.ui.common.ContactAvatar
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.features.group.model.GroupPeer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupPeersSheet(
    onDismissRequest: () -> Unit,
    peers: List<GroupPeer>,
    contacts: List<Contact>,
    selfAvatarUri: String
) {
    val sortedPeers = remember(peers) {
        peers.sortedWith(
            compareByDescending<GroupPeer> { it.isOurselves }
                .thenBy { it.name }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.group_info),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)
            ) {
                items(sortedPeers) { peer ->
                    val matchingContact = contacts.find { it.publicKey.equals(peer.publicKey, ignoreCase = true) }
                    val isOnline = when {
                        peer.isOurselves -> true
                        matchingContact != null -> matchingContact.connectionStatus != ConnectionStatus.None
                        else -> true
                    }
                    val statusColor = if (isOnline) ltd.evilcorp.atox.ui.theme.StatusAvailable else ltd.evilcorp.atox.ui.theme.StatusOffline

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        ListItem(
                            leadingContent = {
                                Box(modifier = Modifier.size(40.dp)) {
                                    ContactAvatar(
                                        name = peer.name.ifEmpty { stringResource(R.string.contact_default_name) },
                                        publicKey = peer.publicKey,
                                        avatarUri = if (peer.isOurselves) selfAvatarUri else (matchingContact?.avatarUri ?: ""),
                                        size = 40.dp,
                                        fontSize = 15.sp
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(statusColor, shape = CircleShape)
                                            .border(1.5.dp, MaterialTheme.colorScheme.surface, shape = CircleShape)
                                            .align(androidx.compose.ui.Alignment.BottomEnd)
                                    )
                                }
                            },
                            headlineContent = {
                                Text(
                                    text = peer.name.ifEmpty { stringResource(R.string.contact_default_name) },
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (peer.isOurselves) FontWeight.Bold else FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
