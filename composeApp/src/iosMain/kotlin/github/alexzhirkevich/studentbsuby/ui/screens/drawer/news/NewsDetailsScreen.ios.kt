package github.alexzhirkevich.studentbsuby.ui.screens.drawer.news

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
import github.alexzhirkevich.studentbsuby.data.models.NewsContent
import github.alexzhirkevich.studentbsuby.repo.DataSource
import github.alexzhirkevich.studentbsuby.repo.NewsRepository
import github.alexzhirkevich.studentbsuby.resources.Res
import github.alexzhirkevich.studentbsuby.resources.error_load_news
import github.alexzhirkevich.studentbsuby.resources.something_gone_wrong
import github.alexzhirkevich.studentbsuby.ui.common.BsuProgressBar
import github.alexzhirkevich.studentbsuby.ui.common.ErrorWidget
import github.alexzhirkevich.studentbsuby.ui.theme.values.Colors
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.catch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import platform.Foundation.NSURL
import platform.WebKit.WKWebView

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun NewsDetailsScreen(id: Int, viewModel: NewsViewModel) {
    val newsRepository = koinInject<NewsRepository>()

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
                factory = { WKWebView() },
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
