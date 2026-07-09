package one.secureai.app.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.Uri
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView

private const val BASE_URL = "https://secureai.one"
private const val CHAT_URL = "$BASE_URL/chat"

fun isOnline(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    return cm.activeNetworkInfo?.isConnected == true
}

// Bridge so the web app can call native Android functions
class NativeBridge(
    private val onOpenSettings: () -> Unit
) {
    @JavascriptInterface
    fun openSettings() = onOpenSettings()

    @JavascriptInterface
    fun getAppVersion() = "android"
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ChatWebViewScreen(deepLinkUrl: String? = null, onOpenSettings: () -> Unit = {}) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var loadingProgress by remember { mutableFloatStateOf(0f) }
    var isLoading by remember { mutableStateOf(true) }
    var isOffline by remember { mutableStateOf(false) }
    var fileUploadCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        fileUploadCallback?.onReceiveValue(uris.toTypedArray())
        fileUploadCallback = null
    }

    BackHandler(enabled = webView?.canGoBack() == true) {
        webView?.goBack()
    }

    if (isOffline) {
        OfflineScreen(onRetry = {
            isOffline = false
            webView?.reload()
        })
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        allowFileAccess = true
                        mediaPlaybackRequiresUserGesture = false
                        setSupportZoom(false)
                        builtInZoomControls = false
                        displayZoomControls = false
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        userAgentString = "$userAgentString SecureAI-Android/1.0"
                    }

                    // Native bridge — web app can call window.SecureAI.openSettings()
                    addJavascriptInterface(NativeBridge(onOpenSettings), "SecureAI")

                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                            val url = request.url.toString()
                            return if (url.startsWith(BASE_URL)) {
                                false
                            } else {
                                context.startActivity(
                                    android.content.Intent(android.content.Intent.ACTION_VIEW, request.url)
                                )
                                true
                            }
                        }

                        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                            isLoading = true
                            isOffline = false
                        }

                        override fun onPageFinished(view: WebView, url: String) {
                            isLoading = false
                        }

                        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                            if (request.isForMainFrame) isOffline = !isOnline(context)
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView, newProgress: Int) {
                            loadingProgress = newProgress / 100f
                        }

                        override fun onPermissionRequest(request: PermissionRequest) {
                            request.grant(request.resources)
                        }

                        override fun onShowFileChooser(
                            webView: WebView,
                            filePathCallback: ValueCallback<Array<Uri>>,
                            fileChooserParams: FileChooserParams
                        ): Boolean {
                            fileUploadCallback = filePathCallback
                            filePickerLauncher.launch("*/*")
                            return true
                        }
                    }

                    loadUrl(deepLinkUrl?.takeIf { it.startsWith(BASE_URL) } ?: CHAT_URL)
                    webView = this
                }
            }
        )

        if (isLoading && loadingProgress < 1f) {
            LinearProgressIndicator(
                progress = { loadingProgress },
                modifier = Modifier.align(Alignment.TopCenter),
                color = Color(0xFF2563EB),
                trackColor = Color.Transparent
            )
        }
    }
}
