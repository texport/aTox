package ltd.evilcorp.atox.ui.navigation

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow

data class AppBarConfig @OptIn(ExperimentalMaterial3Api::class) constructor(
    val title: @Composable () -> Unit = {},
    val navigationIcon: @Composable (() -> Unit)? = null,
    val actions: @Composable (RowScope.() -> Unit)? = null,
    val containerColor: Color? = null,
    val isLarge: Boolean = false,
    val scrollBehavior: TopAppBarScrollBehavior? = null
)

object AppBarStateHolder {
    val config = MutableStateFlow<AppBarConfig?>(null)
}
