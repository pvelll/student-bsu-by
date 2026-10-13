package github.sushkpavel.studentbsuby.services.firebase

interface RemoteConfigClient {

    suspend fun fetchAndActivate(): Boolean

    fun getString(key: String): String
}
