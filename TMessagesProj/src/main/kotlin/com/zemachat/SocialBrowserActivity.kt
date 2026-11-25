package com.zemachat

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject

class SocialBrowserActivity : Activity() {

    companion object {
        const val EXTRA_URL = "extra_url"
        private const val PREFS_NAME = "zema_social_prefs"
        private const val KEY_SHORTCUTS = "shortcuts_json"
        private const val DESKTOP_UA =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"

        fun open(context: Context, url: String? = null) {
            val intent = Intent(context, SocialBrowserActivity::class.java)
            if (!url.isNullOrBlank()) {
                intent.putExtra(EXTRA_URL, url)
            }
            if (context !is Activity) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    data class Shortcut(val name: String, val url: String)

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var chipsContainer: LinearLayout
    private lateinit var desktopBtn: TextView
    private var defaultUa: String = ""
    private var desktopMode = false
    private val shortcuts = mutableListOf<Shortcut>()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loadShortcuts()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#121212"))
        }

        // Top toolbar
        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.parseColor("#121212"))
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }

        val backBtn = makeIconBtn("←") { onBackPressed() }
        val titleView = TextView(this).apply {
            text = "Zema Social"
            setTextColor(Color.parseColor("#D4AF37"))
            textSize = 17f
            setPadding(dp(8), 0, dp(8), 0)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        desktopBtn = makeIconBtn("🖥") { toggleDesktopMode() }
        val openExtBtn = makeIconBtn("↗") { openInExternalBrowser() }
        val addBtn = makeIconBtn("＋") { showAddShortcutDialog() }
        val wmBtn = makeIconBtn("⚙") { WatermarkSettingsActivity.open(this) }

        toolbar.addView(backBtn)
        toolbar.addView(titleView)
        toolbar.addView(desktopBtn)
        toolbar.addView(openExtBtn)
        toolbar.addView(addBtn)
        toolbar.addView(wmBtn)
        root.addView(toolbar)

        // Shortcuts bar
        chipsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(6), dp(4), dp(6), dp(6))
        }
        val scroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            setBackgroundColor(Color.parseColor("#1A1A1A"))
            addView(chipsContainer)
        }
        root.addView(scroll)

        // Progress bar
        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(3))
            visibility = View.GONE
        }
        root.addView(progressBar)

        // WebView
        webView = object : WebView(this) {
            override fun onWindowVisibilityChanged(visibility: Int) {
                super.onWindowVisibilityChanged(View.VISIBLE)
            }
        }.apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
        }
        setupWebView()
        root.addView(webView)

        setContentView(root)
        renderShortcuts()

        val initialUrl = intent.getStringExtra(EXTRA_URL)
            ?: shortcuts.firstOrNull()?.url
            ?: "https://www.google.com"
        loadUrlSmart(initialUrl)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val s = webView.settings
        s.javaScriptEnabled = true
        s.domStorageEnabled = true
        s.databaseEnabled = true
        s.mediaPlaybackRequiresUserGesture = false
        s.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        s.useWideViewPort = true
        s.loadWithOverviewMode = true
        s.setSupportZoom(true)
        s.builtInZoomControls = true
        s.displayZoomControls = false
        defaultUa = s.userAgentString

        val cm = CookieManager.getInstance()
        cm.setAcceptCookie(true)
        cm.setAcceptThirdPartyCookies(webView, true)

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val scheme = request.url.scheme?.lowercase() ?: ""
                return !(scheme == "http" || scheme == "https")
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                view?.evaluateJavascript("(function(){try{Object.defineProperty(document,'hidden',{get:function(){return false;},configurable:true});Object.defineProperty(document,'visibilityState',{get:function(){return 'visible';},configurable:true});document.addEventListener('visibilitychange',function(e){e.stopImmediatePropagation();},true);}catch(e){}})();", null)
                CookieManager.getInstance().flush()
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progressBar.visibility = if (newProgress < 100) View.VISIBLE else View.GONE
                progressBar.progress = newProgress
            }
        }
    }

    private fun loadUrlSmart(url: String) {
        if (url.contains("tiktok.com", ignoreCase = true)) {
            for (pkg in listOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill")) {
                try {
                    if (packageManager.getLaunchIntentForPackage(pkg) != null) {
                        val i = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply { setPackage(pkg) }
                        startActivity(i)
                        return
                    }
                } catch (_: Exception) {}
            }
        }
        val needsDesktop = url.contains("web.whatsapp.com", ignoreCase = true)
        if (needsDesktop && !desktopMode) {
            setDesktopMode(true, reload = false)
        }
        webView.loadUrl(url)
    }

    private fun toggleDesktopMode() {
        setDesktopMode(!desktopMode, reload = true)
    }

    private fun setDesktopMode(enabled: Boolean, reload: Boolean) {
        desktopMode = enabled
        webView.settings.userAgentString = if (enabled) DESKTOP_UA else defaultUa
        desktopBtn.setTextColor(if (enabled) Color.parseColor("#D4AF37") else Color.WHITE)
        Toast.makeText(
            this,
            if (enabled) "Desktop mode ON" else "Mobile mode ON",
            Toast.LENGTH_SHORT
        ).show()
        if (reload) webView.reload()
    }

    private fun openInExternalBrowser() {
        val currentUrl = webView.url ?: return
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl)))
        } catch (_: Exception) {
            Toast.makeText(this, "Cannot open browser", Toast.LENGTH_SHORT).show()
        }
    }

    private fun renderShortcuts() {
        chipsContainer.removeAllViews()
        shortcuts.forEachIndexed { index, item ->
            val chip = TextView(this).apply {
                text = item.name
                setTextColor(Color.WHITE)
                textSize = 13f
                setBackgroundColor(Color.parseColor("#2A2A2A"))
                setPadding(dp(12), dp(6), dp(12), dp(6))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(dp(4), 0, dp(4), 0) }
                setOnClickListener { loadUrlSmart(item.url) }
                setOnLongClickListener {
                    showManageShortcutDialog(index)
                    true
                }
            }
            chipsContainer.addView(chip)
        }
    }

    private fun showAddShortcutDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(8))
        }
        val nameInput = EditText(this).apply { hint = "Name (e.g. News)" }
        val urlInput = EditText(this).apply { hint = "URL (e.g. https://example.com)" }
        layout.addView(nameInput)
        layout.addView(urlInput)

        AlertDialog.Builder(this)
            .setTitle("Add Shortcut")
            .setView(layout)
            .setPositiveButton("Add") { _, _ ->
                val name = nameInput.text.toString().trim()
                var url = urlInput.text.toString().trim()
                if (name.isNotEmpty() && url.isNotEmpty()) {
                    if (!url.startsWith("http://") && !url.startsWith("https://")) {
                        url = "https://$url"
                    }
                    shortcuts.add(Shortcut(name, url))
                    saveShortcuts()
                    renderShortcuts()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showManageShortcutDialog(index: Int) {
        val item = shortcuts[index]
        val options = arrayOf("Move Left ◀", "Move Right ▶", "Delete 🗑")
        AlertDialog.Builder(this)
            .setTitle(item.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> if (index > 0) {
                        shortcuts.removeAt(index)
                        shortcuts.add(index - 1, item)
                        saveShortcuts()
                        renderShortcuts()
                    }
                    1 -> if (index < shortcuts.size - 1) {
                        shortcuts.removeAt(index)
                        shortcuts.add(index + 1, item)
                        saveShortcuts()
                        renderShortcuts()
                    }
                    2 -> {
                        shortcuts.removeAt(index)
                        saveShortcuts()
                        renderShortcuts()
                    }
                }
            }
            .show()
    }

    private fun loadShortcuts() {
        shortcuts.clear()
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val raw = prefs.getString(KEY_SHORTCUTS, null)
        if (raw != null) {
            try {
                val arr = JSONArray(raw)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    shortcuts.add(Shortcut(obj.getString("name"), obj.getString("url")))
                }
                if (shortcuts.isNotEmpty()) return
            } catch (_: Exception) {
            }
        }
        shortcuts.addAll(
            listOf(
                Shortcut("TikTok", "https://www.tiktok.com"),
                Shortcut("Facebook", "https://m.facebook.com"),
                Shortcut("Instagram", "https://www.instagram.com"),
                Shortcut("YouTube", "https://m.youtube.com"),
                Shortcut("X", "https://x.com"),
                Shortcut("LinkedIn", "https://www.linkedin.com"),
                Shortcut("Pinterest", "https://www.pinterest.com"),
                Shortcut("Reddit", "https://www.reddit.com"),
                Shortcut("Snapchat", "https://web.snapchat.com"),
                Shortcut("WhatsApp", "https://web.whatsapp.com"),
                Shortcut("Google", "https://www.google.com")
            )
        )
    }

    private fun saveShortcuts() {
        val arr = JSONArray()
        shortcuts.forEach {
            arr.put(JSONObject().put("name", it.name).put("url", it.url))
        }
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putString(KEY_SHORTCUTS, arr.toString())
            .apply()
    }

    private fun makeIconBtn(label: String, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            text = label
            setTextColor(Color.WHITE)
            textSize = 18f
            setPadding(dp(10), dp(4), dp(10), dp(4))
            setOnClickListener { onClick() }
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onPause() {
        super.onPause()
        CookieManager.getInstance().flush()
    }
}
