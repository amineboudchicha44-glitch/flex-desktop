package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.LaptopChromebook
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.i18n.LocalStrings
import com.example.ui.state.BuiltinAppId
import com.example.ui.theme.LocalFlexDeskTokens

private const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (X11; CrOS x86_64 14816.131.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GoogleWebAppWindowContent(
    app: BuiltinAppId,
    isOnline: Boolean,
    defaultDesktopUserAgent: Boolean
) {
    val context = LocalContext.current
    val strings = LocalStrings.current
    val tokens = LocalFlexDeskTokens.current
    val initialUrl = app.webUrl ?: "https://www.google.com"

    var currentUrl by remember(app.id) { mutableStateOf(initialUrl) }
    var pageTitle by remember(app.id) { mutableStateOf(app.localizedLabel(strings)) }
    var loadingProgress by remember(app.id) { mutableIntStateOf(0) }
    var isLoading by remember(app.id) { mutableStateOf(true) }
    var canGoBack by remember(app.id) { mutableStateOf(false) }
    var canGoForward by remember(app.id) { mutableStateOf(false) }
    var useDesktopMode by remember(app.id) { mutableStateOf(defaultDesktopUserAgent) }
    var webViewRef by remember(app.id) { mutableStateOf<WebView?>(null) }

    val displayHost = remember(currentUrl) {
        runCatching { Uri.parse(currentUrl).host ?: currentUrl }.getOrDefault(currentUrl)
    }

    DisposableEffect(app.id) {
        onDispose {
            webViewRef?.apply {
                stopLoading()
                destroy()
            }
            webViewRef = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("webapp_window_${app.id}")
    ) {
        // ChromeOS PWA Top Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(tokens.windowChromeBackground.copy(alpha = 0.7f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = { webViewRef?.goBack() },
                enabled = canGoBack,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = { webViewRef?.goForward() },
                enabled = canGoForward,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = "Forward",
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = { webViewRef?.reload() },
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = strings.webRetry,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Domain Pill
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, tokens.borderColor, RoundedCornerShape(50))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = "HTTPS",
                        tint = tokens.syncGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "$displayHost • ${strings.webAppBadge}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            FilterChip(
                selected = useDesktopMode,
                onClick = {
                    val next = !useDesktopMode
                    useDesktopMode = next
                    webViewRef?.let { wv ->
                        wv.settings.userAgentString = if (next) DESKTOP_USER_AGENT else null
                        wv.reload()
                    }
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.LaptopChromebook,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                },
                label = { Text(strings.webDesktopSite, style = MaterialTheme.typography.labelSmall) }
            )

            IconButton(
                onClick = {
                    runCatching {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }
                },
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                    contentDescription = strings.webOpenExternal,
                    modifier = Modifier.size(17.dp)
                )
            }
        }

        if (isLoading && loadingProgress in 1..99 && isOnline) {
            LinearProgressIndicator(
                progress = { loadingProgress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
            )
        } else {
            HorizontalDivider(color = tokens.borderColor)
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (!isOnline) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WifiOff,
                        contentDescription = null,
                        tint = tokens.offlineAmber,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = strings.webOfflineTitle,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = strings.webOfflineSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { webViewRef?.loadUrl(currentUrl) }) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.webRetry)
                    }
                }
            } else {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                setSupportZoom(true)
                                builtInZoomControls = true
                                displayZoomControls = false
                                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                                if (useDesktopMode) {
                                    userAgentString = DESKTOP_USER_AGENT
                                }
                            }
                            CookieManager.getInstance().setAcceptCookie(true)
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    isLoading = true
                                    if (!url.isNullOrBlank()) currentUrl = url
                                    canGoBack = view?.canGoBack() == true
                                    canGoForward = view?.canGoForward() == true
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    isLoading = false
                                    if (!url.isNullOrBlank()) currentUrl = url
                                    canGoBack = view?.canGoBack() == true
                                    canGoForward = view?.canGoForward() == true
                                }

                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    val reqUrl = request?.url ?: return false
                                    val scheme = reqUrl.scheme?.lowercase()
                                    if (scheme == "http" || scheme == "https") {
                                        return false // keep navigation inside our Web App window
                                    }
                                    runCatching {
                                        val externalIntent = Intent(Intent.ACTION_VIEW, reqUrl).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        ctx.startActivity(externalIntent)
                                    }
                                    return true
                                }
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    loadingProgress = newProgress
                                }

                                override fun onReceivedTitle(view: WebView?, title: String?) {
                                    if (!title.isNullOrBlank()) pageTitle = title
                                }
                            }

                            webViewRef = this
                            loadUrl(initialUrl)
                        }
                    },
                    update = { wv ->
                        webViewRef = wv
                    }
                )
            }
        }
    }
}
