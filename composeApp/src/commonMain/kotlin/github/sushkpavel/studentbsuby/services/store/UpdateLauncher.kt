package github.sushkpavel.studentbsuby.services.store

interface UpdateLauncher {

    suspend fun tryUpdate(
        immediate : Boolean,
        onFailedToInAppUpdate : () -> Unit,
    )
}
