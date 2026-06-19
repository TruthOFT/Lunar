package com.lunar

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.lunar.data.AppVersionInfo
import com.lunar.data.AuthSession
import com.lunar.data.ChartRecordItem
import com.lunar.data.ChartResult
import com.lunar.data.SessionStore
import com.lunar.data.fetchLatestVersion
import com.lunar.navigation.AppDestinations
import com.lunar.ui.components.BottomNavBar
import com.lunar.ui.screens.AiAnalysisScreen
import com.lunar.ui.screens.ChartRoute
import com.lunar.ui.screens.LoginScreen
import com.lunar.ui.screens.MineScreen
import com.lunar.ui.screens.RecordScreen

@Composable
fun LunarAppApp() {
    val context = LocalContext.current
    val sessionStore = remember { SessionStore(context) }
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.CHART) }
    val backStack = remember { mutableStateListOf<AppDestinations>() }
    var pendingDestination by rememberSaveable { mutableStateOf<AppDestinations?>(null) }
    var session by remember { mutableStateOf<AuthSession?>(sessionStore.load()) }
    var showLogin by rememberSaveable { mutableStateOf(false) }
    var chartResult by remember { mutableStateOf<ChartResult?>(null) }
    var aiRecord by remember { mutableStateOf<ChartRecordItem?>(null) }
    var updateInfo by remember { mutableStateOf<AppVersionInfo?>(null) }

    // 切换页面时记录来路，供系统返回键回上一页
    fun navigateTo(destination: AppDestinations) {
        if (destination != currentDestination) {
            backStack.add(currentDestination)
            currentDestination = destination
        }
    }

    // 系统返回键：AI 分析/登录浮层先关掉回到原页面，其次回上一个 tab，
    // 排盘结果页没有来路时回排盘输入页，都没有则交给系统（退出）
    BackHandler(
        enabled = aiRecord != null || showLogin || backStack.isNotEmpty() || chartResult != null
    ) {
        when {
            aiRecord != null -> aiRecord = null
            showLogin -> {
                showLogin = false
                pendingDestination = null
            }
            backStack.isNotEmpty() -> currentDestination = backStack.removeAt(backStack.size - 1)
            chartResult != null -> chartResult = null
        }
    }

    LaunchedEffect(Unit) {
        val latest = fetchLatestVersion() ?: return@LaunchedEffect
        if (latest.versionCode > BuildConfig.VERSION_CODE) {
            updateInfo = latest
        }
    }

    updateInfo?.let { info ->
        AlertDialog(
            onDismissRequest = { if (!info.forceUpdate) updateInfo = null },
            title = { Text("发现新版本 ${info.versionName}") },
            text = { Text(info.changelog.ifBlank { "有新版本可用，请更新。" }) },
            confirmButton = {
                TextButton(onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(info.downloadUrl)))
                }) { Text("立即更新") }
            },
            dismissButton = if (info.forceUpdate) null else {
                { TextButton(onClick = { updateInfo = null }) { Text("稍后再说") } }
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            BottomNavBar(
                currentDestination = currentDestination,
                onDestinationSelected = { destination ->
                    aiRecord = null
                    if (destination == AppDestinations.CHART) {
                        showLogin = false
                        // 底栏点「排盘」一律回排盘输入页
                        chartResult = null
                        navigateTo(destination)
                    } else if (destination == AppDestinations.MINE) {
                        showLogin = false
                        navigateTo(destination)
                    } else if (session == null) {
                        pendingDestination = destination
                        showLogin = true
                        navigateTo(destination)
                    } else {
                        showLogin = false
                        navigateTo(destination)
                    }
                }
            )
        }
    ) { innerPadding ->
        val activeAiRecord = aiRecord
        if (activeAiRecord != null) {
            AiAnalysisScreen(
                record = activeAiRecord,
                authSession = session,
                onBack = { aiRecord = null },
                modifier = Modifier.padding(innerPadding)
            )
        } else if (showLogin) {
            LoginScreen(
                onLoginSuccess = { authSession ->
                    sessionStore.save(authSession)
                    session = authSession
                    showLogin = false
                    currentDestination = pendingDestination ?: AppDestinations.CHART
                    pendingDestination = null
                    chartResult = null
                },
                modifier = Modifier.padding(innerPadding)
            )
        } else when (currentDestination) {
            AppDestinations.CHART -> ChartRoute(
                result = chartResult,
                authSession = session,
                onResult = { chartResult = it },
                onReset = { chartResult = null },
                onRequireLogin = {
                    pendingDestination = AppDestinations.CHART
                    showLogin = true
                },
                modifier = Modifier.padding(innerPadding)
            )

            AppDestinations.RECORD -> RecordScreen(
                authSession = session,
                onRequireLogin = {
                    pendingDestination = AppDestinations.RECORD
                    showLogin = true
                },
                onOpenRecord = { result ->
                    chartResult = result
                    showLogin = false
                    navigateTo(AppDestinations.CHART)
                },
                onAiAnalysis = { record ->
                    aiRecord = record
                    showLogin = false
                },
                modifier = Modifier.padding(innerPadding)
            )

            AppDestinations.MINE -> MineScreen(
                authSession = session,
                onRequireLogin = {
                    pendingDestination = AppDestinations.MINE
                    showLogin = true
                    currentDestination = AppDestinations.MINE
                },
                onLoginInvalid = {
                    sessionStore.clear()
                    session = null
                    pendingDestination = AppDestinations.MINE
                    showLogin = true
                    currentDestination = AppDestinations.MINE
                },
                onLogout = {
                    sessionStore.clear()
                    session = null
                    showLogin = false
                    currentDestination = AppDestinations.MINE
                    chartResult = null
                    aiRecord = null
                },
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
