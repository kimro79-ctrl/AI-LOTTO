// File Path: app/src/main/java/com/kimro/ai/lotto/ui/fortune/FortuneScreen.kt
package com.kimro.ai.lotto.ui.fortune

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FortuneScreen(
    viewModel: FortuneViewModel = hiltViewModel()
) {
    var selectedSetCount by remember { mutableStateOf(5) }
    var isShuffling by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val tarotCardName by viewModel.tarotCardName.collectAsState()
    val tarotKeyword by viewModel.tarotKeyword.collectAsState()
    val tarotMeaningPositive by viewModel.tarotMeaningPositive.collectAsState()
    val tarotMeaningNegative by viewModel.tarotMeaningNegative.collectAsState()
    val selectedCardInfo by viewModel.selectedCardInfo.collectAsState()
    val tarotCardOptions by viewModel.tarotCardOptions.collectAsState()
    val selectedCardIndex by viewModel.selectedCardIndex.collectAsState()
    val generatedTarotSets by viewModel.generatedTarotSets.collectAsState()
    val saveMessage by viewModel.saveMessage.collectAsState()
    val hasDrawnToday by viewModel.hasDrawnToday.collectAsState()
    val checkInHistory by viewModel.checkInHistory.collectAsState()

    val context = LocalContext.current
    val hasSelected = selectedCardIndex != null

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
                        text = "오늘의 타로 운세",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(
                            fontSize = 22.sp,
                            brush = Brush.horizontalGradient(colors = listOf(Color(0xFF0EA5E9), Color(0xFF7C3AED)))
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF8FAFC))
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
            item {
                FortuneCalendarSection(checkInHistory = checkInHistory)
            }

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
                        Text(text = "🔮", fontSize = 26.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "오늘 당신에게 필요한 메시지를\n카드 한 장으로 확인해보세요",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (hasDrawnToday) {
                            Surface(
                                color = Color(0xFFF3E8FF),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 14.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "✅ ", fontSize = 15.sp)
                                    Text(
                                        text = "오늘의 타로를 이미 확인했어요. 내일 다시 만나요!",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF7C3AED)
                                    )
                                }
                            }
                        } else if (isShuffling) {
                            ShufflingIndicator()
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                tarotCardOptions.forEachIndexed { index, _ ->
                                    CardBackItem(
                                        isSelected = selectedCardIndex == index,
                                        isLocked = hasSelected,
                                        onClick = { viewModel.selectTarotCard(index) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        isShuffling = true
                                        delay(650)
                                        viewModel.drawTarotCards(selectedSetCount)
                                        isShuffling = false
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
                            ) {
                                Text(
                                    text = if (hasSelected) "🔀 카드 섞고 다시 뽑기" else "🔀 카드 섞고 뽑기",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            if (hasSelected && tarotCardName != null) {
                item {
                    AnimatedVisibility(visible = true, enter = fadeIn() + expandVertically()) {
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
                                Text(text = "🃏 오늘의 카드", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = tarotCardName ?: "",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = tarotKeyword ?: "",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF7C3AED)
                                )
                            }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "⭐ 오늘의 운세 점수", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                Text(
                                    text = "${selectedCardInfo?.luckScore ?: 0}점",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF7C3AED)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(verticalAlignment = Alignment.Top) {
                                Text(text = "🌞 긍정  ", fontWeight = FontWeight.Bold, color = Color(0xFF10B981), fontSize = 13.sp)
                                Text(text = tarotMeaningPositive ?: "", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFF334155))
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(verticalAlignment = Alignment.Top) {
                                Text(text = "🌧️ 주의  ", fontWeight = FontWeight.Bold, color = Color(0xFFEF4444), fontSize = 13.sp)
                                Text(text = tarotMeaningNegative ?: "", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFF334155))
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "※ 재미를 위한 콘텐츠 점수이며 실제 확률과 무관합니다", fontSize = 9.sp, color = Color(0xFFCBD5E1))
                        }
                    }
                }

                item {
                    selectedCardInfo?.let { info ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(text = "🍀 오늘의 행운", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                Spacer(modifier = Modifier.height(12.dp))

                                StarRatingRow(label = "재물운", stars = info.wealthStars)
                                Spacer(modifier = Modifier.height(6.dp))
                                StarRatingRow(label = "애정운", stars = info.loveStars)
                                Spacer(modifier = Modifier.height(6.dp))
                                StarRatingRow(label = "대인관계", stars = info.relationshipStars)

                                Spacer(modifier = Modifier.height(14.dp))
                                HorizontalDivider(color = Color(0xFFF1F5F9))
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    LuckyInfoChip(title = "행운의 색상", value = info.luckyColor)
                                    LuckyInfoChip(title = "행운의 숫자", value = "${info.luckyNumber}")
                                    LuckyInfoChip(title = "행운의 방향", value = info.luckyDirection)
                                }
                            }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(text = "🎯 타로 행운번호", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "${tarotKeyword ?: ""}의 기운을 담아 구성한 오늘의 조합입니다", fontSize = 11.sp, color = Color(0xFF94A3B8))

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                listOf(5, 10).forEach { count ->
                                    val isSelected = selectedSetCount == count
                                    OutlinedButton(
                                        onClick = {
                                            selectedSetCount = count
                                            viewModel.updateSetCount(count)
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
                                        Text(text = "${count}개 조합", fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                generatedTarotSets.forEachIndexed { index, numbers ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${index + 1}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF94A3B8),
                                            modifier = Modifier.width(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceBetween) {
                                            numbers.forEach { num -> TarotBallItem(number = num, size = 30) }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = { viewModel.saveAllTarotNumbers() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                            ) {
                                Text(text = "🍀 행운번호 저장하기", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            } else if (!hasDrawnToday) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "카드를 선택하면 오늘의 운세가 펼쳐져요", color = Color(0xFF94A3B8), fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ShufflingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "shuffle")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(600, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "shuffleRotation"
    )

    Column(
        modifier = Modifier.padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "🔀", fontSize = 32.sp, modifier = Modifier.rotate(rotation))
        Spacer(modifier = Modifier.height(10.dp))
        Text(text = "카드를 섞고 있어요...", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7C3AED))
    }
}

@Composable
fun StarRatingRow(label: String, stars: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
        Row {
            repeat(5) { i ->
                Text(
                    text = if (i < stars) "★" else "☆",
                    fontSize = 15.sp,
                    color = if (i < stars) Color(0xFFFACC15) else Color(0xFFE2E8F0)
                )
            }
        }
    }
}

@Composable
fun LuckyInfoChip(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = title, fontSize = 10.sp, color = Color(0xFF94A3B8))
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7C3AED))
    }
}

@Composable
fun CardBackItem(isSelected: Boolean, isLocked: Boolean, onClick: () -> Unit) {
    val backgroundBrush = when {
        isSelected -> Brush.verticalGradient(listOf(Color(0xFFFACC15), Color(0xFFF59E0B)))
        isLocked -> Brush.verticalGradient(listOf(Color(0xFF7C3AED).copy(alpha = 0.3f), Color(0xFF0EA5E9).copy(alpha = 0.3f)))
        else -> Brush.verticalGradient(listOf(Color(0xFF7C3AED), Color(0xFF4C1D95)))
    }

    Box(
        modifier = Modifier
            .size(width = 95.dp, height = 140.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(backgroundBrush)
            .clickable(enabled = !isLocked) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "✦", fontSize = 14.sp, color = if (isSelected) Color(0xFF3F2D00) else Color.White.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "🔮", fontSize = 26.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "✦", fontSize = 14.sp, color = if (isSelected) Color(0xFF3F2D00) else Color.White.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "TAROT", color = if (isSelected) Color(0xFF3F2D00) else Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
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
        modifier = Modifier.size(size.dp).background(color = ballColor, shape = CircleShape)
    ) {
        Text(text = number.toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = (size * 0.38).sp)
    }
}

private val calendarDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

private data class YearMonth(val year: Int, val month: Int) {
    fun next(): YearMonth = if (month == 12) YearMonth(year + 1, 1) else YearMonth(year, month + 1)
    fun prev(): YearMonth = if (month == 1) YearMonth(year - 1, 12) else YearMonth(year, month - 1)
}

private fun currentYearMonth(): YearMonth {
    val cal = Calendar.getInstance()
    return YearMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
}

private fun dayKey(year: Int, month: Int, day: Int): String {
    val cal = Calendar.getInstance()
    cal.set(year, month - 1, day, 0, 0, 0)
    return calendarDateFormat.format(cal.time)
}

@Composable
fun FortuneCalendarSection(checkInHistory: Map<String, FortuneCheckIn>) {
    var yearMonth by remember { mutableStateOf(currentYearMonth()) }
    val todayKey = remember { calendarDateFormat.format(Date()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "📅 오늘의 운세 달력", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { yearMonth = yearMonth.prev() }, modifier = Modifier.size(28.dp)) {
                        Text(text = "‹", fontSize = 18.sp, color = Color(0xFF7C3AED), fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "${yearMonth.year}.${"%02d".format(yearMonth.month)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    IconButton(onClick = { yearMonth = yearMonth.next() }, modifier = Modifier.size(28.dp)) {
                        Text(text = "›", fontSize = 18.sp, color = Color(0xFF7C3AED), fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("일", "월", "화", "수", "목", "금", "토").forEach { d ->
                    Text(
                        text = d,
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            val cal = Calendar.getInstance()
            cal.set(yearMonth.year, yearMonth.month - 1, 1)
            val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1
            val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

            val totalCells = firstDayOfWeek + daysInMonth
            val totalRows = (totalCells + 6) / 7

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (row in 0 until totalRows) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        for (col in 0 until 7) {
                            val cellIndex = row * 7 + col
                            val day = cellIndex - firstDayOfWeek + 1
                            if (day in 1..daysInMonth) {
                                val key = dayKey(yearMonth.year, yearMonth.month, day)
                                val record = checkInHistory[key]
                                val isToday = key == todayKey
                                CalendarDayCell(day = day, record = record, isToday = isToday)
                            } else {
                                Spacer(modifier = Modifier.width(32.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(text = "🔮 카드를 뽑은 날은 운세 점수가 표시돼요", fontSize = 10.sp, color = Color(0xFF94A3B8))
        }
    }
}

@Composable
private fun CalendarDayCell(day: Int, record: FortuneCheckIn?, isToday: Boolean) {
    val hasRecord = record != null
    Column(
        modifier = Modifier
            .width(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (hasRecord) Modifier.background(Color(0xFFF3E8FF))
                else Modifier
            )
            .then(
                if (isToday) Modifier.border(1.dp, Color(0xFF7C3AED), RoundedCornerShape(8.dp))
                else Modifier
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$day",
            fontSize = 11.sp,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            color = if (hasRecord) Color(0xFF7C3AED) else Color(0xFF334155)
        )
        if (hasRecord) {
            Text(text = "${record!!.luckScore}", fontSize = 8.sp, color = Color(0xFF7C3AED), fontWeight = FontWeight.Bold)
        }
    }
}
