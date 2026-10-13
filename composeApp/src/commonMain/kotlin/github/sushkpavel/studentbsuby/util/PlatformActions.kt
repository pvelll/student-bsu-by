package github.sushkpavel.studentbsuby.util

interface PlatformActions {
    fun exitApp()
    fun openStorePage()
    fun openUrl(url: String)
    fun shareFile(path: String, mime: String)
    suspend fun requestNotificationsPermission(): Boolean
}
