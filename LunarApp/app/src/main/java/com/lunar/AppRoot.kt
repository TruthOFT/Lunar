package com.lunar

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.lunar.data.AuthSession
import com.lunar.data.BaziResponse
import com.lunar.data.ChartRecordItem
import com.lunar.data.SessionStore
import com.lunar.navigation.AppDestinations
import com.lunar.ui.components.BottomNavBar
import com.lunar.ui.screens.AiAnalysisScreen
import com.lunar.ui.screens.ChartRoute
import com.lunar.ui.screens.CourseScreen
import com.lunar.ui.screens.LoginScreen
import com.lunar.ui.screens.RecordScreen

@Composable
fun LunarAppApp() {
    val context = LocalContext.current
    val sessionStore = remember { SessionStore(context) }
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.CHART) }
    var pendingDestination by rememberSaveable { mutableStateOf<AppDestinations?>(null) }
    var session by remember { mutableStateOf<AuthSession?>(sessionStore.load()) }
    var showLogin by rememberSaveable { mutableStateOf(false) }
    var chartResult by remember { mutableStateOf<BaziResponse?>(null) }
    var aiRecord by remember { mutableStateOf<ChartRecordItem?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            BottomNavBar(
                currentDestination = currentDestination,
                onDestinationSelected = { destination ->
                    aiRecord = null
                    if (destination == AppDestinations.CHART) {
                        showLogin = false
                        currentDestination = destination
                    } else if (session == null) {
                        pendingDestination = destination
                        showLogin = true
                        currentDestination = destination
                    } else {
                        showLogin = false
                        currentDestination = destination
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
                    currentDestination = AppDestinations.CHART
                },
                onAiAnalysis = { record ->
                    aiRecord = record
                    showLogin = false
                },
                modifier = Modifier.padding(innerPadding)
            )

            AppDestinations.COURSE -> CourseScreen(
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
