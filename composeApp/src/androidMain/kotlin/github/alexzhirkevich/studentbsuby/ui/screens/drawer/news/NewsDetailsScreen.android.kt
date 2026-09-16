package github.alexzhirkevich.studentbsuby.ui.screens.drawer.news

import android.content.Intent
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import github.alexzhirkevich.studentbsuby.data.models.NewsContent
import github.alexzhirkevich.studentbsuby.repo.DataSource
import github.alexzhirkevich.studentbsuby.repo.NewsRepository
import github.alexzhirkevich.studentbsuby.resources.Res
import github.alexzhirkevich.studentbsuby.resources.error_load_news
import github.alexzhirkevich.studentbsuby.resources.something_gone_wrong
import github.alexzhirkevich.studentbsuby.ui.common.BsuProgressBar
import github.alexzhirkevich.studentbsuby.ui.common.ErrorWidget
import github.alexzhirkevich.studentbsuby.ui.theme.values.Colors
import github.alexzhirkevich.studentbsuby.util.LoginCookieManager
import kotlinx.coroutines.flow.catch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
actual fun NewsDetailsScreen(id: Int, viewModel: NewsViewModel) {
    val newsRepository = koinInject<NewsRepository>()
    val loginCookieManager = koinInject<LoginCookieManager>()

    var content by remember(id) { mutableStateOf<NewsContent?>(null) }
    var failed by remember(id) { mutableStateOf(false) }

    LaunchedEffect(id) {
        newsRepository.getNewsItem(id, DataSource.All)
            .catch { failed = true }
            .collect { content = it }
    }

    val current = content
    when {
        current != null -> NewsWebView(
            content = current,
            baseUrl = newsRepository.newsUrl,
            cookies = loginCookieManager.getCookies(),
            cookiesUrl = newsRepository.baseUrl
        )
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

@Composable
private fun NewsWebView(
    content: NewsContent,
    baseUrl: String,
    cookies: String,
    cookiesUrl: String,
) {
    val bgColor = Colors.GrayBackground

    Box(
        Modifier
            .fillMaxSize()
            .background(bgColor)
            .verticalScroll(rememberScrollState())
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
                .windowInsetsPadding(WindowInsets.navigationBars),
            factory = { context ->
                WebView(context).apply {
                    setBackgroundColor(bgColor.toArgb())
                    CookieManager.getInstance().setCookie(cookiesUrl, cookies)
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest
                        ): Boolean {
                            runCatching {
                                view.context.startActivity(
                                    Intent(Intent.ACTION_VIEW, request.url)
                                )
                            }
                            return true
                        }
                    }
                }
            },
            update = { webView ->
                if (webView.tag != content.id) {
                    webView.tag = content.id
                    webView.loadDataWithBaseURL(
                        baseUrl,
                        content.content,
                        "text/html",
                        "UTF-8",
                        null
                    )
                }
            }
        )
    }
}
