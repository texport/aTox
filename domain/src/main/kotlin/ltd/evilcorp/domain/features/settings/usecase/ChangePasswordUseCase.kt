package ltd.evilcorp.domain.features.settings.usecase

import ltd.evilcorp.domain.core.network.ITox
import javax.inject.Inject

class ChangePasswordUseCase @Inject constructor(
    private val tox: ITox,
) {
    fun execute(newPassword: String?) {
        tox.changePassword(newPassword)
    }
}
