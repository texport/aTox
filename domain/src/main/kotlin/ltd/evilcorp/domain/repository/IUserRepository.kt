package ltd.evilcorp.domain.repository

import kotlinx.coroutines.flow.Flow
import ltd.evilcorp.domain.model.UserStatus
import ltd.evilcorp.domain.model.User

interface IUserRepository {
    fun get(publicKey: String): Flow<User?>
    fun add(user: User)
    fun exists(publicKey: String): Boolean
    fun updateName(publicKey: String, name: String)
    fun updateStatusMessage(publicKey: String, statusMessage: String)
    fun updateStatus(publicKey: String, status: UserStatus)
}
