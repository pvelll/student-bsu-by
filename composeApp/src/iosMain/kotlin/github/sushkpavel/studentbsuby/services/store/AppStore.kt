package github.sushkpavel.studentbsuby.services.store

import platform.Foundation.NSBundle

internal fun appStoreUrl(): String? =
    (NSBundle.mainBundle.infoDictionary?.get("AppStoreID") as? String)
        ?.takeIf { it.isNotEmpty() && it.all(Char::isDigit) }
        ?.let { "itms-apps://apps.apple.com/app/id$it" }
