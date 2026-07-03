// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.transfer.usecase

import kotlinx.coroutines.flow.Flow
import ltd.evilcorp.domain.features.transfer.model.FileTransfer
import ltd.evilcorp.domain.features.transfer.repository.IFileTransferRepository
import javax.inject.Inject

class GetFileTransferUseCase @Inject constructor(
    private val fileTransferRepository: IFileTransferRepository
) {
    fun execute(id: Int): Flow<FileTransfer> {
        return fileTransferRepository.get(id)
    }
}
