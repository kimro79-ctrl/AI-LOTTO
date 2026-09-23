// File Path: app/src/main/java/com/kimro/ai/lotto/ui/trend/TrendScreen.kt
package com.kimro.ai.lotto.ui.trend

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kimro.ai.lotto.ui.analysis.HistoricalDraw
import com.kimro.ai.lotto.ui.analysis.HotColdDialog
import com.kimro.ai.lotto.ui.analysis.NumberFrequency
import com.kimro.ai.lotto.ui.analysis.computeNumberFrequencies
import com.kimro.ai.lotto.ui.analysis.fetchHistoricalDraws
import kotlinx.coroutines.launch

/**
 * "최신경향" 탭. 다빈도/콜드넘버는 기존 HotColdDialog·computeNumberFrequencies를 그대로 재사용하고
 * (TOP5만 미리보기, "더보기"로 기존 팝업의 TOP10 전체를 그대로 연다), 홀짝/고저 추이와 구간별
 * 출현 분포만 이 화면에서 새로 계산한다. 전체 회차 데이터(fetchHistoricalDraws)는 캐시돼 있어
 * 다른 화면(백테스트 등)에서 이미 받아둔 경우 재요청 없이 바로 재사용된다.
 */
@Composable
fun TrendScreen() {
    var allDraws by remember { mutableStateOf<List<HistoricalDraw>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showHotColdDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun load() {
        isLoading = true
        errorMessage = null
        scope.launch {
            try {
                allDraws = fetchHistoricalDraws()
            } catch (e: Exception) {
                errorMessage = "데이터를 불러오지 못했어요. 다시 시도해주세요."
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) { load() }

    Scaffold(containerColor = Color(0xFFF1F5F9)) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "경향 리포트",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                val latestRound = allDraws?.maxOfOrNull { it.drawNo }
                if (latestRound != null) {
                    Surface(color = Color(0xFFF3E8FF), shape = RoundedCornerShape(8.dp)) {
                        Text(
                            text = "${latestRound}회 기준",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF7C3AED),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            Text(
                text = "매 회차는 독립 추첨이에요. 참고용 통계예요.",
                fontSize = 11.sp,
                color = Color(0xFF94A3B8),
                modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
            )

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF7C3AED))
                    }
                }
                errorMessage != null -> {
                    Text(errorMessage ?: "", fontSize = 13.sp, color = Color(0xFFEF4444))
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { load() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
                    ) {
                        Text("다시 시도", color = Color.White)
                    }
                }
                allDraws != null -> {
                    val draws = allDraws!!
                    val frequencies = remember(draws) { computeNumberFrequencies(draws) }
                    val hotTop5 = remember(frequencies) { frequencies.sortedByDescending { it.count }.take(5) }
                    val coldTop5 = remember(frequencies) { frequencies.sortedBy { it.count }.take(5) }

                    // 최근 10회 홀짝/고저 비율
                    val recent10 = remember(draws) { draws.sortedByDescending { it.drawNo }.take(10) }
                    val totalNumbers = recent10.size * 6
                    val oddPercent = remember(recent10) {
                        if (totalNumbers == 0) 0
                        else recent10.sumOf { d -> d.numbers.count { it % 2 != 0 } } * 100 / totalNumbers
                    }
                    val lowPercent = remember(recent10) {
                        if (totalNumbers == 0) 0
                        else recent10.sumOf { d -> d.numbers.count { it in 1..22 } } * 100 / totalNumbers
                    }

                    // 구간별 출현 분포 (전체 회차 기준)
                    val sectionLabels = listOf("1-9", "10-18", "19-27", "28-36", "37-45")
                    val sectionRanges = listOf(1..9, 10..18, 19..27, 28..36, 37..45)
                    val sectionCounts = remember(draws) {
                        sectionRanges.map { range -> draws.sumOf { d -> d.numbers.count { it in range } } }
                    }
                    val maxSectionCount = (sectionCounts.maxOrNull() ?: 0).coerceAtLeast(1)

                    TrendCard(emoji = "🔥", title = "전체 회차 다빈도 TOP 5") {
                        MiniBallRow(items = hotTop5, ballColor = Color(0xFFEF4444))
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    TrendCard(emoji = "❄️", title = "콜드넘버 TOP 5") {
                        MiniBallRow(items = coldTop5, ballColor = Color(0xFF3B82F6))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "더보기 (TOP 10) ›",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF7C3AED),
                            modifier = Modifier
                                .clickable { showHotColdDialog = true }
                                .padding(4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))

                    TrendCard(emoji = "⚖\uFE0F", title = "최근 10회 홀짝 · 고저") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            RatioBar(
                                modifier = Modifier.weight(1f),
                                leftLabel = "홀 $oddPercent%",
                                rightLabel = "짝 ${100 - oddPercent}%",
                                percent = oddPercent
                            )
                            RatioBar(
                                modifier = Modifier.weight(1f),
                                leftLabel = "저 $lowPercent%",
                                rightLabel = "고 ${100 - lowPercent}%",
                                percent = lowPercent
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    TrendCard(emoji = "📊", title = "구간별 출현 분포 (전체 회차)") {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            sectionCounts.forEachIndexed { index, count ->
                                SectionBar(
                                    label = sectionLabels[index],
                                    ratio = count.toFloat() / maxSectionCount
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    if (showHotColdDialog) {
        HotColdDialog(onDismiss = { showHotColdDialog = false })
    }
}

@Composable
private fun TrendCard(emoji: String, title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(color = Color.White, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, fontSize = 15.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
            }
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun MiniBallRow(items: List<NumberFrequency>, ballColor: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { item ->
            Box(
                modifier = Modifier.size(30.dp).background(ballColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "${item.number}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
private fun RatioBar(modifier: Modifier = Modifier, leftLabel: String, rightLabel: String, percent: Int) {
    Column(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(leftLabel, fontSize = 11.sp, color = Color(0xFF64748B))
            Text(rightLabel, fontSize = 11.sp, color = Color(0xFF64748B))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(Color(0xFFF1F5F9), RoundedCornerShape(3.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = (percent / 100f).coerceIn(0f, 1f))
                    .height(6.dp)
                    .background(Color(0xFF7C3AED), RoundedCornerShape(3.dp))
            )
        }
    }
}

@Composable
private fun SectionBar(label: String, ratio: Float) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, fontSize = 10.sp, color = Color(0xFF94A3B8), modifier = Modifier.width(40.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(7.dp)
                .background(Color(0xFFF1F5F9), RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = ratio.coerceIn(0f, 1f))
                    .height(7.dp)
                    .background(Color(0xFF7C3AED), RoundedCornerShape(4.dp))
            )
        }
    }
}

