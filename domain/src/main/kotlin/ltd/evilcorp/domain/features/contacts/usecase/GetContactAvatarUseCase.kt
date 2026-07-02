// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.contacts.usecase

import javax.inject.Inject
import ltd.evilcorp.domain.core.network.IFileStorageProvider
import ltd.evilcorp.domain.features.contacts.repository.IContactRepository
import kotlinx.coroutines.flow.first
import java.io.File

/**
 * Use case to retrieve a contact's avatar file from storage.
 */
class GetContactAvatarUseCase @Inject constructor(
    private val contactRepository: IContactRepository,
    private val fileStorageProvider: IFileStorageProvider,
) {
    suspend fun execute(publicKey: String): File {
        val contact = contactRepository.get(publicKey).first()
        val avatarUri = contact?.avatarUri ?: ""

        if (avatarUri.isEmpty()) {
            return File("")
        }

        val path = fileStorageProvider.getAbsolutePath(avatarUri)
        return if (path != null) {
            File(path)
        } else {
            File("")
        }
    }
}
