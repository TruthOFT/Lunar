package com.lunar

import android.content.Context
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
import com.lunar.data.BaziResponse
import com.lunar.navigation.AppDestinations
import com.lunar.ui.components.BottomNavBar
import com.lunar.ui.screens.ChartRoute
import com.lunar.ui.screens.CourseScreen
import com.lunar.ui.screens.LoginScreen
import com.lunar.ui.screens.RecordScreen

@Composable
fun LunarAppApp() {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("lunar_login", Context.MODE_PRIVATE)
    }
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.CHART) }
    var isLogin by rememberSaveable { mutableStateOf(prefs.getInt("isLogin", 0)) }
    var showLogin by rememberSaveable { mutableStateOf(false) }
    var chartResult by remember { mutableStateOf<BaziResponse?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            BottomNavBar(
                currentDestination = currentDestination,
                onDestinationSelected = { destination ->
                    if (destination == AppDestinations.CHART) {
                        showLogin = false
                        currentDestination = destination
                    } else if (isLogin == 0) {
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
        if (showLogin) {
            LoginScreen(
                onLoginSuccess = {
                    prefs.edit().putInt("isLogin", 1).apply()
                    isLogin = 1
                    showLogin = false
                    currentDestination = AppDestinations.CHART
                    chartResult = null
                },
                modifier = Modifier.padding(innerPadding)
            )
        } else when (currentDestination) {
            AppDestinations.CHART -> ChartRoute(
                result = chartResult,
                onResult = { chartResult = it },
                onReset = { chartResult = null },
                modifier = Modifier.padding(innerPadding)
            )

            AppDestinations.RECORD -> RecordScreen(
                modifier = Modifier.padding(innerPadding)
            )

            AppDestinations.COURSE -> CourseScreen(
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
