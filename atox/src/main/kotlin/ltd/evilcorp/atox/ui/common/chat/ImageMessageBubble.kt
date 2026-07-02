package ltd.evilcorp.atox.ui.common.chat

import android.content.ContentResolver
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ltd.evilcorp.atox.ui.common.LocalFileStorageProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ltd.evilcorp.domain.features.transfer.model.FileTransfer

private const val MAX_IMAGE_HEIGHT = 320

@Composable
fun ImageMessageBubble(ft: FileTransfer, shape: Shape = RoundedCornerShape(12.dp)) {
    val context = LocalContext.current
    val fileStorageProvider = LocalFileStorageProvider.current
    var showFullScreen by remember { mutableStateOf(false) }

    val imageBitmap by produceState<ImageBitmap?>(initialValue = null, ft.destination) {
        value = withContext(Dispatchers.IO) {
            try {
                val uri = android.net.Uri.parse(ft.destination)
                val inputStream = if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
                    context.contentResolver.openInputStream(uri)
                } else {
                    val path = fileStorageProvider.getAbsolutePath(ft.destination)
                    if (path != null && fileStorageProvider.exists(ft.destination)) {
                        java.io.FileInputStream(path)
                    } else null
                }
                inputStream?.use { stream ->
                    val bytes = stream.readBytes()
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                }
            } catch (e: Exception) {
                android.util.Log.e("ImageMessageBubble", "Failed to load image", e)
                null
            }
        }
    }

    if (showFullScreen) {
        FileImageViewerDialog(
            destination = ft.destination,
            fileName = ft.fileName,
            onDismiss = { showFullScreen = false }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = MAX_IMAGE_HEIGHT.dp)
            .clip(shape)
            .clickable(enabled = imageBitmap != null) { showFullScreen = true }
            .semantics { contentDescription = ft.fileName },
        contentAlignment = Alignment.Center
    ) {
        imageBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap,
                contentDescription = ft.fileName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } ?: run {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
