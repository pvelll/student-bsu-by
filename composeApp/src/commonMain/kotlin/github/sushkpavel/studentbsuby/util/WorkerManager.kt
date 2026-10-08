package github.sushkpavel.studentbsuby.util

interface WorkerManager {

    suspend fun isEnabled() : Boolean

    fun run()

    fun stop()
}
