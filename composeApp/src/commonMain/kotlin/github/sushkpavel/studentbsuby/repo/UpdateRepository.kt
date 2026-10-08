package github.sushkpavel.studentbsuby.repo

import github.sushkpavel.studentbsuby.services.store.UpdateLauncher

class UpdateRepository(
    private val updateLauncher: UpdateLauncher,
) {

    suspend fun tryUpdate(
        immediate : Boolean,
        onFailedToInAppUpdate : () -> Unit,
    ) = kotlin.runCatching {
        updateLauncher.tryUpdate(
            immediate = immediate,
            onFailedToInAppUpdate = onFailedToInAppUpdate
        )
    }
}
