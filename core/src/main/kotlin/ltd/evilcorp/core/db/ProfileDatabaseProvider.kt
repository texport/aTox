package ltd.evilcorp.core.db

import android.content.Context
import androidx.room.Room
import javax.inject.Inject
import javax.inject.Singleton
import javax.inject.Qualifier
import ltd.evilcorp.core.profile.ProfileManager

@Qualifier
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.FUNCTION)
annotation class DatabaseCloseOnSwitch

@Singleton
class ProfileDatabaseProvider @Inject constructor(
    private val context: Context,
    @DatabaseCloseOnSwitch private val closeOnSwitch: Boolean = false,
) {
    private var currentDb: Database? = null
    private var currentProfileId: String? = null

    @Synchronized
    fun getDatabase(): Database {
        val profileId = ProfileManager.getActiveProfileId(context)
        val cachedDb = currentDb
        if (currentProfileId == profileId && cachedDb != null) {
            return cachedDb
        }

        // Safely close the old database connection before switching (skip during tests to avoid CloseBarrier crashes from active Flows)
        if (closeOnSwitch) {
            try {
                currentDb?.close()
            } catch (e: Exception) {
                android.util.Log.e("ProfileDatabaseProvider", "Error closing database during switch", e)
            }
        }

        val targetDbName = if (profileId == ProfileManager.DEFAULT_PROFILE_ID) "core_db" else "core_db_$profileId"
        val dbDir = context.getDatabasePath(targetDbName).parentFile
        val existingDbName = if (dbDir?.exists() == true) {
            dbDir.listFiles()?.find { it.name.equals(targetDbName, ignoreCase = true) }?.name
        } else {
            null
        }
        val dbName = existingDbName ?: targetDbName
        val db = Room.databaseBuilder(context, Database::class.java, dbName)
            .addMigrations(*ALL_MIGRATIONS)
            .build()

        currentDb = db
        currentProfileId = profileId
        return db
    }

    @Synchronized
    fun closeDatabase() {
        if (closeOnSwitch) {
            try {
                currentDb?.close()
            } catch (e: Exception) {
                android.util.Log.e("ProfileDatabaseProvider", "Error closing database", e)
            }
        }
        currentDb = null
        currentProfileId = null
    }
}

