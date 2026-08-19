package com.kimro.ai.lotto

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kimro.ai.lotto.ui.analysis.AnalysisScreen
import com.kimro.ai.lotto.ui.analysis.AnalysisViewModel
import com.kimro.ai.lotto.ui.fortune.FortuneScreen
import com.kimro.ai.lotto.ui.history.HistoryScreen
import com.kimro.ai.lotto.ui.history.HistoryViewModel
import com.kimro.ai.lotto.ui.qr.QrScanScreen
import dagger.hilt.android.AndroidEntryPoint

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Analysis : Screen("analysis", "분석", Icons.Default.Settings)
    object Fortune : Screen("fortune", "운세", Icons.Default.DateRange)
    object QrScan : Screen("qr_scan", "QR당첨확인", Icons.Default.Search)
    object History : Screen("history", "내역", Icons.Default.List)
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val analysisViewModel: AnalysisViewModel by viewModels()
    private val historyViewModel: HistoryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                var currentScreen by remember { mutableStateOf<Screen>(Screen.Analysis) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        val items = listOf(
                            Screen.Analysis,
                            Screen.Fortune,
                            Screen.QrScan,
                            Screen.History
                        )

                        // 표준 NavigationBar는 내부 여백을 줄일 수 없어서, 직접 만든 Row로 교체했다.
                        // .windowInsetsPadding(WindowInsets.navigationBars)로 기기의 제스처바 영역만큼
                        // 자동으로 띄우고, 혹시 몰라 최소 8dp 여백도 추가로 깔아 안전하게 만든다.
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF1F5F9))
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .padding(top = 2.dp, bottom = 3.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            items.forEach { screen ->
                                val selected = currentScreen == screen
                                val tint = if (selected) Color(0xFF0EA5E9) else Color(0xFF64748B)

                                Column(
                                    modifier = Modifier
                                        .clickable { currentScreen = screen }
                                        .padding(horizontal = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        screen.icon,
                                        contentDescription = screen.title,
                                        tint = tint,
                                        modifier = Modifier.size(23.dp)
                                    )
                                    Spacer(modifier = Modifier.height(1.dp))
                                    Text(
                                        text = screen.title,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        color = tint
                                    )
                                }
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
                            is Screen.Fortune -> FortuneScreen()
                            is Screen.QrScan -> QrScanScreen()
                            is Screen.History -> HistoryScreen(viewModel = historyViewModel)
                        }
                    }
                }
            }
        }
    }
}
