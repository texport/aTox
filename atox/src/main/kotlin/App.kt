package ltd.evilcorp.atox

import android.app.Application
import androidx.annotation.VisibleForTesting
import ltd.evilcorp.atox.appearance.AppearanceManager
import ltd.evilcorp.atox.di.AppComponent
import ltd.evilcorp.atox.di.DaggerAppComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class App : Application() {
    val component: AppComponent by lazy {
        componentOverride ?: DaggerAppComponent.factory().create(applicationContext)
    }

    @VisibleForTesting
    var componentOverride: AppComponent? = null

    override fun onCreate() {
        super.onCreate()
        AppearanceManager.applyPersistedAppearance(this)

        // Background cleanup of shared cache files on startup to prevent leaks
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val sharedCacheDir = File(cacheDir, "shared")
                if (sharedCacheDir.exists() && sharedCacheDir.isDirectory) {
                    sharedCacheDir.listFiles()?.forEach { file ->
                        file.delete()
                    }
                }
            }
        }
    }
}
