// File Path: app/src/main/java/com/kimro/ai/lotto/ui/analysis/AnalysisScreen.kt
package com.kimro.ai.lotto.ui.analysis

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    viewModel: AnalysisViewModel
) {
    val context = LocalContext.current
    val numberSets by viewModel.numberSets.collectAsState()
    val selectedCondition by viewModel.selectedCondition.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val sakaiInfoMessage by viewModel.sakaiInfoMessage.collectAsState()
    val saveMessage by viewModel.saveMessage.collectAsState()
    val favoriteNumbers by viewModel.favoriteNumbers.collectAsState()
    val excludedNumbers by viewModel.excludedNumbers.collectAsState()

    var showConditionDialog by remember { mutableStateOf(false) }
    var showLogicInfoDialog by remember { mutableStateOf(false) }
    var showManualPickDialog by remember { mutableStateOf(false) }
    var showFavoriteExcludeDialog by remember { mutableStateOf(false) }
    var selectedSetCount by remember { mutableIntStateOf(5) }

    LaunchedEffect(saveMessage) {
        saveMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearSaveMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "AI 스마트 로또 분석",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        style = androidx.compose.ui.text.TextStyle(
                            fontSize = 22.sp,
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFF0EA5E9), Color(0xFF7C3AED))
                            )
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF8FAFC)
                )
            )
        },
        containerColor = Color(0xFFF1F5F9)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // 1. 당첨번호 확인 링크 배너 + 판매점 찾기 버튼
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CheckWinResultBanner(modifier = Modifier.weight(1f))
                    FindStoreButton(modifier = Modifier.weight(1f))
                }
            }

            // 2. 스마트 패턴분석 영역
            item {
                SmartPatternAnalysisSection(
                    selectedCount = selectedSetCount,
                    onCountSelected = { count ->
                        selectedSetCount = count
                    },
                    onGenerateClick = {
                        when (selectedCondition) {
                            CONDITION_SAKAI -> viewModel.generateSakaiNumbers(selectedSetCount)
                            CONDITION_RANDOM -> viewModel.generateRandomNumbers(selectedSetCount)
                            CONDITION_AC_FILTER -> viewModel.generateAcFilteredNumbers(selectedSetCount)
                            CONDITION_BALANCE_FILTER -> viewModel.generateBalancedNumbers(selectedSetCount)
                            CONDITION_END_DIGIT_FILTER -> viewModel.generateEndDigitFilteredNumbers(selectedSetCount)
                            else -> viewModel.generateSmartNumbers(selectedSetCount) // CONDITION_ADVANCED(기본값) 등
                        }
                    },
                    favoriteNumbers = favoriteNumbers,
                    excludedNumbers = excludedNumbers,
                    onOpenFavoriteExcludeDialog = { showFavoriteExcludeDialog = true },
                    isGenerating = isGenerating,
                    sakaiInfoMessage = sakaiInfoMessage
                )
            }

            // 3. 조건변경 배너
            item {
                ConditionChangeBanner(
                    currentCondition = selectedCondition,
                    onClick = { showConditionDialog = true }
                )
            }

            // 3-1. 7대 로직이 무엇인지 설명하는 링크
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showLogicInfoDialog = true }
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "7대 로직이 뭔가요?",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0D9488)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "→",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0D9488)
                    )
                }
            }

            // 3-2. 직접 번호 선택 (버튼을 누르면 팝업으로 로또 용지 그리드가 뜬다)
            item {
                OutlinedButton(
                    onClick = { showManualPickDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.horizontalGradient(listOf(Color(0xFF0D9488), Color(0xFF0D9488)))
                    )
                ) {
                    Text(
                        text = "🎫  번호 직접 선택하기",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0D9488)
                    )
                }
            }

            // 4. 생성된 번호 조합 리스트 (카드 섹션) - 카드마다 펼쳐서 상세 분석을 볼 수 있음
            if (numberSets.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "생성된 AI 추천 조합 (${numberSets.size}개)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Button(
                            onClick = { viewModel.saveNumbers() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0EA5E9)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("내역 저장", fontSize = 13.sp, color = Color.White)
                        }
                    }
                }

                item {
                    Text(
                        text = "카드를 탭하면 조합별 통계 분석 리포트를 볼 수 있어요",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                itemsIndexed(numberSets) { index, set ->
                    LottoSetCard(
                        setIndex = index + 1,
                        numbers = set,
                        initiallyExpanded = index == 0,
                        onSaveClick = { viewModel.saveSingleSet(set) }
                    )
                }
            }
        }
    }

    if (showConditionDialog) {
        ConditionSelectDialog(
            currentCondition = selectedCondition,
            onSelect = {
                viewModel.setCondition(it)
                showConditionDialog = false
            },
            onDismiss = { showConditionDialog = false }
        )
    }

    if (showLogicInfoDialog) {
        LogicInfoDialog(onDismiss = { showLogicInfoDialog = false })
    }

    if (showManualPickDialog) {
        ManualPickDialog(
            onSaveClick = { numbers -> viewModel.saveSingleSet(numbers) },
            onDismiss = { showManualPickDialog = false }
        )
    }

    if (showFavoriteExcludeDialog) {
        FavoriteExcludeDialog(
            favoriteNumbers = favoriteNumbers,
            excludedNumbers = excludedNumbers,
            onToggleFavorite = { viewModel.toggleFavoriteNumber(it) },
            onToggleExcluded = { viewModel.toggleExcludedNumber(it) },
            onDismiss = { showFavoriteExcludeDialog = false }
        )
    }
}

// 7대 로직 각각에 대한 이름 + 짧은 설명
private data class LogicInfoItem(val title: String, val description: String)

private val sevenLogics = listOf(
    LogicInfoItem("① 홀짝 균형", "홀수와 짝수 개수를 3:3에 가깝게 맞춰 극단적인 편중을 방지합니다."),
    LogicInfoItem("② 고저 균형", "1~22(저구간)와 23~45(고구간)의 번호 개수를 고르게 배분합니다."),
    LogicInfoItem("③ 연속번호 제한", "번호가 연달아 이어지는 조합(예: 12,13)을 배제합니다."),
    LogicInfoItem("④ 끝수 분산", "끝자리 숫자가 3개 이상 겹치지 않도록 분산시킵니다."),
    LogicInfoItem("⑤ 총합 적정구간", "6개 번호의 합이 통계적으로 흔한 100~175 구간에 들도록 유도합니다."),
    LogicInfoItem("⑥ 구간 분포", "1~45를 5개 구간으로 나눠 번호가 여러 구간에 고르게 퍼지도록 합니다."),
    LogicInfoItem("⑦ AC값(번호 복잡도)", "번호 간 모든 차이값의 다양성을 측정하는 통계 지표로, 값이 높을수록(7~10) 규칙적인 패턴 없이 무작위성이 높은 조합입니다.")
)

@Composable
fun LogicInfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "고도화 종합 분석 - 7대 로직",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = Color(0xFF0F172A)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                sevenLogics.forEach { logic ->
                    Column {
                        Text(
                            text = logic.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0D9488)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = logic.description,
                            fontSize = 12.sp,
                            color = Color(0xFF475569),
                            lineHeight = 16.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "생성된 각 조합의 '상세 분석 보기'에서 이 7가지 지표를 조합별로 직접 확인할 수 있습니다.",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8),
                    lineHeight = 15.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("확인", color = Color(0xFF0EA5E9), fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White
    )
}

// 당첨번호 확인 링크 배너. 동행복권 서버가 앱의 자동 요청을 차단하고 있어
// 앱 안에서 번호를 직접 보여주는 대신, 공식 사이트로 바로 이동하는 링크만 제공한다.
@Composable
fun CheckWinResultBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable {
                try {
                    val intent = android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        // 특정 결과 페이지 대신 메인 홈페이지로 연결 - 메인 화면에 최신 회차 당첨번호가
                        // 바로 표시되고, 하위 경로보다 접속이 훨씬 안정적이다.
                        android.net.Uri.parse("https://www.dhlottery.co.kr/common.do?method=main")
                    ).apply {
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFF0284C7), Color(0xFF38BDF8))
                    )
                )
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "🎯", fontSize = 18.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "당첨번호 확인",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "동행복권 공식 사이트",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 9.sp
                )
            }
        }
    }
}

// 판매점 찾기 버튼. 앱 자체 지도/API 없이, 기기의 기본 지도 앱을 geo: 인텐트로 실행해
// "로또판매점"을 검색시킨다. 위치 권한이 앱에 없어도 지도 앱이 알아서 현재 위치 기준으로 찾아준다.
@Composable
fun FindStoreButton(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable {
                try {
                    val geoIntent = android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse("geo:0,0?q=로또판매점")
                    ).apply {
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(geoIntent)
                } catch (e: Exception) {
                    // 기기에 지도 앱이 없는 경우 - 웹 지도 검색으로 대체
                    try {
                        val fallbackIntent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://map.naver.com/v5/search/로또판매점")
                        ).apply {
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(fallbackIntent)
                    } catch (fallbackError: Exception) {
                        fallbackError.printStackTrace()
                    }
                }
            },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFF7C3AED), Color(0xFFA78BFA))
                    )
                )
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "📍", fontSize = 18.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "판매점 찾기",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "내 주변 지도로 보기",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 9.sp
                )
            }
        }
    }
}

@Composable
fun SmartPatternAnalysisSection(
    selectedCount: Int,
    onCountSelected: (Int) -> Unit,
    onGenerateClick: () -> Unit,
    favoriteNumbers: Set<Int> = emptySet(),
    excludedNumbers: Set<Int> = emptySet(),
    onOpenFavoriteExcludeDialog: () -> Unit = {},
    isGenerating: Boolean = false,
    sakaiInfoMessage: String? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                brush = Brush.horizontalGradient(colors = listOf(Color(0xFF0EA5E9), Color(0xFF7C3AED))),
                shape = RoundedCornerShape(22.dp)
            ),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Brush.horizontalGradient(listOf(Color(0xFF0EA5E9), Color(0xFF7C3AED)))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "스마트 패턴 분석",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    style = androidx.compose.ui.text.TextStyle(
                        fontSize = 18.sp,
                        brush = Brush.horizontalGradient(listOf(Color(0xFF0EA5E9), Color(0xFF7C3AED)))
                    )
                )
            }

            Text(
                text = "홀짝 균형, 고저 비율, 연속번호 제한 등 통계적 필터를 실시간 반영합니다. 생성된 조합은 몬테카를로 시뮬레이션으로 실제 당첨 확률까지 직접 검증해볼 수 있어요.",
                fontSize = 12.sp,
                color = Color(0xFF64748B),
                lineHeight = 16.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf(5, 10).forEach { count ->
                    val isSelected = selectedCount == count
                    OutlinedButton(
                        onClick = { onCountSelected(count) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isSelected) Color(0xFFE0F2FE) else Color.Transparent,
                            contentColor = if (isSelected) Color(0xFF0284C7) else Color(0xFF64748B)
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.horizontalGradient(
                                listOf(
                                    if (isSelected) Color(0xFF0284C7) else Color(0xFFCBD5E1),
                                    if (isSelected) Color(0xFF0284C7) else Color(0xFFCBD5E1)
                                )
                            )
                        )
                    ) {
                        Text(
                            text = "${count}개 조합",
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // 즐겨찾는 번호 / 기피 번호 설정 진입 영역
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF8FAFC))
                    .clickable { onOpenFavoriteExcludeDialog() }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "⭐ 즐겨찾는 번호 · 🚫 기피 번호",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = buildString {
                            if (favoriteNumbers.isEmpty() && excludedNumbers.isEmpty()) {
                                append("설정 안 함 · 탭해서 설정하기")
                            } else {
                                if (favoriteNumbers.isNotEmpty()) {
                                    append("즐겨찾기 ${favoriteNumbers.sorted().joinToString(", ")}")
                                }
                                if (excludedNumbers.isNotEmpty()) {
                                    if (favoriteNumbers.isNotEmpty()) append("  ·  ")
                                    append("기피 ${excludedNumbers.sorted().joinToString(", ")}")
                                }
                            }
                        },
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
                Text(text = "›", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
            }

            Button(
                onClick = onGenerateClick,
                enabled = !isGenerating,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0EA5E9),
                    disabledContainerColor = Color(0xFF93C5FD)
                )
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "과거 데이터 분석 중...", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                } else {
                    Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "AI 추천 번호 생성하기",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            if (!sakaiInfoMessage.isNullOrBlank()) {
                Surface(color = Color(0xFFF3E8FF), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "📊 $sakaiInfoMessage",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF7C3AED),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ConditionChangeBanner(
    currentCondition: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFF0D9488), Color(0xFF10B981))
                    )
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "적용된 분석 조건",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = currentCondition,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(20.dp),
                    shadowElevation = 2.dp
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "변경",
                            color = Color(0xFF0D9488),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "›",
                            color = Color(0xFF0D9488),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

/**
 * 로또 용지처럼 1~45 번호를 직접 눌러서 6개를 고르는 팝업.
 * 6개가 다 채워지면 AI 추천 조합과 완전히 동일한 통계 분석 리포트를 보여주고,
 * 마음에 들면 개별 저장할 수 있다.
 */
@Composable
fun ManualPickDialog(
    onSaveClick: (List<Int>) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedNumbers by remember { mutableStateOf(setOf<Int>()) }
    var saved by remember { mutableStateOf(false) }
    var showSimulationDialog by remember { mutableStateOf(false) }
    var showBacktestDialog by remember { mutableStateOf(false) }
    var showHotColdDialog by remember { mutableStateOf(false) }

    // 번호가 바뀌면 저장 완료 상태는 초기화
    LaunchedEffect(selectedNumbers) {
        saved = false
    }

    val sortedSelected = selectedNumbers.sorted()
    val isComplete = sortedSelected.size == 6

    val analysis = remember(sortedSelected) {
        if (isComplete) analyzeLottoSet(sortedSelected) else null
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 12.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color.White
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 상단 헤더 (동행복권 용지 타이틀 느낌)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "로또6/45 번호선택",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "닫기",
                            tint = Color(0xFF64748B)
                        )
                    }
                }
                HorizontalDivider(color = Color(0xFFE5E7EB))

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // 핫/콜드 번호 참고 버튼 - 번호 고르기 전에 참고할 수 있도록 맨 위에 배치
                    OutlinedButton(
                        onClick = { showHotColdDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
                    ) {
                        Text("🔥 핫/콜드 번호 보기", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 초기화 / 자동 채우기 버튼
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { selectedNumbers = emptySet() },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(6.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = Brush.horizontalGradient(listOf(Color(0xFFD1D5DB), Color(0xFFD1D5DB)))
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF374151))
                        ) {
                            Text("초기화", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                        OutlinedButton(
                            onClick = {
                                val remaining = (1..45).filter { it !in selectedNumbers }
                                val need = 6 - selectedNumbers.size
                                if (need > 0) {
                                    selectedNumbers = selectedNumbers + remaining.shuffled().take(need)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(6.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = Brush.horizontalGradient(listOf(Color(0xFFD1D5DB), Color(0xFFD1D5DB)))
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF374151))
                        ) {
                            Text("자동 채우기", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 용지 느낌의 박스 (번호 그리드) - 금액 표시는 제외
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFF9A8A8), RoundedCornerShape(4.dp))
                    ) {
                        // 1~45 번호 그리드 (7열)
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            (1..45).chunked(7).forEach { rowNumbers ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    rowNumbers.forEach { number ->
                                        val isSelected = number in selectedNumbers
                                        val canSelectMore = selectedNumbers.size < 6

                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .then(
                                                    if (isSelected) {
                                                        Modifier.background(Color(0xFFF2626B))
                                                    } else {
                                                        Modifier.border(1.dp, Color(0xFFF2A0A5), RoundedCornerShape(4.dp))
                                                    }
                                                )
                                                .clickable(enabled = isSelected || canSelectMore) {
                                                    selectedNumbers = if (isSelected) {
                                                        selectedNumbers - number
                                                    } else {
                                                        selectedNumbers + number
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = number.toString(),
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (isSelected) Color.White else Color(0xFFF2626B)
                                            )
                                        }
                                    }
                                    // 마지막 줄(45까지 3개뿐)일 때 빈 칸 채워서 정렬 유지
                                    repeat(7 - rowNumbers.size) {
                                        Spacer(modifier = Modifier.size(38.dp))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "내가 선택한 번호",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF374151)
                        )
                        Text(
                            text = "${sortedSelected.size} / 6",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isComplete) Color(0xFF10B981) else Color(0xFF94A3B8)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = Color(0xFFE5E7EB))
                    Spacer(modifier = Modifier.height(10.dp))

                    if (sortedSelected.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            sortedSelected.forEach { number ->
                                LottoBall(number = number, size = 30)
                            }
                        }
                    } else {
                        Text(
                            text = "번호를 선택하면 여기에 표시됩니다",
                            fontSize = 12.sp,
                            color = Color(0xFF9CA3AF)
                        )
                    }

                    if (isComplete && analysis != null) {
                        Spacer(modifier = Modifier.height(18.dp))
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                        Spacer(modifier = Modifier.height(14.dp))
                        AnalysisReportSection(analysis)

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                onSaveClick(sortedSelected)
                                saved = true
                            },
                            enabled = !saved,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (saved) Color(0xFFE2E8F0) else Color(0xFF10B981),
                                disabledContainerColor = Color(0xFFE2E8F0)
                            )
                        ) {
                            Text(
                                text = if (saved) "저장됨" else "이 조합 저장하기",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (saved) Color(0xFF64748B) else Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = { showSimulationDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "💰 당첨 확률 시뮬레이션",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF7C3AED)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = { showBacktestDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "📜 과거 회차 백테스트",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0EA5E9)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }

    if (showSimulationDialog) {
        SimulationDialog(
            numbers = sortedSelected,
            onDismiss = { showSimulationDialog = false }
        )
    }

    if (showBacktestDialog) {
        BacktestDialog(
            numbers = sortedSelected,
            onDismiss = { showBacktestDialog = false }
        )
    }

    if (showHotColdDialog) {
        HotColdDialog(onDismiss = { showHotColdDialog = false })
    }
}


/**
 * 즐겨찾는 번호(항상 포함) / 기피 번호(항상 제외)를 설정하는 팝업.
 * 상단에서 모드를 고른 뒤(⭐ 즐겨찾기 / 🚫 기피) 번호를 탭하면 해당 목록에 추가/제거된다.
 * 같은 번호가 동시에 즐겨찾기이면서 기피일 수는 없다 - 하나를 누르면 반대쪽에서는 자동으로 빠진다.
 */
@Composable
fun FavoriteExcludeDialog(
    favoriteNumbers: Set<Int>,
    excludedNumbers: Set<Int>,
    onToggleFavorite: (Int) -> Unit,
    onToggleExcluded: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var isFavoriteMode by remember { mutableStateOf(true) }
    var isLoadingRecommendation by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 12.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color.White
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "즐겨찾기 · 기피 번호 설정",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "닫기", tint = Color(0xFF94A3B8))
                    }
                }
                HorizontalDivider(color = Color(0xFFF1F5F9))

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp)
                ) {
                    // 모드 선택 (즐겨찾기 / 기피)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val favoriteSelected = isFavoriteMode
                        Button(
                            onClick = { isFavoriteMode = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (favoriteSelected) Color(0xFFFACC15) else Color(0xFFF1F5F9)
                            )
                        ) {
                            Text(
                                text = "⭐ 즐겨찾기 (${favoriteNumbers.size}/6)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (favoriteSelected) Color(0xFF3F2D00) else Color(0xFF64748B)
                            )
                        }
                        Button(
                            onClick = { isFavoriteMode = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!favoriteSelected) Color(0xFFEF4444) else Color(0xFFF1F5F9)
                            )
                        ) {
                            Text(
                                text = "🚫 기피 (${excludedNumbers.size})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (!favoriteSelected) Color.White else Color(0xFF64748B)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (isFavoriteMode)
                            "생성되는 모든 조합에 항상 포함시킬 번호를 최대 6개까지 골라주세요."
                        else
                            "생성되는 모든 조합에서 절대 나오지 않길 원하는 번호를 골라주세요.",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        lineHeight = 16.sp
                    )

                    if (!isFavoriteMode) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                if (!isLoadingRecommendation) {
                                    isLoadingRecommendation = true
                                    coroutineScope.launch {
                                        try {
                                            val draws = fetchHistoricalDraws()
                                            val frequencies = computeNumberFrequencies(draws)
                                            val hotTop10 = frequencies.sortedByDescending { it.count }.take(10).map { it.number }
                                            hotTop10.forEach { number ->
                                                if (number !in excludedNumbers) onToggleExcluded(number)
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        } finally {
                                            isLoadingRecommendation = false
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
                        ) {
                            if (isLoadingRecommendation) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color(0xFFEF4444))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("과거 데이터 확인 중...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Text("🔥 인기번호(핫넘버) 자동 기피 추천", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "실제 과거 데이터에서 가장 자주 나온 번호 10개를 기피 목록에 추가합니다. " +
                                    "당첨 확률과는 무관하고, 당첨금을 나눠 갖는 인기번호를 피하고 싶을 때 참고용으로만 쓰세요.",
                            fontSize = 9.sp,
                            color = Color(0xFF94A3B8),
                            lineHeight = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 1~45 번호 그리드
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        (1..45).chunked(7).forEach { rowNumbers ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                rowNumbers.forEach { number ->
                                    val isFav = number in favoriteNumbers
                                    val isExcl = number in excludedNumbers

                                    val bgColor = when {
                                        isFav -> Color(0xFFFACC15)
                                        isExcl -> Color(0xFFEF4444)
                                        else -> Color(0xFFF1F5F9)
                                    }
                                    val textColor = when {
                                        isFav -> Color(0xFF3F2D00)
                                        isExcl -> Color.White
                                        else -> Color(0xFF64748B)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(bgColor)
                                            .clickable {
                                                if (isFavoriteMode) onToggleFavorite(number)
                                                else onToggleExcluded(number)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = number.toString(),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textColor
                                        )
                                    }
                                }
                                repeat(7 - rowNumbers.size) {
                                    Spacer(modifier = Modifier.size(38.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "⭐ 노란색 = 즐겨찾기 · 🔴 빨간색 = 기피 · 같은 번호를 두 목록에 동시에 넣을 수는 없어요.",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

/**
 * 조합 1개를 보여주는 카드. 우측 화살표를 누르면 통계 분석 리포트가 펼쳐진다.
 */
@Composable
fun LottoSetCard(
    setIndex: Int,
    numbers: List<Int>,
    initiallyExpanded: Boolean = false,
    onSaveClick: () -> Unit = {}
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    var saved by remember(numbers) { mutableStateOf(false) }
    var showSimulationDialog by remember { mutableStateOf(false) }
    var showBacktestDialog by remember { mutableStateOf(false) }
    val analysis = remember(numbers) {
        analyzeLottoSet(numbers)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${setIndex}세트",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    fontSize = 13.sp
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    numbers.forEach { number ->
                        LottoBall(number = number, size = 32)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 탭 가능한 영역임을 명확히 알려주는 라벨 + 화살표 (배경색으로 버튼처럼 보이게)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF8FAFC))
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (expanded) "상세 분석 접기" else "상세 분석 보기",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0EA5E9)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "분석 접기" else "분석 펼치기",
                    tint = Color(0xFF0EA5E9),
                    modifier = Modifier.size(18.dp)
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFF1F5F9))
                Spacer(modifier = Modifier.height(12.dp))
                AnalysisReportSection(analysis)

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        onSaveClick()
                        saved = true
                    },
                    enabled = !saved,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (saved) Color(0xFFE2E8F0) else Color(0xFF10B981),
                        disabledContainerColor = Color(0xFFE2E8F0)
                    )
                ) {
                    if (saved) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "이 조합 저장됨",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B)
                        )
                    } else {
                        Text(
                            text = "이 조합만 저장하기",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { showSimulationDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "💰 당첨 확률 시뮬레이션",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF7C3AED)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { showBacktestDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "📜 과거 회차 백테스트",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0EA5E9)
                    )
                }
            }
        }
    }

    if (showSimulationDialog) {
        SimulationDialog(
            numbers = numbers,
            onDismiss = { showSimulationDialog = false }
        )
    }

    if (showBacktestDialog) {
        BacktestDialog(
            numbers = numbers,
            onDismiss = { showBacktestDialog = false }
        )
    }
}

/**
 * 펼쳤을 때 보여줄 통계 분석 리포트.
 * 홀짝/고저 비율 게이지, 연속번호·끝수중복 여부, 총합, 구간 분포, 종합 점수를 표시한다.
 */
// 로또 6/45 공식 등수별 확률 (전체 조합 수 C(45,6) = 8,145,060 기준으로 계산된 실제 통계값).
// 어떤 번호를 고르든 완전 무작위 추첨이므로 이 확률은 항상 동일하다 - 특정 번호가 더 유리하지 않다.
private data class RankOdds(val rank: Int, val label: String, val singleGameProbability: Double)

private val LOTTO_RANK_ODDS = listOf(
    RankOdds(1, "1등 (6개 일치)", 1.0 / 8_145_060.0),
    RankOdds(2, "2등 (5개+보너스)", 6.0 / 8_145_060.0),
    RankOdds(3, "3등 (5개 일치)", 228.0 / 8_145_060.0),
    RankOdds(4, "4등 (4개 일치)", 11_115.0 / 8_145_060.0),
    RankOdds(5, "5등 (3개 일치)", 182_780.0 / 8_145_060.0)
)

private fun formatProbabilityPercent(p: Double): String {
    val percent = p * 100
    return when {
        percent >= 1.0 -> String.format("%.2f%%", percent)
        percent >= 0.01 -> String.format("%.4f%%", percent)
        else -> String.format("%.6f%%", percent)
    }
}

private fun formatOddsFraction(p: Double): String {
    if (p <= 0.0) return "-"
    val n = (1.0 / p).roundToLong()
    return "약 1 / ${"%,d".format(n)}"
}

/**
 * 몬테카를로 시뮬레이션: 사용자가 고른 6개 번호를 고정해두고, 실제 로또 추첨과 동일한 방식으로
 * (6개 메인 번호 + 보너스 번호 1개) 가상 추첨을 trials번 반복해서 등수별 적중 횟수를 센다.
 * 결과는 시행 횟수가 많아질수록 이론적 확률(LOTTO_RANK_ODDS)에 점점 가까워져야 정상이다.
 * CPU 연산량이 있어 반드시 Dispatchers.Default(백그라운드 스레드)에서 실행한다.
 */
private suspend fun runMonteCarloSimulation(userNumbers: List<Int>, trials: Int): IntArray {
    return withContext(Dispatchers.Default) {
        // index: 0=1등, 1=2등, 2=3등, 3=4등, 4=5등, 5=낙첨
        val rankCounts = IntArray(6)

        val userMarked = BooleanArray(46)
        userNumbers.forEach { if (it in 1..45) userMarked[it] = true }

        val rng = java.util.Random()
        val used = BooleanArray(46)
        val mainDraw = IntArray(6)

        repeat(trials) {
            // Floyd's algorithm으로 1~45 중 서로 다른 6개를 무작위로 뽑는다 (매 시행 O(6))
            var idx = 0
            for (j in 40..45) {
                val t = rng.nextInt(j) + 1
                if (!used[t]) {
                    used[t] = true
                    mainDraw[idx++] = t
                } else {
                    used[j] = true
                    mainDraw[idx++] = j
                }
            }

            // 보너스 번호: 메인 6개와 겹치지 않는 번호 중 하나
            var bonus: Int
            do {
                bonus = rng.nextInt(45) + 1
            } while (used[bonus])

            var matches = 0
            for (n in mainDraw) if (userMarked[n]) matches++

            when {
                matches == 6 -> rankCounts[0]++
                matches == 5 && userMarked[bonus] -> rankCounts[1]++
                matches == 5 -> rankCounts[2]++
                matches == 4 -> rankCounts[3]++
                matches == 3 -> rankCounts[4]++
                else -> rankCounts[5]++
            }

            for (n in mainDraw) used[n] = false
        }

        rankCounts
    }
}

// 실제 과거 당첨 회차 1개를 담는 데이터. smok95.github.io의 공개 데이터셋에서 가져온다.
// (동행복권 도메인이 아니라 GitHub Pages라서 API 차단 문제가 없다.)
data class HistoricalDraw(val drawNo: Int, val numbers: List<Int>, val bonusNo: Int)

// 같은 세션 안에서 백테스트/핫콜드 등 여러 기능이 반복해서 전체 회차를 새로 받아오지 않도록
// 한 번 받아온 결과를 메모리에 캐시해둔다.
object HistoricalDrawCache {
    var draws: List<HistoricalDraw>? = null
}

/**
 * 로또 전체 회차(1회~최신) 과거 당첨 데이터를 실시간으로 받아온다.
 * 출처: smok95/lotto (GitHub, 커뮤니티가 관리하는 공개 데이터셋). 동행복권 공식 API가 아니므로
 * 최신 회차 반영이 늦거나 일시적으로 접속이 안 될 수 있다.
 */
suspend fun fetchHistoricalDraws(): List<HistoricalDraw> {
    HistoricalDrawCache.draws?.let { return it }

    return withContext(Dispatchers.IO) {
        val connection = URL("https://smok95.github.io/lotto/results/all.json").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 15000
            val text = connection.inputStream.bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(text)
            val result = mutableListOf<HistoricalDraw>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val drawNo = obj.getInt("draw_no")
                val numsArray = obj.getJSONArray("numbers")
                val nums = (0 until numsArray.length()).map { numsArray.getInt(it) }
                val bonus = obj.optInt("bonus_no", -1)
                result.add(HistoricalDraw(drawNo, nums, bonus))
            }
            result
        } finally {
            connection.disconnect()
        }
    }.also { HistoricalDrawCache.draws = it }
}

// 번호 1개 + 전체 회차 동안 나온 횟수
private data class NumberFrequency(val number: Int, val count: Int)

/** 1~45 각 번호가 과거 전체 회차 동안 몇 번 나왔는지 센다. */
private fun computeNumberFrequencies(draws: List<HistoricalDraw>): List<NumberFrequency> {
    val counts = IntArray(46)
    draws.forEach { draw -> draw.numbers.forEach { if (it in 1..45) counts[it]++ } }
    return (1..45).map { NumberFrequency(it, counts[it]) }
}

/**
 * 실제 과거 데이터를 기준으로 가장 자주 나온 번호(핫)와 가장 드물게 나온 번호(콜드)를 보여주는 팝업.
 */
@Composable
fun HotColdDialog(onDismiss: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var frequencies by remember { mutableStateOf<List<NumberFrequency>?>(null) }
    var totalDraws by remember { mutableStateOf(0) }

    fun load() {
        isLoading = true
        errorMessage = null
        coroutineScope.launch {
            try {
                val draws = fetchHistoricalDraws()
                totalDraws = draws.size
                frequencies = computeNumberFrequencies(draws)
            } catch (e: Exception) {
                errorMessage = "데이터를 불러오지 못했습니다 (${e.javaClass.simpleName}). 네트워크 상태를 확인 후 다시 시도해주세요."
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) { load() }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "핫 / 콜드 번호",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(26.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "닫기", tint = Color(0xFF94A3B8))
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "실제 과거 전체 회차 동안 번호별 출현 횟수를 집계한 결과입니다.",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                when {
                    isLoading -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color(0xFFEF4444))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("과거 회차 데이터 불러오는 중...", fontSize = 12.sp, color = Color(0xFF64748B))
                        }
                    }
                    errorMessage != null -> {
                        Text(errorMessage ?: "", fontSize = 12.sp, color = Color(0xFFEF4444), lineHeight = 16.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { load() },
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                        ) {
                            Text("다시 시도", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                    frequencies != null -> {
                        Surface(color = Color(0xFFFEF2F2), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "총 ${"%,d".format(totalDraws)}개 회차 기준",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF4444),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "🔥 핫 넘버 TOP 10 (가장 자주 나온 번호)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        Spacer(modifier = Modifier.height(8.dp))

                        val hotTop10 = frequencies!!.sortedByDescending { it.count }.take(10)
                        FrequencyGrid(items = hotTop10, ballColor = Color(0xFFEF4444))

                        Spacer(modifier = Modifier.height(20.dp))
                        Text(text = "❄️ 콜드 넘버 TOP 10 (가장 드물게 나온 번호)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        Spacer(modifier = Modifier.height(8.dp))

                        val coldTop10 = frequencies!!.sortedBy { it.count }.take(10)
                        FrequencyGrid(items = coldTop10, ballColor = Color(0xFF3B82F6))

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "⚠️ 과거에 자주(또는 드물게) 나왔다고 해서 앞으로도 그럴 확률이 높거나 낮은 건 아닙니다. " +
                                    "로또는 매 회차 완전히 독립적인 무작위 추첨이라, 이전 결과가 다음 결과에 영향을 주지 않습니다. " +
                                    "이 정보는 순수하게 과거 기록일 뿐입니다.",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8),
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FrequencyGrid(items: List<NumberFrequency>, ballColor: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(5).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                row.forEach { item ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(ballColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "${item.number}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(text = "${item.count}회", fontSize = 10.sp, color = Color(0xFF94A3B8))
                    }
                }
                repeat(5 - row.size) { Spacer(modifier = Modifier.size(36.dp)) }
            }
        }
    }
}

/**
 * 사용자가 고른 번호 6개를, 실제로 존재했던 모든 과거 회차와 하나씩 비교해서
 * "만약 이 번호로 매 회차 응모했다면 몇 등이 몇 번 나왔을까"를 계산한다.
 * 몬테카를로(가상 무작위 추첨)와 달리 이건 진짜 있었던 역사적 사실을 기준으로 한다.
 */
private fun computeBacktest(userNumbers: List<Int>, draws: List<HistoricalDraw>): IntArray {
    val userMarked = BooleanArray(46)
    userNumbers.forEach { if (it in 1..45) userMarked[it] = true }

    // index: 0=1등, 1=2등, 2=3등, 3=4등, 4=5등, 5=낙첨
    val rankCounts = IntArray(6)
    draws.forEach { draw ->
        val matches = draw.numbers.count { it in 1..45 && userMarked[it] }
        val bonusMatched = draw.bonusNo in 1..45 && userMarked[draw.bonusNo]
        when {
            matches == 6 -> rankCounts[0]++
            matches == 5 && bonusMatched -> rankCounts[1]++
            matches == 5 -> rankCounts[2]++
            matches == 4 -> rankCounts[3]++
            matches == 3 -> rankCounts[4]++
            else -> rankCounts[5]++
        }
    }
    return rankCounts
}

private val BACKTEST_RANK_LABELS = listOf("1등 (6개 일치)", "2등 (5개+보너스)", "3등 (5개 일치)", "4등 (4개 일치)", "5등 (3개 일치)")

/**
 * "이 번호로 과거에 실제로 있었던 모든 회차에 응모했다면 어떤 결과였을까?"를 보여주는 백테스트 팝업.
 * 몬테카를로처럼 가상의 무작위 추첨이 아니라, 실존했던 당첨 회차 데이터를 그대로 사용한다.
 */
@Composable
fun BacktestDialog(
    numbers: List<Int>,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var historicalDraws by remember { mutableStateOf<List<HistoricalDraw>?>(null) }
    var backtestResult by remember { mutableStateOf<IntArray?>(null) }

    fun runBacktest() {
        isLoading = true
        errorMessage = null
        coroutineScope.launch {
            try {
                val draws = historicalDraws ?: fetchHistoricalDraws().also { historicalDraws = it }
                backtestResult = computeBacktest(numbers, draws)
            } catch (e: Exception) {
                errorMessage = "데이터를 불러오지 못했습니다 (${e.javaClass.simpleName}). 네트워크 상태를 확인 후 다시 시도해주세요."
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) { runBacktest() }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "과거 회차 백테스트",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(26.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "닫기", tint = Color(0xFF94A3B8))
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    numbers.sorted().forEach { number ->
                        LottoBall(number = number, size = 26)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "이 번호로 1회부터 지금까지 실제로 있었던 모든 회차에 응모했다면?",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                when {
                    isLoading -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color(0xFF7C3AED))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("과거 회차 데이터 불러오는 중...", fontSize = 12.sp, color = Color(0xFF64748B))
                        }
                    }
                    errorMessage != null -> {
                        Text(errorMessage ?: "", fontSize = 12.sp, color = Color(0xFFEF4444), lineHeight = 16.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { runBacktest() },
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
                        ) {
                            Text("다시 시도", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                    backtestResult != null -> {
                        val totalDraws = historicalDraws?.size ?: 0
                        Surface(color = Color(0xFFF3E8FF), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "총 ${"%,d".format(totalDraws)}개 회차 데이터 기준",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF7C3AED),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            BACKTEST_RANK_LABELS.forEachIndexed { index, label ->
                                val count = backtestResult!![index]
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                                    Text(
                                        text = if (count > 0) "${count}회" else "0회",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (count > 0) Color(0xFF10B981) else Color(0xFF94A3B8)
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("낙첨", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                Text("${backtestResult!![5]}회", fontSize = 11.sp, color = Color(0xFF94A3B8))
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "📜 이 데이터는 커뮤니티가 관리하는 공개 회차 기록(GitHub: smok95/lotto)을 사용합니다. " +
                                    "동행복권 공식 데이터가 아니라 최신 회차 반영이 늦을 수 있습니다. " +
                                    "과거에 이랬다는 사실일 뿐, 미래 당첨을 예측하거나 보장하지 않습니다 — 매 회차는 완전히 독립적인 무작위 추첨입니다.",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8),
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * "이 번호가 이번 회차에 등수별로 당첨될 확률이 얼마나 될까?"를 보여주는 시뮬레이션 팝업.
 * 확률 자체는 어떤 번호를 쓰든 동일하므로, numbers는 화면 상단에 "이 번호" 표시용으로만 쓰인다.
 */
@Composable
fun SimulationDialog(
    numbers: List<Int>,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isSimulating by remember { mutableStateOf(false) }
    var simulationResult by remember { mutableStateOf<IntArray?>(null) }
    val trialOptions = listOf(10_000, 50_000, 100_000)
    var selectedTrialIndex by remember { mutableStateOf(0) } // 기본값: 10,000회 (속도/정확도 균형)
    val trials = trialOptions[selectedTrialIndex]

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "당첨 확률 시뮬레이션",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(26.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "닫기", tint = Color(0xFF94A3B8))
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    numbers.sorted().forEach { number ->
                        LottoBall(number = number, size = 26)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "이번 회차 기준 등수별 당첨 확률 (이론값)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LOTTO_RANK_ODDS.forEachIndexed { index, rankOdds ->
                        val empiricalCount = simulationResult?.get(index)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = rankOdds.label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF334155)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = formatProbabilityPercent(rankOdds.singleGameProbability),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0EA5E9)
                                    )
                                    Text(
                                        text = formatOddsFraction(rankOdds.singleGameProbability),
                                        fontSize = 10.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                                if (empiricalCount != null) {
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = formatProbabilityPercent(empiricalCount.toDouble() / trials),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF7C3AED)
                                        )
                                        Text(
                                            text = "${empiricalCount}회 적중",
                                            fontSize = 10.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "시뮬레이션 시행 횟수",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF334155)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    trialOptions.forEachIndexed { index, option ->
                        val isSelected = index == selectedTrialIndex
                        OutlinedButton(
                            onClick = {
                                if (!isSimulating && selectedTrialIndex != index) {
                                    selectedTrialIndex = index
                                    simulationResult = null // 시행 횟수가 바뀌면 이전 결과는 더 이상 유효하지 않으므로 초기화
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isSelected) Color(0xFFF3E8FF) else Color.Transparent,
                                contentColor = if (isSelected) Color(0xFF7C3AED) else Color(0xFF94A3B8)
                            )
                        ) {
                            Text(
                                text = if (option >= 10_000) "${option / 10_000}만" else "${"%,d".format(option)}",
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "🎯 횟수를 늘려도 당첨 확률이 오르진 않아요 — 검증이 더 정밀해질 뿐이에요",
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8)
                )

                if (simulationResult != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(text = "🔵 이론값   🟣 시뮬레이션값(${"%,d".format(trials)}회 가상추첨)", fontSize = 10.sp, color = Color(0xFF94A3B8))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        if (!isSimulating) {
                            isSimulating = true
                            coroutineScope.launch {
                                val result = runMonteCarloSimulation(numbers, trials)
                                simulationResult = result
                                isSimulating = false
                            }
                        }
                    },
                    enabled = !isSimulating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
                ) {
                    if (isSimulating) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("가상 추첨 ${"%,d".format(trials)}회 진행 중...", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    } else {
                        Text(
                            text = if (simulationResult == null) "🎲 몬테카를로 시뮬레이션 실행 (${"%,d".format(trials)}회)" else "🎲 다시 실행",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "📊 이론값은 로또 6/45의 전체 조합 8,145,060개를 기준으로 계산한 실제 공식 확률입니다. " +
                            "완전 무작위 추첨이므로 어떤 번호를 선택하든 확률은 항상 동일합니다.",
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8),
                    lineHeight = 14.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "🎲 몬테카를로 시뮬레이션은 예측이 아니라 검증입니다. 6개 메인 번호와 보너스 번호를 매번 새로 무작위 " +
                            "추첨해서 이 번호가 실제로 몇 번 적중하는지 세어본 결과이며, 시행 횟수가 많을수록 위 이론값에 " +
                            "가까워지는지 눈으로 확인하는 용도입니다.",
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8),
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
fun AnalysisReportSection(analysis: LottoSetAnalysis) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // 종합 점수 배지
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "통계 분석 리포트",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            val scoreColor = when {
                analysis.score >= 85 -> Color(0xFF10B981)
                analysis.score >= 65 -> Color(0xFF0EA5E9)
                else -> Color(0xFFF59E0B)
            }
            Surface(
                color = scoreColor.copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "균형도 점수 ${analysis.score}점",
                    color = scoreColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        // 홀짝 비율 게이지
        RatioGaugeRow(
            label = "홀짝 비율",
            leftLabel = "홀 ${analysis.oddCount}",
            rightLabel = "짝 ${analysis.evenCount}",
            leftValue = analysis.oddCount,
            total = 6,
            barColor = Color(0xFF3B82F6)
        )

        // 고저 비율 게이지
        RatioGaugeRow(
            label = "고저 비율",
            leftLabel = "저 ${analysis.lowCount}",
            rightLabel = "고 ${analysis.highCount}",
            leftValue = analysis.lowCount,
            total = 6,
            barColor = Color(0xFFF59E0B)
        )

        // 체크 리스트형 지표들
        AnalysisCheckRow(
            label = "연속번호",
            passed = !analysis.hasConsecutive,
            passedText = "연속번호 없음",
            failedText = "연속번호 포함"
        )
        AnalysisCheckRow(
            label = "끝수 분산",
            passed = !analysis.hasTooManySameEndDigits,
            passedText = "끝수 중복 없음",
            failedText = "동일 끝수 3개 이상"
        )
        AnalysisCheckRow(
            label = "총합 (${analysis.sum})",
            passed = analysis.isSumInNormalRange,
            passedText = "적정 구간(100~175)",
            failedText = "통계적 적정 구간 벗어남"
        )
        AnalysisCheckRow(
            label = "AC값 (${analysis.acValue})",
            passed = analysis.isAcGood,
            passedText = "무작위성 양호(7 이상)",
            failedText = "규칙적인 패턴 존재"
        )

        // 구간 분포 / 표준편차 / 평균 간격 정보
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InfoChip(
                modifier = Modifier.weight(1f),
                title = "구간 분포",
                value = "${analysis.occupiedDecadeBins} / 5구간"
            )
            InfoChip(
                modifier = Modifier.weight(1f),
                title = "표준편차",
                value = String.format("%.1f", analysis.stdDeviation)
            )
            InfoChip(
                modifier = Modifier.weight(1f),
                title = "평균 간격",
                value = String.format("%.1f", analysis.avgGap)
            )
        }

        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "⚠️ 이 점수는 번호 배열이 통계적으로 얼마나 자연스러운지를 7대 로직 기준으로 계산한 참고용 지표입니다. 로또는 매회 완전히 독립적인 무작위 추첨이므로, 이 점수가 높다고 해서 실제 당첨 확률이 올라가는 것은 아닙니다.",
            fontSize = 10.sp,
            color = Color(0xFF94A3B8),
            lineHeight = 14.sp
        )
    }
}

@Composable
fun RatioGaugeRow(
    label: String,
    leftLabel: String,
    rightLabel: String,
    leftValue: Int,
    total: Int,
    barColor: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontSize = 11.sp, color = Color(0xFF64748B))
            Text(
                text = "$leftLabel : $rightLabel",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF334155)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { leftValue.toFloat() / total.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = barColor,
            trackColor = Color(0xFFE2E8F0)
        )
    }
}

@Composable
fun AnalysisCheckRow(
    label: String,
    passed: Boolean,
    passedText: String,
    failedText: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = Color(0xFF64748B))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (passed) Icons.Default.Check else Icons.Default.Warning,
                contentDescription = null,
                tint = if (passed) Color(0xFF10B981) else Color(0xFFF59E0B),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (passed) passedText else failedText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (passed) Color(0xFF10B981) else Color(0xFFF59E0B)
            )
        }
    }
}

@Composable
fun InfoChip(modifier: Modifier = Modifier, title: String, value: String) {
    Surface(
        modifier = modifier,
        color = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(text = title, fontSize = 10.sp, color = Color(0xFF94A3B8))
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF334155)
            )
        }
    }
}

@Composable
fun LottoBall(number: Int, size: Int = 34) {
    val ballColor = when (number) {
        in 1..10 -> Color(0xFFF59E0B)
        in 11..20 -> Color(0xFF3B82F6)
        in 21..30 -> Color(0xFFEF4444)
        in 31..40 -> Color(0xFF64748B)
        else -> Color(0xFF10B981)
    }

    Box(
        modifier = Modifier
            .size(size.dp)
            .background(ballColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number.toString(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size / 2.5).sp
        )
    }
}

@Composable
fun ConditionSelectDialog(
    currentCondition: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val conditions = listOf(
        CONDITION_ADVANCED,
        CONDITION_SAKAI,
        CONDITION_RANDOM,
        CONDITION_AC_FILTER,
        CONDITION_BALANCE_FILTER,
        CONDITION_END_DIGIT_FILTER
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "분석 조건 선택",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                conditions.forEach { condition ->
                    val isSelected = condition == currentCondition
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) Color(0xFFE0F2FE) else Color.Transparent)
                            .clickable { onSelect(condition) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = condition,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color(0xFF0284C7) else Color(0xFF334155)
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { /* 확인 버튼은 사용하지 않음 - 목록에서 바로 선택되는 구조 */ },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소", color = Color(0xFF64748B))
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White
    )
}
