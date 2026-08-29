// File Path: app/src/main/java/com/kimro/ai/lotto/ui/history/HistoryScreen.kt
package com.kimro.ai.lotto.ui.history

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kimro.ai.lotto.data.local.LottoEntity
import com.kimro.ai.lotto.ui.analysis.analyzeLottoSet
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// 필터 옵션. "저장 시 type 문자열"을 하나 이상 매핑해서 여러 타입을 하나로 묶을 수 있게 했다.
// (예: 타로 화면은 "TAROT", 예전 코드는 "FORTUNE"으로 저장한 이력이 섞여있어도 "운세"로 함께 묶임)
private enum class HistoryFilter(val label: String, val matchTypes: Set<String>?) {
    ALL("전체", null),
    ANALYSIS("스마트 분석", setOf("ANALYSIS")),
    FORTUNE("운세", setOf("FORTUNE", "TAROT")),
    QR("QR 스캔", setOf("QR"))
}

private enum class HistorySort(val label: String) {
    LATEST("최신순"),
    SCORE("균형도 점수순")
}

/** entity.date는 "yyyy-MM-dd HH:mm" 형식이라, 앞 10글자만 잘라내면 날짜별 그룹 키가 된다. */
private fun dateKeyOf(entity: LottoEntity): String = entity.date.take(10)

/** "2026-08-19" 같은 키를 "오늘 · 8월 19일" / "어제 · 8월 18일" / "2026년 8월 17일" 형태로 보기 좋게 바꾼다. */
private fun formatDateGroupLabel(dateKey: String): String {
    val parts = dateKey.split("-")
    if (parts.size != 3) return dateKey
    val (year, month, day) = parts

    val today = Calendar.getInstance()
    val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(today.time)

    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -1) }
    val yesterdayKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(yesterday.time)

    val monthDay = "${month.toInt()}월 ${day.toInt()}일"
    return when (dateKey) {
        todayKey -> "오늘 · $monthDay"
        yesterdayKey -> "어제 · $monthDay"
        else -> "${year}년 $monthDay"
    }
}

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val historyList by viewModel.historyList.collectAsState()

    var showClearAllDialog by remember { mutableStateOf(false) }
    // 삭제 확인을 기다리는 날짜 그룹 키. null이 아니면 "이 날짜 삭제할까요?" 팝업이 뜬다.
    var dateGroupPendingDelete by remember { mutableStateOf<String?>(null) }
    var selectedFilter by remember { mutableStateOf(HistoryFilter.ALL) }
    var selectedSort by remember { mutableStateOf(HistorySort.LATEST) }

    // 필터링 + 정렬된 최종 리스트 계산
    val displayedList = remember(historyList, selectedFilter, selectedSort) {
        val filtered = when (selectedFilter) {
            HistoryFilter.ALL -> historyList
            else -> historyList.filter { it.type in (selectedFilter.matchTypes ?: emptySet()) }
        }
        when (selectedSort) {
            HistorySort.LATEST -> filtered // DB에서 이미 최신순(id desc)으로 내려옴
            HistorySort.SCORE -> filtered.sortedByDescending { entity ->
                val numbers = entity.numbers.split(",").mapNotNull { it.trim().toIntOrNull() }
                if (numbers.size == 6) analyzeLottoSet(numbers).score else -1
            }
        }
    }

    // 날짜별로 묶는다. 날짜 그룹 자체는 항상 최신 날짜가 위로 오도록 정렬하고,
    // 그룹 안의 순서는 위에서 이미 계산한 displayedList(최신순 또는 점수순)의 순서를 그대로 따른다.
    val groupedByDate = remember(displayedList) {
        displayedList
            .groupBy { dateKeyOf(it) }
            .toList()
            .sortedByDescending { (dateKey, _) -> dateKey }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 상단 타이틀 및 전체 삭제 버튼 영역
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "저장된 번호 내역",
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                style = androidx.compose.ui.text.TextStyle(
                    fontSize = 22.sp,
                    brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                        colors = listOf(Color(0xFF0EA5E9), Color(0xFF7C3AED))
                    )
                )
            )

            if (historyList.isNotEmpty()) {
                IconButton(
                    onClick = { showClearAllDialog = true },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "전체 삭제",
                        tint = Color.Gray
                    )
                }
            }
        }

        if (historyList.isNotEmpty()) {
            // 타입별 필터 칩
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                items(HistoryFilter.values().toList()) { filter ->
                    val isSelected = selectedFilter == filter
                    Surface(
                        color = if (isSelected) Color(0xFF7C3AED) else Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.clickable { selectedFilter = filter }
                    ) {
                        Text(
                            text = filter.label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else Color(0xFF64748B),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            // 정렬 옵션 칩
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                HistorySort.values().forEach { sort ->
                    val isSelected = selectedSort == sort
                    Surface(
                        color = if (isSelected) Color(0xFFE0F2FE) else Color.Transparent,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.clickable { selectedSort = sort }
                    ) {
                        Text(
                            text = if (isSelected) "✓ ${sort.label}" else sort.label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color(0xFF0284C7) else Color(0xFF94A3B8),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        if (historyList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "저장된 내역이 없습니다.",
                    color = Color.Gray,
                    fontSize = 16.sp
                )
            }
        } else if (displayedList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "'${selectedFilter.label}' 항목이 없습니다.",
                    color = Color.Gray,
                    fontSize = 15.sp
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                groupedByDate.forEach { (dateKey, itemsForDate) ->
                    item(key = "header_$dateKey") {
                        DateGroupHeader(
                            dateKey = dateKey,
                            count = itemsForDate.size,
                            onDeleteGroupClick = { dateGroupPendingDelete = dateKey }
                        )
                    }
                    items(
                        items = itemsForDate,
                        key = { item -> item.id }
                    ) { item ->
                        HistoryItem(
                            entity = item,
                            onDeleteClick = { viewModel.deleteHistory(item.id) }
                        )
                    }
                    item(key = "spacer_$dateKey") {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }

    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text("전체 삭제 경고") },
            text = { Text("저장된 모든 번호 내역을 영구적으로 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteAllHistory()
                    showClearAllDialog = false
                }) {
                    Text("전체 삭제", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("취소")
                }
            }
        )
    }

    // 날짜 그룹 삭제 확인 팝업
    val groupKeyToDelete = dateGroupPendingDelete
    if (groupKeyToDelete != null) {
        val groupLabel = formatDateGroupLabel(groupKeyToDelete)
        val groupCount = groupedByDate.firstOrNull { it.first == groupKeyToDelete }?.second?.size ?: 0
        AlertDialog(
            onDismissRequest = { dateGroupPendingDelete = null },
            title = { Text("날짜별 삭제") },
            text = { Text("'$groupLabel'에 저장된 ${groupCount}개 조합을 모두 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.") },
            confirmButton = {
                TextButton(onClick = {
                    historyList
                        .filter { dateKeyOf(it) == groupKeyToDelete }
                        .forEach { entity -> viewModel.deleteHistory(entity.id) }
                    dateGroupPendingDelete = null
                }) {
                    Text("삭제", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { dateGroupPendingDelete = null }) {
                    Text("취소")
                }
            }
        )
    }
}

/** 날짜 그룹 상단에 붙는 헤더. "오늘 · 8월 19일 (2)" 형태로 보여주고, 옆에 이 날짜만 삭제하는 버튼이 있다. */
@Composable
fun DateGroupHeader(dateKey: String, count: Int, onDeleteGroupClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = formatDateGroupLabel(dateKey),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color(0xFF334155)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "${count}개",
                fontSize = 12.sp,
                color = Color(0xFF94A3B8)
            )
        }
        IconButton(
            onClick = onDeleteGroupClick,
            modifier = Modifier.size(26.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "이 날짜 전체 삭제",
                tint = Color(0xFFEF4444).copy(alpha = 0.65f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun HistoryItem(
    entity: LottoEntity,
    onDeleteClick: () -> Unit
) {
    val typeLabel = if (entity.conditionLabel.isNotBlank()) {
        entity.conditionLabel
    } else {
        when (entity.type) {
            "ANALYSIS" -> "스마트 분석"
            "FORTUNE", "TAROT" -> "운세 추천"
            "QR" -> "QR 스캔"
            else -> "기타"
        }
    }

    val numberList = entity.numbers.split(",").mapNotNull { it.trim().toIntOrNull() }

    // QR로 스캔해서 실제 회차가 확인된 경우에만 회차 뱃지를 보여준다 (round=0은 회차 미상)
    val hasRound = entity.round > 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0EAF5))
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "[$typeLabel] ${entity.date}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                    if (hasRound) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = Color(0xFF7C3AED).copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${entity.round}회차",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF7C3AED),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "삭제",
                        tint = Color.LightGray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                numberList.forEach { num ->
                    HistoryBallItem(number = num)
                }
            }
        }
    }
}

@Composable
fun HistoryBallItem(number: Int) {
    val ballColor = when (number) {
        in 1..10 -> Color(0xFFFBC02D)
        in 11..20 -> Color(0xFF1E88E5)
        in 21..30 -> Color(0xFFE53935)
        in 31..40 -> Color(0xFF8E24AA)
        else -> Color(0xFF43A047)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(36.dp)
            .background(color = ballColor, shape = CircleShape)
    ) {
        Text(
            text = number.toString(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
    }
}
