package com.fcmguard.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fcmguard.app.ui.screen.*
import com.fcmguard.app.ui.theme.FCMGuardTheme
import com.fcmguard.app.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* handled */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            val vm: MainViewModel = viewModel()
            val themeMode by vm.themeMode.collectAsState()

            FCMGuardTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "dashboard") {
                        composable("dashboard") {
                            DashboardScreen(
                                viewModel = vm,
                                onNavigateWhitelist = { navController.navigate("whitelist") },
                                onNavigateStats = { navController.navigate("stats") },
                                onNavigateSettings = { navController.navigate("settings") }
                            )
                        }
                        composable("whitelist") {
                            WhitelistScreen(
                                viewModel = vm,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("stats") {
                            StatsScreen(
                                viewModel = vm,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                viewModel = vm,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
