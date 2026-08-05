// File Path: app/src/main/java/example/lotto/ui/analysis/AnalysisScreen.kt
package com.example.lotto.ui.analysis

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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
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
        // 번호 생성 시 카드들이 아래로 차곡차곡 쌓이며 위아래로 스크롤되는 영역
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // 1. 상단 최신 당첨 번호 배너
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

            // 4. 생성된 번호 조합 리스트 (카드 섹션 스크롤)
            if (numberSets.isNotEmpty()) {
                item {
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
                    LottoSetCard(setIndex = index + 1, numbers = set)
                }
            }
        }
    }

    // 조건 선택 다이얼로그
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

// 상단 최신 당첨 번호 배너
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
                .padding(18.dp)
        ) {
            Column {
                Text(
                    text = "최신 회차 당첨 번호",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    winNumbers.forEach { number ->
                        LottoBall(number = number, size = 38)
                    }
                }
            }
        }
    }
}

// 스마트 패턴 분석 섹션
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
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFF0EA5E9),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "스마트 패턴 분석",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            }

            Text(
                text = "최신 당첨 이월 패턴, 홀짝 균형, 고저 비율 필터를 실시간 반영하여 최적의 번호를 추출합니다.",
                fontSize = 13.sp,
                color = Color(0xFF64748B),
                lineHeight = 18.sp
            )

            // 조합 수 선택 버튼 (5개 / 10개)
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
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // 번호 생성 실행 버튼
            Button(
                onClick = onGenerateClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0EA5E9)
                )
            ) {
                Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI 추천 번호 생성하기",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

// 조건변경 배너
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
                .padding(horizontal = 18.dp, vertical = 14.dp)
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
                            .size(36.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "적용된 분석 조건",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = currentCondition,
                            fontSize = 14.sp,
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
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

// 번호 조합 세트 카드 (카드 섹션 형태 및 스크롤 지원)
@Composable
fun LottoSetCard(setIndex: Int, numbers: List<Int>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${setIndex}세트",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF64748B),
                fontSize = 14.sp
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                numbers.forEach { number ->
                    LottoBall(number = number, size = 34)
                }
            }
        }
    }
}

// 로또 공 UI 컴포넌트
@Composable
fun LottoBall(number: Int, size: Int = 36) {
    val ballColor = when (number) {
        in 1..10 -> Color(0xFFF59E0B)  // 노랑
        in 11..20 -> Color(0xFF3B82F6) // 파랑
        in 21..30 -> Color(0xFFEF4444) // 빨강
        in 31..40 -> Color(0xFF64748B) // 회색
        else -> Color(0xFF10B981)      // 초록
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
            fontSize = (size / 2.6).sp
        )
    }
}

// 조건 선택 다이얼로그
@Composable
fun ConditionSelectDialog(
    currentCondition: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val conditions = listOf(
        "고도화 종합 분석 (7대 로직 적용)",
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
