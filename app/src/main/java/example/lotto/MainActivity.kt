// File Path: app/src/main/java/example/lotto/MainActivity.kt
package com.example.lotto

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.lotto.ui.analysis.AnalysisScreen
import com.example.lotto.ui.analysis.AnalysisViewModel
import com.example.lotto.ui.fortune.FortuneScreen
import com.example.lotto.ui.history.HistoryScreen
import com.example.lotto.ui.qr.QRScannerScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                MainScreen()
            }
        }
    }
}

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Analysis : BottomNavItem("analysis", "분석", Icons.Default.Build)
    object Fortune : BottomNavItem("fortune", "운세", Icons.Default.Favorite)
    object QRScan : BottomNavItem("qr_scan", "QR스캔", Icons.Default.Search)
    object History : BottomNavItem("history", "내역", Icons.Default.List)
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val analysisViewModel: AnalysisViewModel = hiltViewModel()

    val navItems = listOf(
        BottomNavItem.Analysis,
        BottomNavItem.Fortune,
        BottomNavItem.QRScan,
        BottomNavItem.History
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                navItems.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = {
                            if (currentRoute != item.route) {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title
                            )
                        },
                        label = { Text(text = item.title) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Analysis.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Analysis.route) {
                AnalysisScreen(viewModel = analysisViewModel)
            }
            composable(BottomNavItem.Fortune.route) {
                FortuneScreen()
            }
            composable(BottomNavItem.QRScan.route) {
                QRScannerScreen()
            }
            composable(BottomNavItem.History.route) {
                HistoryScreen()
            }
        }
    }
}
