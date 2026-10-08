package github.sushkpavel.studentbsuby.ui.screens.drawer.news

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitView
import github.sushkpavel.studentbsuby.data.models.NewsContent
import github.sushkpavel.studentbsuby.repo.DataSource
import github.sushkpavel.studentbsuby.repo.NewsRepository
import github.sushkpavel.studentbsuby.resources.Res
import github.sushkpavel.studentbsuby.resources.error_load_news
import github.sushkpavel.studentbsuby.resources.something_gone_wrong
import github.sushkpavel.studentbsuby.ui.common.BsuProgressBar
import github.sushkpavel.studentbsuby.ui.common.ErrorWidget
import github.sushkpavel.studentbsuby.ui.theme.values.Colors
import github.sushkpavel.studentbsuby.util.PlatformActions
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.catch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import platform.Foundation.NSURL
import platform.WebKit.WKNavigationAction
import platform.WebKit.WKNavigationActionPolicy
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKNavigationTypeLinkActivated
import platform.WebKit.WKWebView
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun NewsDetailsScreen(id: Int, viewModel: NewsViewModel) {
    val newsRepository = koinInject<NewsRepository>()
    val platformActions = koinInject<PlatformActions>()
    // WKWebView keeps its navigation delegate weakly.
    val navigationDelegate = remember { ExternalLinksDelegate(platformActions::openUrl) }

    var content by remember(id) { mutableStateOf<NewsContent?>(null) }
    var failed by remember(id) { mutableStateOf(false) }

    LaunchedEffect(id) {
        newsRepository.getNewsItem(id, DataSource.All)
            .catch { failed = true }
            .collect { content = it }
    }

    val current = content
    when {
        current != null -> {
            var loadedId by remember { mutableStateOf<Int?>(null) }
            UIKitView(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Colors.GrayBackground),
                factory = { WKWebView().apply { this.navigationDelegate = navigationDelegate } },
                update = { webView ->
                    if (loadedId != current.id) {
                        loadedId = current.id
                        webView.loadHTMLString(
                            current.content,
                            baseURL = NSURL.URLWithString(newsRepository.newsUrl)
                        )
                    }
                }
            )
        }
        failed -> Box(Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {
            ErrorWidget(
                title = stringResource(Res.string.something_gone_wrong),
                error = stringResource(Res.string.error_load_news),
                modifier = Modifier.align(Alignment.Center).padding(30.dp)
            )
        }
        else -> Box(Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {
            BsuProgressBar(
                tint = MaterialTheme.colors.primary,
                size = 100.dp,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

/**
 * Opens tapped links in the system browser (as on Android) instead of navigating the
 * news web view, which has no address bar or back button.
 */
private class ExternalLinksDelegate(
    private val openUrl: (String) -> Unit
) : NSObject(), WKNavigationDelegateProtocol {

    override fun webView(
        webView: WKWebView,
        decidePolicyForNavigationAction: WKNavigationAction,
        decisionHandler: (WKNavigationActionPolicy) -> Unit
    ) {
        val url = decidePolicyForNavigationAction.request.URL?.absoluteString
        if (url != null &&
            decidePolicyForNavigationAction.navigationType == WKNavigationTypeLinkActivated
        ) {
            openUrl(url)
            decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyCancel)
        } else {
            decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyAllow)
        }
    }
}
