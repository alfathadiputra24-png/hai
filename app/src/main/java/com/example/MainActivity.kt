package com.example

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.auto(
        AndroidColor.TRANSPARENT,
        AndroidColor.TRANSPARENT
      ),
      navigationBarStyle = SystemBarStyle.auto(
        AndroidColor.TRANSPARENT,
        AndroidColor.TRANSPARENT
      )
    )
    setContent {
      MyApplicationTheme {
        BiteWebAppScreen(onExitApp = { finish() })
      }
    }
  }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BiteWebAppScreen(onExitApp: () -> Unit) {
  var webViewRef by remember { mutableStateOf<WebView?>(null) }
  var pageLoaded by remember { mutableStateOf(false) }

  val density = LocalDensity.current
  val topInsetDp = with(density) { WindowInsets.statusBars.getTop(this).toDp().value }
  val bottomInsetDp = with(density) { WindowInsets.navigationBars.getBottom(this).toDp().value }

  LaunchedEffect(pageLoaded, topInsetDp, bottomInsetDp, webViewRef) {
    if (pageLoaded) {
      webViewRef?.evaluateJavascript(
        "document.documentElement.style.setProperty('--safe-top', '${topInsetDp}px');" +
          "document.documentElement.style.setProperty('--safe-bottom', '${bottomInsetDp}px');",
        null
      )
    }
  }

  BackHandler(enabled = true) {
    val wv = webViewRef
    if (wv == null) {
      onExitApp()
      return@BackHandler
    }
    wv.evaluateJavascript("(function(){ return window.__biteHandleBack ? window.__biteHandleBack() : false; })()") { result ->
      if (result != "true") {
        onExitApp()
      }
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xFF9B0A0A))
  ) {
    AndroidView(
      modifier = Modifier.fillMaxSize(),
      factory = { context ->
        WebView(context).apply {
          setBackgroundColor(AndroidColor.parseColor("#9B0A0A"))
          overScrollMode = View.OVER_SCROLL_NEVER
          isVerticalScrollBarEnabled = false
          isHorizontalScrollBarEnabled = false

          settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            loadsImagesAutomatically = true
            allowFileAccess = true
            allowContentAccess = true
            useWideViewPort = true
            loadWithOverviewMode = true
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            mediaPlaybackRequiresUserGesture = false
          }

          webChromeClient = WebChromeClient()
          webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
              super.onPageFinished(view, url)
              pageLoaded = true
              view?.evaluateJavascript(
                "document.documentElement.style.setProperty('--safe-top', '${topInsetDp}px');" +
                  "document.documentElement.style.setProperty('--safe-bottom', '${bottomInsetDp}px');",
                null
              )
            }

            override fun shouldOverrideUrlLoading(
              view: WebView?,
              request: WebResourceRequest?
            ): Boolean {
              val uri = request?.url ?: return false
              val scheme = uri.scheme?.lowercase() ?: ""
              if (scheme == "file") return false
              if (scheme == "tel" || scheme == "mailto") {
                runCatching {
                  context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                }
                return true
              }
              return false
            }
          }

          loadUrl("file:///android_asset/index.html")
          webViewRef = this
        }
      },
      update = { webView ->
        webViewRef = webView
      }
    )
  }
}
