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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    viewModel: AnalysisViewModel
) {
    val context = LocalContext.current
    val numberSets by viewModel.numberSets.collectAsState()
    val selectedCondition by viewModel.selectedCondition.collectAsState()
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
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
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
            // 1. 당첨번호 확인 링크 배너 (앱 내 자동 조회 대신 공식 사이트로 안내)
            item {
                CheckWinResultBanner()
            }

            // 2. 스마트 패턴분석 영역
            item {
                SmartPatternAnalysisSection(
                    selectedCount = selectedSetCount,
                    onCountSelected = { count ->
                        selectedSetCount = count
                    },
                    onGenerateClick = {
                        viewModel.generateSmartNumbers(selectedSetCount)
                    },
                    favoriteNumbers = favoriteNumbers,
                    excludedNumbers = excludedNumbers,
                    onOpenFavoriteExcludeDialog = { showFavoriteExcludeDialog = true }
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
                        text = "6대 로직이 뭔가요?",
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

// 6대 로직 각각에 대한 이름 + 짧은 설명
private data class LogicInfoItem(val title: String, val description: String)

private val sevenLogics = listOf(
    LogicInfoItem("① 홀짝 균형", "홀수와 짝수 개수를 3:3에 가깝게 맞춰 극단적인 편중을 방지합니다."),
    LogicInfoItem("② 고저 균형", "1~22(저구간)와 23~45(고구간)의 번호 개수를 고르게 배분합니다."),
    LogicInfoItem("③ 연속번호 제한", "번호가 연달아 이어지는 조합(예: 12,13)을 배제합니다."),
    LogicInfoItem("④ 끝수 분산", "끝자리 숫자가 3개 이상 겹치지 않도록 분산시킵니다."),
    LogicInfoItem("⑤ 총합 적정구간", "6개 번호의 합이 통계적으로 흔한 100~175 구간에 들도록 유도합니다."),
    LogicInfoItem("⑥ 구간 분포", "1~45를 5개 구간으로 나눠 번호가 여러 구간에 고르게 퍼지도록 합니다.")
)

@Composable
fun LogicInfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "고도화 종합 분석 - 6대 로직",
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
fun CheckWinResultBanner() {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable {
                try {
                    val intent = android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse("https://www.dhlottery.co.kr/gameResult.do?method=byWin")
                    ).apply {
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFF0284C7), Color(0xFF38BDF8))
                    )
                )
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "이번 주 당첨번호 확인하기",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "동행복권 공식 사이트에서 바로 확인하세요",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp
                    )
                }
                Text(
                    text = "→",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
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
    onOpenFavoriteExcludeDialog: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFF0EA5E9),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "스마트 패턴 분석",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            }

            Text(
                text = "홀짝 균형, 고저 비율, 연속번호 제한 등 통계적 필터를 실시간 반영합니다.",
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0EA5E9)
                )
            ) {
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

                    // 용지 느낌의 박스 (상단 가격 바 + 번호 그리드)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFF9A8A8), RoundedCornerShape(4.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(34.dp)
                                .background(Color(0xFFF2626B)),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Text(
                                text = "${sortedSelected.size * 1000}원",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(end = 14.dp)
                            )
                        }

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
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
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
            }
        }
    }
}

/**
 * 펼쳤을 때 보여줄 통계 분석 리포트.
 * 홀짝/고저 비율 게이지, 연속번호·끝수중복 여부, 총합, 구간 분포, 종합 점수를 표시한다.
 */
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

        // 구간 분포 정보
        InfoChip(
            modifier = Modifier.fillMaxWidth(),
            title = "구간 분포",
            value = "${analysis.occupiedDecadeBins} / 5구간"
        )

        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "⚠️ 이 분석결과는 6대 로직에 의한 통계적 결과 점수를 나타내는 참고용 지표입니다. 로또는 매회 완전히 독립적인 추첨이라 이 점수와 실제 당첨 확률은 무관합니다.",
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
        "고도화 종합 분석 (6대 로직 적용)",
        "완전 무작위 추첨 (일반 자동)",
        "최근 당첨 번호 이월 패턴 분석",
        "홀짝 / 고저 균형 필터링",
        "끝수 및 연속 번호 조합 제한"
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
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소", color = Color(0xFF64748B))
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White
    )
}
