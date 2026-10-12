// File Path: app/src/main/java/com/kimro/ai/lotto/ui/trend/TrendScreen.kt
package com.kimro.ai.lotto.ui.trend

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kimro.ai.lotto.ui.analysis.HistoricalDraw
import com.kimro.ai.lotto.ui.analysis.HotColdDialog
import com.kimro.ai.lotto.ui.analysis.NumberFrequency
import com.kimro.ai.lotto.ui.analysis.computeNumberFrequencies
import com.kimro.ai.lotto.ui.analysis.fetchHistoricalDraws
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * "최신경향" 탭.
 *
 * 구성: 최신 회차 요약 → 다빈도/콜드 TOP 5(번호당 평균 대비 편차) → 최근 10회 홀짝·고저 →
 * 구간별 출현 편차(기대값 대비) → 장기 미출현 번호.
 * 다빈도/콜드 "더보기"는 기존 HotColdDialog를 그대로 재사용하고, 나머지는 전체 회차 데이터
 * (fetchHistoricalDraws, 캐시됨)에서 이 화면이 직접 계산한다.
 */
@Composable
fun TrendScreen() {
    var allDraws by remember { mutableStateOf<List<HistoricalDraw>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showHotColdDialog by remember { mutableStateOf(false) }
    var showSectionDetailDialog by remember { mutableStateOf(false) }
    var showOddEvenDetailDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun load() {
        isLoading = true
        errorMessage = null
        scope.launch {
            try {
                allDraws = fetchHistoricalDraws()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
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
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "최신경향",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                val headerRound = allDraws?.maxOfOrNull { it.drawNo }
                if (headerRound != null) {
                    Surface(color = Color(0xFFF3E8FF), shape = RoundedCornerShape(12.dp)) {
                        Text(
                            text = "${headerRound}회까지 반영",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF7C3AED),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))

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
                    val latest = remember(draws) { draws.maxByOrNull { it.drawNo } }
                    val frequencies = remember(draws) { computeNumberFrequencies(draws) }
                    val hotTop5 = remember(frequencies) { frequencies.sortedByDescending { it.count }.take(5) }
                    val coldTop5 = remember(frequencies) { frequencies.sortedBy { it.count }.take(5) }
                    // 번호 1개당 평균 출현 횟수 (전체 출현 횟수 / 45). 다빈도/콜드의 편차 기준.
                    val avgCount = remember(frequencies) { frequencies.sumOf { it.count } / 45f }

                    // 최근 10회 홀짝/고저 비율
                    val recent10 = remember(draws) { draws.sortedByDescending { it.drawNo }.take(10) }
                    val totalNumbers = recent10.size * 6
                    val oddCount = remember(recent10) {
                        recent10.sumOf { d -> d.numbers.count { it % 2 != 0 } }
                    }
                    val lowCount = remember(recent10) {
                        recent10.sumOf { d -> d.numbers.count { it in 1..22 } }
                    }
                    val oddPercent = if (totalNumbers == 0) 0 else oddCount * 100 / totalNumbers
                    val lowPercent = if (totalNumbers == 0) 0 else lowCount * 100 / totalNumbers

                    // 구간별 출현 편차 (전체 회차 기준). 구간은 모두 번호 9개라 기대값은 전체의 20%.
                    val sectionLabels = listOf("1-9", "10-18", "19-27", "28-36", "37-45")
                    val sectionRanges = listOf(1..9, 10..18, 19..27, 28..36, 37..45)
                    val sectionCounts = remember(draws) {
                        sectionRanges.map { range -> draws.sumOf { d -> d.numbers.count { it in range } } }
                    }
                    val totalSectionNumbers = sectionCounts.sum()
                    val sectionExpected = totalSectionNumbers / 5f
                    val sectionDeviations = sectionCounts.map { (it - sectionExpected).roundToInt() }
                    val maxAbsDeviation = (sectionDeviations.maxOfOrNull { abs(it) } ?: 0).coerceAtLeast(1)

                    // 장기 미출현 번호: 각 번호가 마지막으로 나온 회차 (보너스 번호는 제외)
                    val latestRound = latest?.drawNo ?: 0
                    val lastSeen = remember(draws) {
                        val arr = IntArray(46)
                        draws.sortedBy { it.drawNo }.forEach { d ->
                            d.numbers.forEach { if (it in 1..45) arr[it] = d.drawNo }
                        }
                        arr
                    }
                    val longAbsent = remember(lastSeen, latestRound) {
                        (1..45)
                            .filter { lastSeen[it] > 0 }
                            .map { it to (latestRound - lastSeen[it]) }
                            .sortedByDescending { it.second }
                            .take(3)
                    }

                    // 1) 최신 회차 요약
                    if (latest != null) {
                        LatestRoundCard(draw = latest)
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // 2) 다빈도 / 콜드 TOP 5
                    TrendCard(title = "다빈도 · 콜드 TOP 5", dotColor = Color(0xFFEF4444)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            FreqColumn(
                                modifier = Modifier.weight(1f),
                                title = "다빈도",
                                items = hotTop5,
                                avg = avgCount,
                                devColor = Color(0xFFEF4444)
                            )
                            FreqColumn(
                                modifier = Modifier.weight(1f),
                                title = "콜드",
                                items = coldTop5,
                                avg = avgCount,
                                devColor = Color(0xFF3B82F6)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "번호당 평균 ${avgCount.roundToInt()}회 기준 편차",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                            Text(
                                text = "더보기 (TOP 10) ›",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF7C3AED),
                                modifier = Modifier
                                    .clickable { showHotColdDialog = true }
                                    .padding(horizontal = 4.dp, vertical = 6.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    // 3) 최근 10회 홀짝 · 고저
                    TrendCard(title = "최근 10회 홀짝 · 고저", dotColor = Color(0xFF64748B)) {
                        RatioBar(
                            leftLabel = "홀 $oddPercent%",
                            rightLabel = "짝 ${100 - oddPercent}%",
                            percent = oddPercent,
                            leftColor = Color(0xFFEF4444),
                            rightColor = Color(0xFF3B82F6),
                            subLabel = "홀 ${oddCount}개 · 짝 ${totalNumbers - oddCount}개"
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        RatioBar(
                            leftLabel = "저 $lowPercent%",
                            rightLabel = "고 ${100 - lowPercent}%",
                            percent = lowPercent,
                            leftColor = Color(0xFF10B981),
                            rightColor = Color(0xFFF59E0B),
                            subLabel = "저 ${lowCount}개 · 고 ${totalNumbers - lowCount}개"
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Text(
                                text = "회차별 상세 보기 ›",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF7C3AED),
                                maxLines = 1,
                                modifier = Modifier
                                    .clickable { showOddEvenDetailDialog = true }
                                    .padding(horizontal = 4.dp, vertical = 8.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    // 4) 구간별 출현 편차
                    TrendCard(
                        title = "구간별 출현 편차",
                        dotColor = Color(0xFF3B82F6),
                        trailing = "전체 회차"
                    ) {
                        Text(
                            text = "가운데 선이 기대값이에요 (구간당 약 ${sectionExpected.roundToInt()}회, 20%)",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            sectionDeviations.forEachIndexed { index, deviation ->
                                DeviationBar(
                                    label = sectionLabels[index],
                                    deviation = deviation,
                                    ratio = abs(deviation).toFloat() / maxAbsDeviation
                                )
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Text(
                                text = "번호별 상세 보기 ›",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF7C3AED),
                                maxLines = 1,
                                modifier = Modifier
                                    .clickable { showSectionDetailDialog = true }
                                    .padding(horizontal = 4.dp, vertical = 8.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    // 5) 장기 미출현 번호
                    if (longAbsent.isNotEmpty()) {
                        TrendCard(title = "장기 미출현 번호", dotColor = Color(0xFF8B5CF6)) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                longAbsent.forEach { (number, gap) ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        TrendBall(number = number, size = 28.dp, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "마지막 출현 ${lastSeen[number]}회",
                                            fontSize = 13.sp,
                                            color = Color(0xFF475569),
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "${gap}회째",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "보너스 번호는 제외하고 센 기록이에요.",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Text(
                        text = "매 회차는 독립 추첨이에요. 참고용 통계예요.",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                }
            }
        }
    }

    if (showHotColdDialog) {
        HotColdDialog(onDismiss = { showHotColdDialog = false })
    }

    if (showSectionDetailDialog && allDraws != null) {
        SectionDetailDialog(
            draws = allDraws!!,
            onDismiss = { showSectionDetailDialog = false }
        )
    }

    if (showOddEvenDetailDialog && allDraws != null) {
        OddEvenDetailDialog(
            recentDraws = allDraws!!.sortedByDescending { it.drawNo }.take(10),
            onDismiss = { showOddEvenDetailDialog = false }
        )
    }
}

/** 로또 공 색 (저장 내역 화면과 같은 1-10 노랑 / 11-20 파랑 / 21-30 빨강 / 31-40 보라 / 41-45 초록). */
private fun trendBallColor(number: Int): Color = when (number) {
    in 1..10 -> Color(0xFFFBC02D)
    in 11..20 -> Color(0xFF1E88E5)
    in 21..30 -> Color(0xFFE53935)
    in 31..40 -> Color(0xFF8E24AA)
    else -> Color(0xFF43A047)
}

private fun signed(value: Int): String = if (value > 0) "+$value" else "$value"

@Composable
private fun TrendBall(number: Int, size: Dp = 28.dp, fontSize: androidx.compose.ui.unit.TextUnit = 13.sp) {
    Box(
        modifier = Modifier.size(size).background(trendBallColor(number), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "$number", fontSize = fontSize, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
private fun TrendCard(
    title: String,
    dotColor: Color,
    trailing: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(0.5.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(dotColor, CircleShape))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    modifier = Modifier.weight(1f)
                )
                if (trailing != null) {
                    Text(text = trailing, fontSize = 11.sp, color = Color(0xFF94A3B8))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

/** 가장 최근 회차의 당첨번호 + 합계/홀짝/저고 요약. */
@Composable
private fun LatestRoundCard(draw: HistoricalDraw) {
    val sorted = draw.numbers.sorted()
    val sum = sorted.sum()
    val odd = sorted.count { it % 2 != 0 }
    val low = sorted.count { it in 1..22 }

    TrendCard(
        title = "${draw.drawNo}회 당첨번호",
        dotColor = Color(0xFFF59E0B),
        trailing = draw.date.replace("-", ".")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            sorted.forEach { number -> TrendBall(number = number, size = 34.dp, fontSize = 15.sp) }
            if (draw.bonusNo in 1..45) {
                Text("+", fontSize = 14.sp, color = Color(0xFF94A3B8))
                TrendBall(number = draw.bonusNo, size = 34.dp, fontSize = 15.sp)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryChip(modifier = Modifier.weight(1f), label = "합계", value = "$sum")
            SummaryChip(modifier = Modifier.weight(1f), label = "홀 : 짝", value = "$odd : ${6 - odd}")
            SummaryChip(modifier = Modifier.weight(1f), label = "저 : 고", value = "$low : ${6 - low}")
        }
    }
}

@Composable
private fun SummaryChip(modifier: Modifier = Modifier, label: String, value: String) {
    Surface(color = Color(0xFFF8FAFC), shape = RoundedCornerShape(8.dp), modifier = modifier) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 11.sp, color = Color(0xFF64748B))
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
        }
    }
}

/** 다빈도/콜드 한 줄 목록: 공 + 출현 횟수 + 번호당 평균 대비 편차. */
@Composable
private fun FreqColumn(
    modifier: Modifier = Modifier,
    title: String,
    items: List<NumberFrequency>,
    avg: Float,
    devColor: Color
) {
    Column(modifier = modifier) {
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = devColor)
        Spacer(modifier = Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items.forEach { item ->
                val deviation = (item.count - avg).roundToInt()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TrendBall(number = item.number, size = 28.dp, fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("${item.count}회", fontSize = 13.sp, color = Color(0xFF0F172A))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(signed(deviation), fontSize = 12.sp, color = devColor)
                }
            }
        }
    }
}

/** 두 색으로 나뉜 비율 막대. 가운데(50%)에 흰 눈금을 넣어 한쪽으로 얼마나 치우쳤는지 보이게 한다. */
@Composable
private fun RatioBar(
    leftLabel: String,
    rightLabel: String,
    percent: Int,
    leftColor: Color,
    rightColor: Color,
    subLabel: String? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(leftLabel, fontSize = 13.sp, color = Color(0xFF334155))
            Text(rightLabel, fontSize = 13.sp, color = Color(0xFF334155))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(percent.coerceIn(0, 100).toFloat().coerceAtLeast(0.001f))
                        .fillMaxHeight()
                        .background(leftColor)
                )
                Box(
                    modifier = Modifier
                        .weight((100 - percent).coerceIn(0, 100).toFloat().coerceAtLeast(0.001f))
                        .fillMaxHeight()
                        .background(rightColor)
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(Color.White)
            )
        }
        if (subLabel != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(subLabel, fontSize = 11.sp, color = Color(0xFF94A3B8))
        }
    }
}

/** 기대값을 가운데 선으로 두고, 많이 나온 구간은 오른쪽(파랑), 적게 나온 구간은 왼쪽(회색)으로 뻗는 막대. */
@Composable
private fun DeviationBar(label: String, deviation: Int, ratio: Float) {
    val positiveColor = Color(0xFF3B82F6)
    val negativeColor = Color(0xFF94A3B8)
    val safeRatio = ratio.coerceIn(0f, 1f)

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, fontSize = 12.sp, color = Color(0xFF64748B), modifier = Modifier.width(44.dp))
        Row(
            modifier = Modifier
                .weight(1f)
                .height(10.dp)
                .background(Color(0xFFF1F5F9), RoundedCornerShape(5.dp))
        ) {
            Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.CenterEnd) {
                if (deviation < 0 && safeRatio > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(safeRatio)
                            .fillMaxHeight()
                            .background(
                                negativeColor,
                                RoundedCornerShape(topStart = 5.dp, bottomStart = 5.dp)
                            )
                    )
                }
            }
            Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color(0xFFCBD5E1)))
            Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.CenterStart) {
                if (deviation > 0 && safeRatio > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(safeRatio)
                            .fillMaxHeight()
                            .background(
                                positiveColor,
                                RoundedCornerShape(topEnd = 5.dp, bottomEnd = 5.dp)
                            )
                    )
                }
            }
        }
        Text(
            text = "${signed(deviation)}회",
            fontSize = 12.sp,
            fontWeight = if (deviation > 0) FontWeight.Bold else FontWeight.Normal,
            color = if (deviation > 0) positiveColor else Color(0xFF64748B),
            modifier = Modifier.width(52.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

@Composable
private fun OddEvenDetailDialog(recentDraws: List<HistoricalDraw>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("최근 10회 홀짝 · 고저 상세", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "회차별로 직접 세어보실 수 있게 번호를 그대로 나열했어요.",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                recentDraws.sortedByDescending { it.drawNo }.forEach { draw ->
                    val sorted = draw.numbers.sorted()
                    val drawOdd = sorted.count { it % 2 != 0 }
                    val drawLow = sorted.count { it in 1..22 }

                    Text(
                        text = "${draw.drawNo}회" + if (draw.date.isNotBlank()) " · ${draw.date}" else "",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 6.dp)) {
                        sorted.forEach { number ->
                            val isOdd = number % 2 != 0
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .background(
                                        if (isOdd) Color(0xFFEF4444) else Color(0xFF0EA5E9),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("$number", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                    Text(
                        text = "홀 ${drawOdd}개 · 짝 ${6 - drawOdd}개   저 ${drawLow}개 · 고 ${6 - drawLow}개",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(bottom = 14.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("닫기", color = Color(0xFF7C3AED)) }
        }
    )
}

@Composable
private fun SectionDetailDialog(draws: List<HistoricalDraw>, onDismiss: () -> Unit) {
    val frequencies = remember(draws) { computeNumberFrequencies(draws) }
    val countByNumber = remember(frequencies) { frequencies.associate { it.number to it.count } }

    val sections = listOf(
        Triple("1-9", 1..9, Color(0xFFF59E0B)),
        Triple("10-18", 10..18, Color(0xFF3B82F6)),
        Triple("19-27", 19..27, Color(0xFFEF4444)),
        Triple("28-36", 28..36, Color(0xFF64748B)),
        Triple("37-45", 37..45, Color(0xFF10B981))
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("구간별 출현 분포 상세", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "${draws.size}회차 전체 기준, 번호 하나하나의 출현 횟수예요.",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                sections.forEach { (label, range, color) ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(color, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("$label 구간", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                    }
                    range.chunked(5).forEach { rowNumbers ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        ) {
                            rowNumbers.forEach { number ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier.size(30.dp).background(color, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("$number", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${countByNumber[number] ?: 0}회",
                                        fontSize = 9.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("닫기", color = Color(0xFF7C3AED)) }
        }
    )
}
