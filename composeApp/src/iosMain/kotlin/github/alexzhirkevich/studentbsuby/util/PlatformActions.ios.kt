package github.alexzhirkevich.studentbsuby.util

import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSCharacterSet
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.URLQueryAllowedCharacterSet
import platform.Foundation.create
import platform.Foundation.stringByAddingPercentEncodingWithAllowedCharacters
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNUserNotificationCenter
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlin.coroutines.resume

class PlatformActionsIos : PlatformActions {

    override fun exitApp() {
        // no-op on iOS per guidelines
    }

    override fun openStorePage() {
        openUrl("https://apps.apple.com/app/id")
    }

    /**
     * Opens [url] with the system. Android-style `geo:` uris are translated to Apple Maps
     * and non-ascii characters (cyrillic addresses) are percent-encoded — [NSURL] refuses to
     * parse them otherwise, which made "show on map" a silent no-op.
     */
    override fun openUrl(url: String) {
        val candidates = mapUrls(url) ?: listOf(url)
        openFirstAvailable(candidates.mapNotNull(::toNSURL))
    }

    private fun openFirstAvailable(urls: List<NSURL>) {
        val nsUrl = urls.firstOrNull() ?: return
        dispatch_async(dispatch_get_main_queue()) {
            UIApplication.sharedApplication.openURL(
                nsUrl,
                options = emptyMap<Any?, Any>()
            ) { success ->
                if (!success) {
                    openFirstAvailable(urls.drop(1))
                }
            }
        }
    }

    override fun shareFile(path: String, mime: String) {
        runCatching {
            val nsUrl = NSURL.fileURLWithPath(path)
            val activityVC = UIActivityViewController(activityItems = listOf(nsUrl), applicationActivities = null)
            dispatch_async(dispatch_get_main_queue()) {
                UIApplication.sharedApplication.keyWindow?.rootViewController
                    ?.presentViewController(activityVC, animated = true, completion = null)
            }
        }
    }

    override suspend fun requestNotificationsPermission(): Boolean = suspendCancellableCoroutine { continuation ->
        val center = UNUserNotificationCenter.currentNotificationCenter()
        val options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
        center.requestAuthorizationWithOptions(options) { granted, _ ->
            continuation.resume(granted)
        }
    }

    private fun mapUrls(url: String): List<String>? {
        if (!url.startsWith("geo:", ignoreCase = true))
            return null
        val query = url.substringAfter("?q=", "")
            .ifEmpty { url.substringAfter("geo:").substringBefore('?') }
        val encoded = percentEncode(query)
        return listOf(
            "maps://?q=$encoded",
            "https://maps.apple.com/?q=$encoded",
            "https://www.google.com/maps/search/?api=1&query=$encoded"
        )
    }

    private fun toNSURL(url: String): NSURL? =
        NSURL.URLWithString(url) ?: NSURL.URLWithString(percentEncode(url, keepReserved = true))

    private fun percentEncode(value: String, keepReserved: Boolean = false): String {
        val allowed = if (keepReserved)
            NSCharacterSet.URLQueryAllowedCharacterSet
        else NSCharacterSet.alphanumericCharacterSet()
        @Suppress("CAST_NEVER_SUCCEEDS")
        return (value as NSString)
            .stringByAddingPercentEncodingWithAllowedCharacters(allowed)
            ?: value
    }
}
