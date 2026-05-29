package ltd.evilcorp.atox.ui.common

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FormErrorText(
    errorText: String,
    modifier: Modifier = Modifier
) {
    if (errorText.isNotEmpty()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = errorText,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = modifier
        )
    }
}
