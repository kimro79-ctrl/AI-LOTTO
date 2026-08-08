// File Path: app/src/main/java/com/kimro/ai/lotto/ui/fortune/FortuneScreen.kt
package com.kimro.ai.lotto.ui.fortune

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "타로 카드 운세",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    selectedSetCount = 5
                    viewModel.updatePendingSetCount(5)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedSetCount == 5) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
                ),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(text = "5개 조합", fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = {
                    selectedSetCount = 10
                    viewModel.updatePendingSetCount(10)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedSetCount == 10) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
                ),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(text = "10개 조합", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = { viewModel.drawTarotCards(selectedSetCount) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(text = "카드 다시 뽑기", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "아래 3장의 카드 중 오늘의 운세를 담은 한 장을 선택하세요",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 3장의 카드 - 선택한 카드만 색이 바뀌고, 다른 카드를 누르면 언제든 다시 바뀐다
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            tarotCardOptions.forEachIndexed { index, _ ->
                CardBackItem(
                    cardIndex = index + 1,
                    isSelected = selectedCardIndex == index,
                    onClick = { viewModel.selectTarotCard(index) }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (generatedTarotSets.isNotEmpty()) {
            Button(
                onClick = { viewModel.saveAllTarotNumbers() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(text = "추출된 ${generatedTarotSets.size}개 타로 조합 내역에 저장하기", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (tarotCardName != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = tarotCardName ?: "",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            // 긍정(정방향) 풀이
                            Row(verticalAlignment = Alignment.Top) {
                                Text(
                                    text = "🌞 긍정  ",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = tarotMeaningPositive ?: "",
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // 부정(역방향) 풀이
                            Row(verticalAlignment = Alignment.Top) {
                                Text(
                                    text = "🌧️ 주의  ",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFC62828),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = tarotMeaningNegative ?: "",
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                itemsIndexed(generatedTarotSets) { index, numbers ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "타로 맞춤 조합 ${index + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray,
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
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "카드를 터치하여 운세를 확인하세요",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

/**
 * 카드 뒷면 컴포넌트.
 * isSelected가 true인 카드만 골드색으로 하이라이트되고, 나머지는 기본 보라색을 유지한다.
 * 재선택이 자유롭기 때문에 클릭 제한을 걸지 않는다 - 언제든 다른 카드를 눌러 선택을 바꿀 수 있음.
 */
@Composable
fun CardBackItem(cardIndex: Int, isSelected: Boolean, onClick: () -> Unit) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFFFFD700) else Color(0xFF512DA8),
        label = "cardBackColor"
    )

    Box(
        modifier = Modifier
            .size(width = 95.dp, height = 135.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "TAROT",
                color = if (isSelected) Color(0xFF3E2723) else Color(0xFFFFD700),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "카드 $cardIndex",
                color = if (isSelected) Color(0xFF3E2723) else Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun TarotBallItem(number: Int, size: Int) {
    val ballColor = when (number) {
        in 1..10 -> Color(0xFFFBC02D)
        in 11..20 -> Color(0xFF1E88E5)
        in 21..30 -> Color(0xFFE53935)
        in 31..40 -> Color(0xFF757575)
        else -> Color(0xFF43A047)
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
