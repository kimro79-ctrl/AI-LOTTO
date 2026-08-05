// File Path: app/src/main/java/com/example/lotto/MainActivity.kt
package com.example.lotto

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.lotto.ui.analysis.AnalysisScreen
import com.example.lotto.ui.analysis.AnalysisViewModel
import com.example.lotto.ui.history.HistoryScreen
import com.example.lotto.ui.history.HistoryViewModel
import com.example.lotto.ui.qr.QrScanScreen
import com.example.lotto.ui.theme.LottoTheme

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Analysis : Screen("analysis", "분석", Icons.Default.Settings)
    object Fortune : Screen("fortune", "운세", Icons.Default.DateRange)
    object QrScan : Screen("qr_scan", "QR스캔", Icons.Default.Search)
    object History : Screen("history", "내역", Icons.Default.List)
}

class MainActivity : ComponentActivity() {

    private val analysisViewModel: AnalysisViewModel by viewModels()
    private val historyViewModel: HistoryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LottoTheme {
                var currentScreen by remember { mutableStateOf<Screen>(Screen.Analysis) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar(
                            containerColor = Color(0xFFF1F5F9),
                            contentColor = Color(0xFF64748B)
                        ) {
                            val items = listOf(
                                Screen.Analysis,
                                Screen.Fortune,
                                Screen.QrScan,
                                Screen.History
                            )

                            items.forEach { screen ->
                                NavigationBarItem(
                                    icon = { Icon(screen.icon, contentDescription = screen.title) },
                                    label = { Text(screen.title) },
                                    selected = currentScreen == screen,
                                    onClick = { currentScreen = screen },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color(0xFF0EA5E9),
                                        selectedTextColor = Color(0xFF0EA5E9),
                                        unselectedIconColor = Color(0xFF64748B),
                                        unselectedTextColor = Color(0xFF64748B),
                                        indicatorColor = Color(0xFFE0F2FE)
                                    )
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        when (currentScreen) {
                            is Screen.Analysis -> AnalysisScreen(viewModel = analysisViewModel)
                            is Screen.Fortune -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("운세 화면 준비중", color = Color(0xFF64748B))
                                }
                            }
                            is Screen.QrScan -> QrScanScreen()
                            is Screen.History -> HistoryScreen(viewModel = historyViewModel)
                        }
                    }
                }
            }
        }
    }
}
