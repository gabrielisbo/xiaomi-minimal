package com.minimal.launcher

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.window.OnBackInvokedDispatcher
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

class MainActivity : Activity() {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK

        webView = WebView(this).apply {
            setBackgroundColor(Color.BLACK)
            overScrollMode = WebView.OVER_SCROLL_NEVER
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                cacheMode = WebSettings.LOAD_NO_CACHE
                allowFileAccess = true
                allowContentAccess = false
                @Suppress("DEPRECATION")
                allowFileAccessFromFileURLs = false
                @Suppress("DEPRECATION")
                allowUniversalAccessFromFileURLs = false
                blockNetworkLoads = true
            }

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    return true
                }
            }

            addJavascriptInterface(LauncherBridge(this@MainActivity), "AndroidBridge")
            loadUrl("file:///android_asset/index.html")
        }

        setContentView(webView)
        setupBackNavigation()
    }

    private fun setupBackNavigation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT
            ) {
                webView.evaluateJavascript("if(window.handleAndroidBack) window.handleAndroidBack();", null)
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        webView.evaluateJavascript("if(window.handleAndroidBack) window.handleAndroidBack();", null)
    }

    inner class LauncherBridge(private val context: Context) {

        @JavascriptInterface
        fun openApp(packageName: String) {
            try {
                if (packageName == "com.android.settings.USAGE_ACCESS") {
                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    return
                }

                val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                }
            } catch (e: Exception) {
            }
        }

        @JavascriptInterface
        fun getInstalledAppsJson(): String {
            val jsonArray = JSONArray()
            try {
                val pm = context.packageManager
                val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                val resolveInfos = pm.queryIntentActivities(mainIntent, 0)

                for (info in resolveInfos) {
                    val pkg = info.activityInfo.packageName
                    if (pkg == context.packageName) continue

                    val label = info.loadLabel(pm).toString()
                    val obj = JSONObject().apply {
                        put("label", label)
                        put("pkg", pkg)
                    }
                    jsonArray.put(obj)
                }
            } catch (e: Exception) {
            }
            return jsonArray.toString()
        }

        @JavascriptInterface
        fun getTodayScreenTimeMinutes(): Int {
            try {
                val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
                val mode = appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
                if (mode != AppOpsManager.MODE_ALLOWED) {
                    return 0
                }

                val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val startOfDay = calendar.timeInMillis
                val now = System.currentTimeMillis()

                val stats = usageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY,
                    startOfDay,
                    now
                )

                var totalTimeMs = 0L
                for (stat in stats) {
                    if (stat.lastTimeUsed >= startOfDay) {
                        totalTimeMs += stat.totalTimeInForeground
                    }
                }
                return (totalTimeMs / (1000 * 60)).toInt()
            } catch (e: Exception) {
                return 0
            }
        }
    }
}
