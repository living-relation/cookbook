package com.livingrelation.markdownnotes.feature.reader

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import org.json.JSONArray
import org.json.JSONObject
import com.livingrelation.markdownnotes.core.markdown.TocEntry

@SuppressLint("SetJavaScriptEnabled")
class MarkdownWebView(context: android.content.Context) : WebView(context) {
    private var pageReady = false
    private var pendingMarkdown: String? = null
    private var pendingImageBase: String = ""
    private var pendingToc: List<TocEntry> = emptyList()

    init {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = true
        settings.allowContentAccess = true
        settings.builtInZoomControls = false
        settings.displayZoomControls = false
        settings.setSupportZoom(false)
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        isVerticalScrollBarEnabled = true
        isHorizontalScrollBarEnabled = false
        setBackgroundColor(0xFFF3EEE4.toInt())
        webViewClient =
            object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    pageReady = true
                    renderPending()
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest,
                ): Boolean = openExternal(request.url)

                @Deprecated("Deprecated in Java")
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    val parsed = url?.let(Uri::parse) ?: return false
                    return openExternal(parsed)
                }
            }
        loadUrl(VIEWER_URL)
    }

    fun render(markdown: String, imageBaseUri: String, toc: List<TocEntry>) {
        pendingMarkdown = markdown
        pendingImageBase = imageBaseUri
        pendingToc = toc
        if (pageReady) {
            renderPending()
        }
    }

    fun scrollToHeading(id: String) {
        if (!pageReady) return
        evaluateJavascript("window.scrollToHeading(${JSONObject.quote(id)})", null)
    }

    private fun renderPending() {
        val markdown = pendingMarkdown ?: return
        val tocJson = JSONArray()
        for (entry in pendingToc) {
            tocJson.put(
                JSONObject()
                    .put("id", entry.id)
                    .put("level", entry.level)
                    .put("text", entry.text),
            )
        }
        val script =
            "window.renderMarkdown(" +
                JSONObject.quote(markdown) +
                "," +
                JSONObject.quote(pendingImageBase) +
                "," +
                tocJson.toString() +
                ")"
        evaluateJavascript(script, null)
    }

    private fun openExternal(uri: Uri): Boolean {
        val scheme = uri.scheme?.lowercase().orEmpty()
        if (scheme != "http" && scheme != "https" && scheme != "mailto") {
            return false
        }
        return runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        }.getOrDefault(true)
    }

    companion object {
        const val VIEWER_URL = "file:///android_asset/viewer/index.html"
    }
}
