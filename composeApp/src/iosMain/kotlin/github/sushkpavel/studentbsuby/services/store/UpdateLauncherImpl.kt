package github.sushkpavel.studentbsuby.services.store

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.*
import platform.UIKit.*

class UpdateLauncherImpl : UpdateLauncher {
    override suspend fun tryUpdate(
        immediate : Boolean,
        onFailedToInAppUpdate : () -> Unit,
    ) {
        if (!immediate)
            return

        kotlin.runCatching {
            val url = appStoreUrl()?.let { NSURL.URLWithString(it) }

            withContext(Dispatchers.Main) {
                if (url != null) {
                    UIApplication.sharedApplication.openURL(
                        url,
                        options = emptyMap<Any?, Any>(),
                        completionHandler = null
                    )
                } else {
                    onFailedToInAppUpdate()
                }
            }
        }
    }
}
