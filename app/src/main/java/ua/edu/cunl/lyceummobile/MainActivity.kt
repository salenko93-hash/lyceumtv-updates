package ua.edu.cunl.lyceummobile

import android.content.Intent
import android.os.Bundle
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import ua.edu.cunl.lyceummobile.ui.LyceumApp
import ua.edu.cunl.lyceummobile.ui.theme.LyceumTheme
import ua.edu.cunl.lyceummobile.ui.theme.Navy

class MainActivity : ComponentActivity() {
    private val model: LyceumViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = android.graphics.Color.rgb(12, 37, 78)
        WindowInsetsControllerCompat(window, window.decorView)
            .isAppearanceLightStatusBars = false

        applyPcConfiguration(intent)

        setContent {
            LyceumTheme {
                LyceumApp(model)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        applyPcConfiguration(intent)
    }

    private fun applyPcConfiguration(intent: Intent?) {
        if (intent == null) return

        fun decodeExtra(name: String): String? {
            val raw = intent.getStringExtra(name)?.trim().orEmpty()
            if (raw.isBlank()) return null
            return try {
                String(Base64.decode(raw, Base64.DEFAULT), Charsets.UTF_8).trim()
            } catch (_: Exception) {
                null
            }
        }

        val googleUrl = decodeExtra("pc_google_url_b64")
        val githubUrl = decodeExtra("pc_github_manifest_url_b64")
        val source = intent.getStringExtra("pc_source")?.trim()?.uppercase()

        fun isAllowedGoogleAppsScriptUrl(value: String): Boolean {
            val uri = runCatching { Uri.parse(value) }.getOrNull() ?: return false
            return uri.scheme.equals("https", ignoreCase = true) &&
                uri.host.equals("script.google.com", ignoreCase = true) &&
                uri.path.orEmpty().startsWith("/macros/s/") &&
                uri.path.orEmpty().endsWith("/exec")
        }

        fun isAllowedGithubRawManifestUrl(value: String): Boolean {
            val uri = runCatching { Uri.parse(value) }.getOrNull() ?: return false
            return uri.scheme.equals("https", ignoreCase = true) &&
                uri.host.equals("raw.githubusercontent.com", ignoreCase = true) &&
                uri.path.orEmpty().endsWith("/content_manifest.json")
        }

        var changed = false
        if (!googleUrl.isNullOrBlank() && isAllowedGoogleAppsScriptUrl(googleUrl)) {
            model.setGoogleSheetsUrl(googleUrl)
            changed = true
        }
        if (!githubUrl.isNullOrBlank() && isAllowedGithubRawManifestUrl(githubUrl)) {
            model.setGithubManifestUrl(githubUrl)
            changed = true
        }
        if (source == "GITHUB" || source == "LOCAL") {
            model.setSource(source)
            changed = true
        }

        if (changed) {
            intent.removeExtra("pc_google_url_b64")
            intent.removeExtra("pc_github_manifest_url_b64")
            intent.removeExtra("pc_source")
            Toast.makeText(
                this,
                "URL з ПК збережено. Оновлюю дані…",
                Toast.LENGTH_SHORT
            ).show()
            window.decorView.post { model.refreshEverything() }
        }
    }

    override fun onStart() {
        super.onStart()
        model.setForeground(true)
    }

    override fun onStop() {
        model.setForeground(false)
        super.onStop()
    }
}
