package com.example.nikkxmovie

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback

class MainActivity : ComponentActivity() {
  private lateinit var webView: WebView
  private var customView: View? = null
  private var customViewCallback: WebChromeClient.CustomViewCallback? = null
  private var originalSystemUiVisibility = 0
  private var originalOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

  @SuppressLint("SetJavaScriptEnabled")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Show welcome toast message
    Toast.makeText(this, "Welcome to NikkXMovies", Toast.LENGTH_LONG).show()

    webView = WebView(this).apply {
      layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
      )

      webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
          if (url != null && (url.startsWith("intent:") || url.startsWith("intent://"))) {
            try {
              val intent = android.content.Intent.parseUri(url, android.content.Intent.URI_INTENT_SCHEME)
              if (intent != null) {
                try {
                  view?.context?.startActivity(intent)
                } catch (e: Exception) {
                  // Fallback: Clear package to let user play in any available video player (VLC, MX, etc.)
                  intent.setPackage(null)
                  try {
                    view?.context?.startActivity(intent)
                  } catch (e2: Exception) {
                    Toast.makeText(view?.context, "No video player found. Please install VLC or MX Player.", Toast.LENGTH_LONG).show()
                  }
                }
                return true
              }
            } catch (e: Exception) {
              Toast.makeText(view?.context, "Invalid play link", Toast.LENGTH_SHORT).show()
              return true
            }
          }
          return false
        }
      }

      webChromeClient = object : WebChromeClient() {
        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
          if (customView != null) {
            onHideCustomView()
            return
          }
          customView = view
          customViewCallback = callback
          originalOrientation = requestedOrientation
          originalSystemUiVisibility = window.decorView.systemUiVisibility

          val decor = window.decorView as FrameLayout
          decor.addView(customView, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
          ))

          requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
          window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
          )
        }

        override fun onHideCustomView() {
          val decor = window.decorView as FrameLayout
          decor.removeView(customView)
          customView = null
          customViewCallback?.onCustomViewHidden()
          customViewCallback = null

          requestedOrientation = originalOrientation
          window.decorView.systemUiVisibility = originalSystemUiVisibility
        }
      }

      settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        databaseEnabled = true
        allowContentAccess = true
        allowFileAccess = true
        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        useWideViewPort = true
        loadWithOverviewMode = true
        mediaPlaybackRequiresUserGesture = false
      }

      // Loads public Render server so the app works on real mobile phones anywhere
      loadUrl("https://nikkxmovie-sxkq.onrender.com")
    }

    setContentView(webView)

    // Handle back press event
    onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
      override fun handleOnBackPressed() {
        if (customView != null) {
          webView.webChromeClient?.onHideCustomView()
        } else if (webView.canGoBack()) {
          webView.goBack()
        } else {
          isEnabled = false
          onBackPressedDispatcher.onBackPressed()
        }
      }
    })
  }
}
