package com.thirtyeightxhx.browser

import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import java.net.URLEncoder

class MainActivity : Activity() {
    private val bg = Color.rgb(8, 13, 22)
    private val panel = Color.rgb(17, 26, 42)
    private val blue = Color.rgb(53, 184, 255)
    private val white = Color.rgb(237, 244, 255)
    private val prefs by lazy { getSharedPreferences("browser", Context.MODE_PRIVATE) }
    private val tabs = mutableListOf<Tab>()
    private var active = 0
    private var privateMode = false
    private lateinit var root: LinearLayout
    private lateinit var address: EditText
    private lateinit var webHost: FrameLayout
    private lateinit var status: TextView

    data class Tab(val web: WebView, var title: String = "Nova aba", var url: String = "")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        buildUi()
        addTab("https://www.google.com")
    }

    private fun buildUi() {
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bg); setPadding(12, 10, 12, 8) }
        val brand = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val logo = TextView(this).apply { text = "38HX"; textSize = 23f; setTextColor(blue); setTypeface(null, 1) }
        status = TextView(this).apply { text = "  BROWSER  ·  PRIVACIDADE"; textSize = 10f; setTextColor(Color.LTGRAY) }
        brand.addView(logo); brand.addView(status)
        root.addView(brand, LinearLayout.LayoutParams(-1, dp(42)))

        address = EditText(this).apply {
            hint = "Pesquisar ou digitar endereço"; setHintTextColor(Color.GRAY); setTextColor(white)
            textSize = 14f; singleLine = true; imeOptions = EditorInfo.IME_ACTION_GO
            setPadding(dp(14), 0, dp(12), 0); setBackground(roundDrawable(panel, 18))
            setOnEditorActionListener { _, action, _ -> if (action == EditorInfo.IME_ACTION_GO || action == EditorInfo.IME_ACTION_DONE) { navigate(text.toString()); true } else false }
        }
        root.addView(address, LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(10); bottomMargin = dp(8) })

        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        addAction(actions, "‹", "Voltar") { current()?.web?.goBack() }
        addAction(actions, "›", "Avançar") { current()?.web?.goForward() }
        addAction(actions, "↻", "Atualizar") { current()?.web?.reload() }
        addAction(actions, "+", "Nova aba") { addTab("https://www.google.com") }
        addAction(actions, "▣", "Abas") { showTabs() }
        addAction(actions, "⋮", "Menu") { showMenu() }
        root.addView(actions, LinearLayout.LayoutParams(-1, dp(46)))

        webHost = FrameLayout(this).apply { setBackgroundColor(Color.WHITE) }
        root.addView(webHost, LinearLayout.LayoutParams(-1, 0, 1f).apply { topMargin = dp(8); bottomMargin = dp(8) })

        val bottom = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; setBackground(roundDrawable(panel, 16)); setPadding(4, 4, 4, 4) }
        addAction(bottom, "⌂  Início", "Início") { navigate("https://www.google.com") }
        addAction(bottom, "☆  Salvar", "Favorito") { saveBookmark() }
        addAction(bottom, "◉  VPN", "VPN") { showVpnInfo() }
        addAction(bottom, "◌  Privado", "Modo privado") { togglePrivate() }
        root.addView(bottom, LinearLayout.LayoutParams(-1, dp(48)))
        setContentView(root)
    }

    private fun addAction(row: LinearLayout, label: String, description: String, action: () -> Unit) {
        val b = TextView(this).apply {
            text = label; contentDescription = description; textSize = 13f; gravity = Gravity.CENTER
            setTextColor(if (description == "VPN") blue else white); setPadding(dp(6), dp(8), dp(6), dp(8))
            setOnClickListener { action() }
        }
        row.addView(b, LinearLayout.LayoutParams(0, -1, 1f))
    }

    private fun addTab(url: String) {
        val w = WebView(this)
        w.setBackgroundColor(Color.WHITE)
        w.settings.javaScriptEnabled = true
        w.settings.domStorageEnabled = true
        w.settings.loadsImagesAutomatically = true
        w.settings.javaScriptCanOpenWindowsAutomatically = false
        w.settings.setSupportMultipleWindows(false)
        w.settings.allowFileAccess = false
        w.settings.allowContentAccess = false
        w.settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
        w.webChromeClient = object : WebChromeClient() {
            override fun onReceivedTitle(view: WebView?, title: String?) {
                tabs.firstOrNull { it.web == view }?.title = title ?: "Página"
                if (view == current()?.web) refreshAddress()
            }
        }
        w.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val uri = request?.url ?: return false
                if (uri.scheme == "http" || uri.scheme == "https") return false
                return true
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                val tab = tabs.firstOrNull { it.web == view } ?: return
                tab.url = url ?: ""
                if (!privateMode && url != null && url.startsWith("http")) {
                    val old = prefs.getString("history", "") ?: ""
                    prefs.edit().putString("history", ("$url\n$old").lines().distinct().take(200).joinToString("\n")).apply()
                }
                if (view == current()?.web) refreshAddress()
            }
        }
        w.setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            try {
                val req = DownloadManager.Request(Uri.parse(url))
                    .setMimeType(mimeType)
                    .addRequestHeader("User-Agent", userAgent)
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, android.webkit.URLUtil.guessFileName(url, contentDisposition, mimeType))
                (getSystemService(DOWNLOAD_SERVICE) as DownloadManager).enqueue(req)
                toast("Download iniciado")
            } catch (_: Exception) { toast("Não foi possível iniciar o download") }
        })
        tabs.add(Tab(w)); active = tabs.lastIndex
        webHost.addView(w, FrameLayout.LayoutParams(-1, -1)); showActiveTab(); w.loadUrl(url)
    }

    private fun current(): Tab? = tabs.getOrNull(active)
    private fun showActiveTab() {
        webHost.removeAllViews()
        current()?.let { webHost.addView(it.web, FrameLayout.LayoutParams(-1, -1)); address.setText(it.web.url ?: it.url) }
        status.text = if (privateMode) "  MODO PRIVADO ATIVO" else "  BROWSER  ·  ${tabs.size} ABA(S)"
    }
    private fun refreshAddress() { current()?.let { address.setText(it.web.url ?: it.url) } }

    private fun navigate(raw: String) {
        val value = raw.trim(); if (value.isEmpty()) return
        val url = when {
            value.matches(Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://.*")) -> value
            value.contains(" ") || !value.contains(".") -> "https://www.google.com/search?q=" + URLEncoder.encode(value, "UTF-8")
            else -> "https://$value"
        }
        current()?.web?.loadUrl(url)
    }

    private fun showTabs() {
        if (tabs.isEmpty()) return
        val labels = tabs.mapIndexed { i, t -> "${if (i == active) "● " else ""}${t.title}\n${t.url}" }.toTypedArray()
        AlertDialog.Builder(this).setTitle("Abas abertas (${tabs.size})")
            .setItems(labels) { _, which -> active = which; showActiveTab() }
            .setPositiveButton("Nova aba") { _, _ -> addTab("https://www.google.com") }
            .setNegativeButton("Fechar aba atual") { _, _ -> closeCurrentTab() }.show()
    }

    private fun closeCurrentTab() {
        if (tabs.size <= 1) { current()?.web?.loadUrl("https://www.google.com"); return }
        tabs.removeAt(active).web.destroy(); active = (active - 1).coerceAtLeast(0); showActiveTab()
    }

    private fun showMenu() {
        val options = arrayOf("Favoritos", "Histórico", "Limpar histórico", "Downloads", "Segurança e privacidade", "Informações da VPN")
        AlertDialog.Builder(this).setTitle("38HX Browser").setItems(options) { _, which ->
            when (which) {
                0 -> showSavedList("bookmarks", "Favoritos") { navigate(it) }
                1 -> showSavedList("history", "Histórico") { navigate(it) }
                2 -> { prefs.edit().remove("history").apply(); toast("Histórico local apagado") }
                3 -> try { startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)) } catch (_: Exception) { toast("Abra a pasta Downloads do aparelho") }
                4 -> showPrivacyInfo()
                5 -> showVpnInfo()
            }
        }.show()
    }

    private fun saveBookmark() {
        val url = current()?.web?.url ?: return
        val old = prefs.getString("bookmarks", "") ?: ""
        prefs.edit().putString("bookmarks", ("$url\n$old").lines().filter { it.isNotBlank() }.distinct().joinToString("\n")).apply()
        toast("Favorito salvo")
    }

    private fun showSavedList(key: String, title: String, onPick: (String) -> Unit) {
        val items = (prefs.getString(key, "") ?: "").lines().filter { it.isNotBlank() }.distinct()
        if (items.isEmpty()) { toast("Nada salvo ainda"); return }
        AlertDialog.Builder(this).setTitle(title).setItems(items.toTypedArray()) { _, i -> onPick(items[i]) }
            .setNeutralButton("Limpar") { _, _ -> prefs.edit().remove(key).apply(); toast("Lista apagada") }.show()
    }

    private fun togglePrivate() {
        privateMode = !privateMode
        if (privateMode) {
            CookieManager.getInstance().removeAllCookies(null); CookieManager.getInstance().flush()
            tabs.forEach { it.web.clearHistory(); it.web.clearCache(true) }
            status.text = "  MODO PRIVADO ATIVO"
            toast("Modo privado ligado. Feche as abas privadas ao terminar.")
        } else { status.text = "  BROWSER  ·  ${tabs.size} ABA(S)"; toast("Modo privado desligado") }
    }

    private fun showVpnInfo() {
        AlertDialog.Builder(this).setTitle("Proton VPN · 38HX")
            .setMessage("Esta versão usa o aplicativo oficial Proton VPN para estabelecer a conexão real. O 38HX não controla nem finge o estado da conexão. Depois de conectar no Proton VPN, volte ao navegador e confirme o ícone de chave/VPN do Android. A proteção depende da conexão estar ativa no sistema.")
            .setPositiveButton("Abrir Proton VPN") { _, _ -> openProtonVpn() }
            .setNeutralButton("Baixar app oficial") { _, _ ->
                openUrlExternal("https://protonvpn.com/download-android")
            }
            .setNegativeButton("Cancelar", null).show()
    }

    private fun openProtonVpn() {
        val launch = packageManager.getLaunchIntentForPackage("ch.protonvpn.android")
        if (launch != null) {
            try { startActivity(launch) }
            catch (_: Exception) { toast("Não foi possível abrir o Proton VPN") }
        } else {
            AlertDialog.Builder(this).setTitle("Proton VPN não encontrado")
                .setMessage("Instale o aplicativo oficial Proton VPN. Depois, use este botão para abri-lo e conectar-se.")
                .setPositiveButton("Abrir página oficial") { _, _ -> openUrlExternal("https://protonvpn.com/download-android") }
                .setNegativeButton("Cancelar", null).show()
        }
    }

    private fun openUrlExternal(url: String) {
        try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        catch (_: Exception) { toast("Não foi possível abrir o link") }
    }

    private fun showPrivacyInfo() {
        AlertDialog.Builder(this).setTitle("Segurança e privacidade")
            .setMessage("• O modo privado deste protótipo evita gravar novas URLs no histórico local, mas não garante anonimato.\n\n• Cookies e cache podem ser apagados ao ativá-lo; para privacidade mais forte, use abas privadas isoladas e apague seus dados ao fechá-las.\n\n• Uma VPN não torna você anônimo e o provedor VPN pode ver metadados de conexão.\n\n• Evite inserir dados sensíveis em sites que não conhece.")
            .setPositiveButton("Entendi", null).show()
    }

    private fun roundDrawable(color: Int, radiusDp: Int): android.graphics.drawable.GradientDrawable = android.graphics.drawable.GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radiusDp).toFloat(); setStroke(dp(1), Color.rgb(35, 55, 78))
    }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

    override fun onBackPressed() {
        val w = current()?.web
        if (w?.canGoBack() == true) w.goBack() else if (tabs.size > 1) closeCurrentTab() else super.onBackPressed()
    }
    override fun onDestroy() {
        tabs.forEach { it.web.stopLoading(); it.web.destroy() }
        tabs.clear(); super.onDestroy()
    }
}
