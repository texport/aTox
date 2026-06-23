// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.common.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ReactionCount(
    val emoji: String,
    val count: Int
)

@Composable
fun ReactionsRow(
    reactions: List<ReactionCount>,
    onReactionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (reactions.isEmpty()) return

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(top = 2.dp)
    ) {
        for (reaction in reactions) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                    .clickable { onReactionClick(reaction.emoji) }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = reaction.emoji,
                    fontSize = 14.sp,
                    maxLines = 1
                )
                if (reaction.count > 1) {
                    Text(
                        text = reaction.count.toString(),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
