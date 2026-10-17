@file:OptIn(ExperimentalForeignApi::class)

package github.sushkpavel.studentbsuby.util

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UISceneActivationStateForegroundActive
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.UIKit.popoverPresentationController
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * The controller to present from: the top one in the key window of the foreground scene.
 * UIApplication.keyWindow is deprecated and ambiguous when there are several scenes or
 * displays (iPad multitasking, the two displays of iPhone Duo).
 */
internal fun topViewController(): UIViewController? {
    val scenes = UIApplication.sharedApplication.connectedScenes.filterIsInstance<UIWindowScene>()
    val scene = scenes.firstOrNull { it.activationState == UISceneActivationStateForegroundActive }
        ?: scenes.firstOrNull()
    val window = scene?.keyWindow ?: scene?.windows?.filterIsInstance<UIWindow>()?.firstOrNull()
    var controller = window?.rootViewController
    while (controller?.presentedViewController != null)
        controller = controller.presentedViewController
    return controller
}

/**
 * System share sheet. In a regular width (iPad, the inner display of iPhone Duo) it is shown as
 * a popover, and UIKit throws when a popover has no anchor: the app crashed on "share".
 */
internal fun presentShareSheet(items: List<Any>) {
    dispatch_async(dispatch_get_main_queue()) {
        val presenter = topViewController() ?: return@dispatch_async
        val controller = UIActivityViewController(activityItems = items, applicationActivities = null)
        controller.popoverPresentationController?.let { popover ->
            val view = presenter.view
            popover.sourceView = view
            view.bounds.useContents {
                popover.sourceRect = CGRectMake(size.width / 2, size.height / 2, 0.0, 0.0)
            }
            popover.permittedArrowDirections = 0uL
        }
        presenter.presentViewController(controller, animated = true, completion = null)
    }
}
