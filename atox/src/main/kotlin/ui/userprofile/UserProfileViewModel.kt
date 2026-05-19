// SPDX-FileCopyrightText: 2020 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.userprofile

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import ltd.evilcorp.core.model.User
import ltd.evilcorp.core.model.UserStatus
import ltd.evilcorp.domain.feature.UserManager
import ltd.evilcorp.domain.tox.Tox
import ltd.evilcorp.domain.feature.FileTransferManager

class UserProfileViewModel @Inject constructor(
    private val userManager: UserManager,
    private val tox: Tox,
    private val fileTransferManager: FileTransferManager
) : ViewModel() {
    val publicKey by lazy { tox.publicKey }
    val toxId by lazy { tox.toxId }
    val user: LiveData<User?> = userManager.get(publicKey).asLiveData()

    fun setName(name: String) = userManager.setName(name)
    fun setStatusMessage(statusMessage: String) = userManager.setStatusMessage(statusMessage)
    fun setStatus(status: UserStatus) = userManager.setStatus(status)

    fun broadcastAvatar() {
        viewModelScope.launch {
            fileTransferManager.broadcastAvatar()
        }
    }
}
