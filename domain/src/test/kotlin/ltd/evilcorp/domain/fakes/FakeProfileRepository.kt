package ltd.evilcorp.domain.fakes

import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.features.auth.repository.IProfileRepository

class FakeProfileRepository : IProfileRepository {
    var deletedProfilePk: PublicKey? = null
    var databaseCleared = false
    var checkpointCreated = false
    var checkpointCleared = false
    var restoredFromCheckpoint = false

    var checkpointCreateResult = true
    var checkpointRestoreResult = true

    override suspend fun deleteProfile(publicKey: PublicKey) {
        deletedProfilePk = publicKey
    }

    override suspend fun clearDatabase() {
        databaseCleared = true
    }

    override suspend fun createCheckpoint(): Boolean {
        checkpointCreated = true
        return checkpointCreateResult
    }

    override suspend fun restoreFromCheckpoint(): Boolean {
        restoredFromCheckpoint = true
        return checkpointRestoreResult
    }

    override suspend fun clearCheckpoint() {
        checkpointCleared = true
    }
}
