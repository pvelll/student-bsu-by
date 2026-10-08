package github.sushkpavel.studentbsuby.services.store

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.*
import platform.UIKit.*

class UpdateLauncherImpl : UpdateLauncher {

    // There are no in-app updates on iOS. Update availability can't be checked
    // either, so only forced (immediate) updates lead to the App Store page.
    override suspend fun tryUpdate(
        immediate : Boolean,
        onFailedToInAppUpdate : () -> Unit,
    ) {
        if (!immediate)
            return

        kotlin.runCatching {
            // Without an App Store id forced updates fall back to onFailedToInAppUpdate.
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
