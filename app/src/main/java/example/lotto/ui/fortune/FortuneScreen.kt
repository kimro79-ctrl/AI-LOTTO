// File Path: app/src/main/java/com/kimro/ai/lotto/ui/fortune/FortuneScreen.kt
package com.kimro.ai.lotto.ui.fortune

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FortuneScreen(
    viewModel: FortuneViewModel = hiltViewModel()
) {
    var selectedSetCount by remember { mutableStateOf(5) }

    val tarotCardName by viewModel.tarotCardName.collectAsState()
    val tarotMeaningPositive by viewModel.tarotMeaningPositive.collectAsState()
    val tarotMeaningNegative by viewModel.tarotMeaningNegative.collectAsState()
    val tarotCardOptions by viewModel.tarotCardOptions.collectAsState()
    val selectedCardIndex by viewModel.selectedCardIndex.collectAsState()
    val generatedTarotSets by viewModel.generatedTarotSets.collectAsState()
    val saveMessage by viewModel.saveMessage.collectAsState()

    val context = LocalContext.current

    // 화면에 처음 들어왔을 때 카드 3장을 미리 뽑아둔다
    LaunchedEffect(Unit) {
        viewModel.drawTarotCards(selectedSetCount)
    }

    LaunchedEffect(saveMessage) {
        saveMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearSaveMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "타로 카드 운세",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(
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
            // 카드 뽑기 설정 카드 (메인 "스마트 패턴 분석" 카드와 동일한 톤)
            item {
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🔮", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "오늘의 타로 운세",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }

                        Text(
                            text = "카드를 뽑고 마음에 드는 한 장을 선택하면, 그 카드의 기운을 반영한 번호 조합을 함께 받아볼 수 있어요.",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            lineHeight = 16.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            listOf(5, 10).forEach { count ->
                                val isSelected = selectedSetCount == count
                                OutlinedButton(
                                    onClick = {
                                        selectedSetCount = count
                                        viewModel.updatePendingSetCount(count)
                                    },
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
                            onClick = { viewModel.drawTarotCards(selectedSetCount) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
                        ) {
                            Text(
                                text = "🔄 카드 다시 뽑기",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // 카드 3장 선택 카드
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "아래 3장의 카드 중 오늘의 운세를 담은 한 장을 선택하세요",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // 3장의 카드 - 한 장을 선택하면 그 카드만 색이 바뀌고, 나머지는 잠겨서 더 이상 눌리지 않는다.
                        // "카드 다시 뽑기"를 눌러야 다시 선택할 수 있다.
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            tarotCardOptions.forEachIndexed { index, _ ->
                                CardBackItem(
                                    cardIndex = index + 1,
                                    isSelected = selectedCardIndex == index,
                                    isLocked = selectedCardIndex != null,
                                    onClick = { viewModel.selectTarotCard(index) }
                                )
                            }
                        }
                    }
                }
            }

            // 저장 버튼
            if (generatedTarotSets.isNotEmpty()) {
                item {
                    Button(
                        onClick = { viewModel.saveAllTarotNumbers() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text(
                            text = "추출된 ${generatedTarotSets.size}개 타로 조합 내역에 저장하기",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // 선택한 카드 풀이 + 번호 조합 결과
            if (tarotCardName != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = tarotCardName ?: "",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            // 긍정(정방향) 풀이
                            Row(verticalAlignment = Alignment.Top) {
                                Text(
                                    text = "🌞 긍정  ",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981),
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = tarotMeaningPositive ?: "",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF334155)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // 부정(역방향) 풀이
                            Row(verticalAlignment = Alignment.Top) {
                                Text(
                                    text = "🌧️ 주의  ",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEF4444),
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = tarotMeaningNegative ?: "",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF334155)
                                )
                            }
                        }
                    }
                }

                itemsIndexed(generatedTarotSets) { index, numbers ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "타로 맞춤 조합 ${index + 1}",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                numbers.forEach { num ->
                                    TarotBallItem(number = num, size = 32)
                                }
                            }
                        }
                    }
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "카드를 터치하여 운세를 확인하세요",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * 카드 뒷면 컴포넌트.
 * isSelected가 true인 카드만 골드색으로 하이라이트된다.
 * isLocked가 true(이미 한 장을 선택한 상태)이면 클릭이 아예 먹히지 않는다.
 * "카드 다시 뽑기"를 눌러 drawTarotCards()가 호출되면 selectedCardIndex가 null로
 * 초기화되면서 잠금도 함께 풀린다.
 */
@Composable
fun CardBackItem(cardIndex: Int, isSelected: Boolean, isLocked: Boolean, onClick: () -> Unit) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isSelected -> Color(0xFFFACC15)
            isLocked -> Color(0xFF7C3AED).copy(alpha = 0.35f) // 선택되지 않은 채 잠긴 카드는 살짝 흐리게
            else -> Color(0xFF7C3AED)
        },
        label = "cardBackColor"
    )

    Box(
        modifier = Modifier
            .size(width = 95.dp, height = 135.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(backgroundColor)
            .clickable(enabled = !isLocked) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "🔮",
                fontSize = 22.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "TAROT",
                color = if (isSelected) Color(0xFF3F2D00) else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "카드 $cardIndex",
                color = if (isSelected) Color(0xFF3F2D00) else Color.White.copy(alpha = 0.85f),
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun TarotBallItem(number: Int, size: Int) {
    val ballColor = when (number) {
        in 1..10 -> Color(0xFFF59E0B)
        in 11..20 -> Color(0xFF3B82F6)
        in 21..30 -> Color(0xFFEF4444)
        in 31..40 -> Color(0xFF64748B)
        else -> Color(0xFF10B981)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size.dp)
            .background(color = ballColor, shape = CircleShape)
    ) {
        Text(
            text = number.toString(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size * 0.38).sp
        )
    }
}
