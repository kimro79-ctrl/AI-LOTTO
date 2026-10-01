package com.kimro.ai.lotto

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Info
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
import com.kimro.ai.lotto.notifications.FridayAlarmReceiver
import com.kimro.ai.lotto.notifications.NotificationScheduler
import com.kimro.ai.lotto.ui.analysis.AnalysisScreen
import com.kimro.ai.lotto.ui.analysis.AnalysisViewModel
import com.kimro.ai.lotto.ui.fortune.FortuneScreen
import com.kimro.ai.lotto.ui.history.HistoryScreen
import com.kimro.ai.lotto.ui.history.HistoryViewModel
import com.kimro.ai.lotto.ui.qr.QrScanScreen
import com.kimro.ai.lotto.ui.trend.TrendScreen
import dagger.hilt.android.AndroidEntryPoint

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Analysis : Screen("analysis", "분석", Icons.Default.Settings)
    object Fortune : Screen("fortune", "운세", Icons.Default.DateRange)
    object QrScan : Screen("qr_scan", "QR당첨확인", Icons.Default.Search)
    object History : Screen("history", "내역", Icons.Default.List)
    object Trend : Screen("trend", "최신경향", Icons.Default.Info)
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val analysisViewModel: AnalysisViewModel by viewModels()
    private val historyViewModel: HistoryViewModel by viewModels()

    // setContent 바깥(액티비티 레벨)에 둬서, onNewIntent에서도 같은 상태를 바로 바꿀 수 있게 했다.
    // (onCreate 한 번만 타는 지역 변수로 두면, 알림 탭으로 앱이 이미 떠있는 상태에서 다시 열릴 때
    // onNewIntent만 호출되고 onCreate는 다시 안 타서 화면 전환이 반영이 안 된다.)
    private var currentScreen by mutableStateOf<Screen>(Screen.Analysis)

    // Android 13(API 33) 이상에서 알림 표시 권한을 요청하기 위한 런처.
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* 거부해도 앱 기능 자체는 그대로 쓸 수 있어서 별도 처리 없음 */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestNotificationPermissionIfNeeded()
        NotificationScheduler.scheduleNextFriday(this)
        applyDeepLinkIfPresent(intent)

        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        val items = listOf(
                            Screen.Analysis,
                            Screen.Fortune,
                            Screen.QrScan,
                            Screen.History,
                            Screen.Trend
                        )

                        // 표준 NavigationBar는 내부 여백을 줄일 수 없어서, 직접 만든 Row로 교체했다.
                        // .windowInsetsPadding(WindowInsets.navigationBars)로 기기의 제스처바 영역만큼
                        // 자동으로 띄우고, 혹시 몰라 최소 8dp 여백도 추가로 깔아 안전하게 만든다.
                        // 탭이 5개로 늘어난 만큼, 아이콘/여백을 살짝 줄여서 한 화면에 여유 있게 들어가게 했다.
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
                                        .padding(horizontal = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        screen.icon,
                                        contentDescription = screen.title,
                                        tint = tint,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(1.dp))
                                    Text(
                                        text = screen.title,
                                        fontSize = 10.sp,
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
                            is Screen.Trend -> TrendScreen()
                        }
                    }
                }
            }
        }
    }

    // 앱이 이미 실행 중인 상태(백그라운드 등)에서 알림을 탭하면 onCreate가 아니라 이게 호출된다.
    // (매니페스트에 launchMode="singleTop"을 줘서, 기존 액티비티를 재사용하며 이 콜백을 받게 했다.)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        applyDeepLinkIfPresent(intent)
    }

    /** 알림의 PendingIntent에 담아 보낸 "분석 탭 열어줘" 신호를 확인해서 화면을 전환한다. */
    private fun applyDeepLinkIfPresent(intent: Intent?) {
        val targetScreen = intent?.getStringExtra(FridayAlarmReceiver.EXTRA_OPEN_SCREEN)
        if (targetScreen == FridayAlarmReceiver.SCREEN_ANALYSIS) {
            currentScreen = Screen.Analysis
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
