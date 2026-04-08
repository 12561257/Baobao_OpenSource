package com.baobao.fatloss

import android.os.Bundle
import com.baobao.fatloss.R
import android.content.Context
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.baobao.fatloss.navigation.BottomNavBar
import com.baobao.fatloss.navigation.NavGraph
import com.baobao.fatloss.navigation.Screen
import com.baobao.fatloss.ui.theme.FitAssistantTheme
import com.baobao.fatloss.ui.theme.FusionBackground
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val lang = com.baobao.fatloss.data.local.LanguageStore.getLanguageSync(newBase)
        val wrapped = if (lang != com.baobao.fatloss.data.local.LanguageStore.LANG_SYSTEM) {
            val locale = when (lang) {
                com.baobao.fatloss.data.local.LanguageStore.LANG_ZH -> java.util.Locale.SIMPLIFIED_CHINESE
                com.baobao.fatloss.data.local.LanguageStore.LANG_EN -> java.util.Locale.ENGLISH
                else -> java.util.Locale.getDefault()
            }
            val config = android.content.res.Configuration(newBase.resources.configuration)
            config.setLocale(locale)
            config.setLocales(LocaleList(locale))
            newBase.createConfigurationContext(config)
        } else {
            newBase
        }
        super.attachBaseContext(wrapped)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val crashLog = CrashHandler.getLastCrash(this)

        // 启动时检查 API Key 是否已配置
        val app = applicationContext as FitAssistantApp
        val isConfigured = runBlocking { app.container.apiKeyStore.isConfigured() }
        val startDest = if (isConfigured) Screen.Home.route else Screen.ApiKeySetup.route

        setContent {
            FitAssistantTheme {
                val navController = rememberNavController()
                val currentBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = currentBackStackEntry?.destination?.route

                // 崩溃日志弹窗
                var showCrashDialog by remember { mutableStateOf(crashLog != null) }
                if (showCrashDialog && crashLog != null) {
                    AlertDialog(
                        onDismissRequest = {
                            showCrashDialog = false
                            CrashHandler.clearCrash(this@MainActivity)
                        },
                        title = { Text(getString(R.string.main_crash_title)) },
                        text = {
                            SelectionContainer {
                                Text(
                                    text = crashLog,
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 400.dp)
                                        .verticalScroll(rememberScrollState())
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("crash_log", crashLog))
                                android.widget.Toast.makeText(this@MainActivity, getString(R.string.main_copy_clipboard), android.widget.Toast.LENGTH_SHORT).show()
                            }) { Text(getString(R.string.main_copy_log)) }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                showCrashDialog = false
                                CrashHandler.clearCrash(this@MainActivity)
                            }) { Text(getString(R.string.main_close)) }
                        }
                    )
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = FusionBackground,
                    bottomBar = {
                        val showBottomBar = currentRoute in listOf(
                            Screen.Home.route,
                            Screen.MealLog.route,
                            Screen.Progress.route,
                            Screen.Profile.route
                        )
                        if (showBottomBar) {
                            BottomNavBar(navController, currentRoute)
                        }
                    }
                ) { innerPadding ->
                    NavGraph(navController, paddingValues = innerPadding, startDestination = startDest)
                }
            }
        }
    }
}
