// File Path: app/src/main/java/com/kimro/ai/lotto/ui/analysis/AnalysisScreen.kt
package com.kimro.ai.lotto.ui.analysis

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    viewModel: AnalysisViewModel
) {
    val context = LocalContext.current
    val numberSets by viewModel.numberSets.collectAsState()
    val selectedCondition by viewModel.selectedCondition.collectAsState()
    val latestWinNumbers by viewModel.latestWinNumbers.collectAsState()
    val saveMessage by viewModel.saveMessage.collectAsState()

    var showConditionDialog by remember { mutableStateOf(false) }
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
            // 1. 회차 표시 배너 (1235회 당첨 번호)
            item {
                LatestWinBanner(winNumbers = latestWinNumbers)
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
                    }
                )
            }

            // 3. 조건변경 배너
            item {
                ConditionChangeBanner(
                    currentCondition = selectedCondition,
                    onClick = { showConditionDialog = true }
                )
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

                itemsIndexed(numberSets) { index, set ->
                    LottoSetCard(
                        setIndex = index + 1,
                        numbers = set,
                        latestWinNumbers = latestWinNumbers
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
}

// 회차 정보가 포함된 상단 배너 (1235회 당첨 번호)
@Composable
fun LatestWinBanner(winNumbers: List<Int>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "1235회 당첨 번호",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    winNumbers.forEach { number ->
                        LottoBall(number = number, size = 36)
                    }
                }
            }
        }
    }
}

@Composable
fun SmartPatternAnalysisSection(
    selectedCount: Int,
    onCountSelected: (Int) -> Unit,
    onGenerateClick: () -> Unit
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
                text = "최신 당첨 이월 패턴, 홀짝 균형, 고저 비율 필터를 실시간 반영합니다.",
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
                    color = Color.White.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = "변경",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
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
    latestWinNumbers: List<Int>
) {
    var expanded by remember { mutableStateOf(false) }
    val analysis = remember(numbers, latestWinNumbers) {
        analyzeLottoSet(numbers, latestWinNumbers)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
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
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "분석 접기" else "분석 펼치기",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFF1F5F9))
                Spacer(modifier = Modifier.height(12.dp))
                AnalysisReportSection(analysis)
            }
        }
    }
}

/**
 * 펼쳤을 때 보여줄 통계 분석 리포트.
 * 홀짝/고저 비율 게이지, 연속번호·끝수중복 여부, 총합, 이월수, 구간 분포, 종합 점수를 표시한다.
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
                    text = "분석 점수 ${analysis.score}점",
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

        // 이월수 / 구간 분포 정보
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InfoChip(
                modifier = Modifier.weight(1f),
                title = "이월수",
                value = if (analysis.carryOverNumbers.isEmpty()) "없음"
                        else analysis.carryOverNumbers.joinToString(", ")
            )
            InfoChip(
                modifier = Modifier.weight(1f),
                title = "구간 분포",
                value = "${analysis.occupiedDecadeBins} / 5구간"
            )
        }
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
        "고도화 종합 분석 (7대 로직 적용)",
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
