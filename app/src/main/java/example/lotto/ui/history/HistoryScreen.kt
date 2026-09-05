// File Path: app/src/main/java/com/kimro/ai/lotto/ui/history/HistoryScreen.kt
package com.kimro.ai.lotto.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.kimro.ai.lotto.data.local.LottoEntity
import com.kimro.ai.lotto.ui.analysis.HistoricalDraw
import com.kimro.ai.lotto.ui.analysis.analyzeLottoSet
import com.kimro.ai.lotto.ui.analysis.fetchHistoricalDraws
import kotlinx.coroutines.launch
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

// "과거 당첨 이력 보기" 전용 등수 라벨. AnalysisScreen.kt의 BacktestDialog와 같은 등수 체계를 쓰지만,
// 그쪽 상수(BACKTEST_RANK_LABELS)가 private이라 재사용하지 않고 이 파일 안에 독립적으로 둔다
// (다른 화면 파일을 건드리지 않기 위한 안전한 선택).
private val HISTORY_BACKTEST_RANK_LABELS = listOf("1등 (6개 일치)", "2등 (5개+보너스)", "3등 (5개 일치)", "4등 (4개 일치)", "5등 (3개 일치)")

/**
 * 저장된 번호 조합이, 실제로 존재했던 모든 과거 회차와 비교했을 때 몇 등이 몇 번 나왔는지 계산한다.
 * 등수별 횟수뿐 아니라, 실제로 어느 회차였는지도 같이 담아서 사용자가 펼쳐볼 수 있게 한다.
 * rankCounts index: 0=1등, 1=2등, 2=3등, 3=4등, 4=5등, 5=낙첨.
 * matchedDrawsByRank index 0~4는 각 등수에 해당하는 실제 회차 목록(최신 회차가 먼저 오도록 정렬).
 */
private data class HistoryBacktestResult(
    val rankCounts: IntArray,
    val matchedDrawsByRank: List<List<HistoricalDraw>>
)

private fun computeHistoryBacktest(userNumbers: List<Int>, draws: List<HistoricalDraw>): HistoryBacktestResult {
    val userMarked = BooleanArray(46)
    userNumbers.forEach { if (it in 1..45) userMarked[it] = true }

    val rankCounts = IntArray(6)
    val matchedDrawsByRank = List(5) { mutableListOf<HistoricalDraw>() }

    draws.sortedByDescending { it.drawNo }.forEach { draw ->
        val matches = draw.numbers.count { it in 1..45 && userMarked[it] }
        val bonusMatched = draw.bonusNo in 1..45 && userMarked[draw.bonusNo]
        val rankIndex = when {
            matches == 6 -> 0
            matches == 5 && bonusMatched -> 1
            matches == 5 -> 2
            matches == 4 -> 3
            matches == 3 -> 4
            else -> 5
        }
        rankCounts[rankIndex]++
        if (rankIndex < 5) matchedDrawsByRank[rankIndex].add(draw)
    }
    return HistoryBacktestResult(rankCounts, matchedDrawsByRank)
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

    // 여러 개를 체크박스로 골라서 한 번에 공유하기 위한 "선택 모드" 상태.
    // 선택 모드를 끄면 선택 내역도 함께 초기화한다.
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    var showSelectedDeleteConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current

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
            .background(Color(0xFFF1F5F9))
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
                text = if (isSelectionMode) "${selectedIds.size}개 선택됨" else "저장된 번호 내역",
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                style = androidx.compose.ui.text.TextStyle(
                    fontSize = 22.sp,
                    brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                        colors = listOf(Color(0xFF0EA5E9), Color(0xFF7C3AED))
                    )
                )
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isSelectionMode) {
                    TextButton(onClick = {
                        isSelectionMode = false
                        selectedIds = emptySet()
                    }) {
                        Text("취소", color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                    }
                } else if (historyList.isNotEmpty()) {
                    TextButton(onClick = { isSelectionMode = true }) {
                        Text("선택", color = Color(0xFF7C3AED), fontWeight = FontWeight.Bold)
                    }
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
        }

        // 선택 모드일 때 화면 사용법을 짧게 안내한다 - "선택이 뭘 하는건지 모르겠다"는 혼란을 줄이기 위함.
        if (isSelectionMode) {
            Text(
                text = "삭제하거나 공유하고 싶은 조합을 탭해서 선택하세요",
                fontSize = 12.sp,
                color = Color(0xFF64748B),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // 선택 모드에서 1개 이상 골랐을 때만 뜨는 "공유" / "삭제" 액션 바.
        if (isSelectionMode && selectedIds.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF0D9488))
                        .clickable {
                            val selectedEntities = historyList.filter { it.id in selectedIds }
                            shareLottoNumbers(context, buildMultiShareText(selectedEntities))
                        }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "공유 (${selectedIds.size})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFEF4444))
                        .clickable { showSelectedDeleteConfirm = true }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "삭제 (${selectedIds.size})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (historyList.isNotEmpty() && !isSelectionMode) {
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
                            onDeleteClick = { viewModel.deleteHistory(item.id) },
                            isSelectionMode = isSelectionMode,
                            isSelected = item.id in selectedIds,
                            onToggleSelect = {
                                selectedIds = if (item.id in selectedIds) {
                                    selectedIds - item.id
                                } else {
                                    selectedIds + item.id
                                }
                            }
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

    // 선택 모드에서 고른 항목들을 한 번에 삭제하기 전 확인 팝업
    if (showSelectedDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showSelectedDeleteConfirm = false },
            title = { Text("선택 삭제") },
            text = { Text("선택한 ${selectedIds.size}개 조합을 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.") },
            confirmButton = {
                TextButton(onClick = {
                    selectedIds.forEach { id -> viewModel.deleteHistory(id) }
                    selectedIds = emptySet()
                    isSelectionMode = false
                    showSelectedDeleteConfirm = false
                }) {
                    Text("삭제", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSelectedDeleteConfirm = false }) {
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
            // 다른 화면(분석/운세)의 원형 그라데이션 아이콘 배지와 톤을 맞췄다.
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(
                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                            listOf(Color(0xFF0EA5E9), Color(0xFF7C3AED))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🗓", fontSize = 11.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
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

/** 저장된 항목의 표시용 조건 라벨을 구한다. HistoryItem과 묶음 공유 양쪽에서 같이 쓴다. */
private fun resolveTypeLabel(entity: LottoEntity): String {
    return if (entity.conditionLabel.isNotBlank()) {
        entity.conditionLabel
    } else {
        when (entity.type) {
            "ANALYSIS" -> "스마트 분석"
            "FORTUNE", "TAROT" -> "운세 추천"
            "QR" -> "QR 스캔"
            else -> "기타"
        }
    }
}

/**
 * 저장된 조합을 카카오톡/문자 등으로 공유할 때 쓸 텍스트를 만든다.
 * 표준 공유 시트(Intent.ACTION_SEND)를 그대로 활용하므로, 사용자가 목록에서
 * 카카오톡·문자·기타 앱 중 원하는 것을 골라 보낼 수 있다.
 */
private fun buildShareText(typeLabel: String, numberList: List<Int>, round: Int): String {
    val numbersText = numberList.sorted().joinToString(", ")
    val roundText = if (round > 0) " (${round}회차)" else ""
    return "🎫 AI로또 6/45 - $typeLabel$roundText\n$numbersText"
}

/** 선택 모드에서 여러 개를 골랐을 때, 각 조합을 구분선으로 나눠 하나의 텍스트로 묶는다. */
private fun buildMultiShareText(entities: List<LottoEntity>): String {
    return entities.joinToString(separator = "\n\n") { entity ->
        val typeLabel = resolveTypeLabel(entity)
        val numberList = entity.numbers.split(",").mapNotNull { it.trim().toIntOrNull() }
        buildShareText(typeLabel, numberList, entity.round)
    }
}

private fun shareLottoNumbers(context: android.content.Context, shareText: String) {
    val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_TEXT, shareText)
    }
    context.startActivity(android.content.Intent.createChooser(sendIntent, "번호 공유하기"))
}

@Composable
fun HistoryItem(
    entity: LottoEntity,
    onDeleteClick: () -> Unit,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {}
) {
    val context = LocalContext.current
    val typeLabel = resolveTypeLabel(entity)

    val numberList = entity.numbers.split(",").mapNotNull { it.trim().toIntOrNull() }

    // QR로 스캔해서 실제 회차가 확인된 경우에만 회차 뱃지를 보여준다 (round=0은 회차 미상)
    val hasRound = entity.round > 0

    // "과거 당첨 이력 보기" 팝업 상태 - 항목마다 독립적으로 관리한다.
    var showBacktestDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var isLoadingBacktest by remember { mutableStateOf(false) }
    var backtestError by remember { mutableStateOf<String?>(null) }
    var backtestResult by remember { mutableStateOf<HistoryBacktestResult?>(null) }
    var backtestTotalDraws by remember { mutableStateOf(0) }
    // 어떤 등수 행이 펼쳐져 있는지 (한 번에 하나만 펼치도록 인덱스 하나만 저장)
    var expandedRankIndex by remember { mutableStateOf<Int?>(null) }
    // 펼친 등수 안에서 "전체 보기"를 눌렀는지 여부. 등수를 바꿔 펼칠 때마다 초기화된다.
    var showAllInExpandedRank by remember { mutableStateOf(false) }

    fun runBacktest() {
        if (numberList.size != 6) return
        isLoadingBacktest = true
        backtestError = null
        coroutineScope.launch {
            try {
                val draws = fetchHistoricalDraws()
                backtestTotalDraws = draws.size
                backtestResult = computeHistoryBacktest(numberList, draws)
            } catch (e: Exception) {
                backtestError = "데이터를 불러오지 못했습니다. 네트워크 상태를 확인 후 다시 시도해주세요."
            } finally {
                isLoadingBacktest = false
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isSelectionMode) Modifier.clickable { onToggleSelect() } else Modifier
            )
            .then(
                if (isSelectionMode && isSelected) {
                    Modifier.border(2.dp, Color(0xFF7C3AED), RoundedCornerShape(16.dp))
                } else {
                    Modifier
                }
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 라벨 줄에 weight(1f)를 줘서, 조건 문구가 길어도 아이콘 영역을 밀어내지 않고
                // 말줄임표(...)로 잘리도록 했다. (예전에 번호 볼이 잘리던 문제와 같은 원인이라 같은 방식으로 고침)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "[$typeLabel] ${entity.date}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
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
                if (isSelectionMode) {
                    // 선택 모드에서는 개별 공유/삭제 대신 체크박스만 보여줘서 화면을 단순하게 유지한다.
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelect() },
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF7C3AED))
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // 배경이 있는 pill 버튼 대신, 삭제 아이콘과 같은 톤(아이콘만)으로 맞춰서
                        // 튀어 보이지 않게 했다. 색만 청록색으로 구분해서 삭제(회색)와 헷갈리지 않게 함.
                        IconButton(
                            onClick = {
                                shareLottoNumbers(context, buildShareText(typeLabel, numberList, entity.round))
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "공유하기",
                                tint = Color(0xFF0D9488),
                                modifier = Modifier.size(18.dp)
                            )
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

            if (numberList.size == 6 && !isSelectionMode) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        showBacktestDialog = true
                        if (backtestResult == null && !isLoadingBacktest) runBacktest()
                    },
                    modifier = Modifier.fillMaxWidth().height(38.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "📜 과거 당첨 이력 보기",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF7C3AED)
                    )
                }
            }
        }
    }

    if (showBacktestDialog) {
        Dialog(onDismissRequest = { showBacktestDialog = false }) {
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
                            text = "과거 당첨 이력",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        IconButton(onClick = { showBacktestDialog = false }, modifier = Modifier.size(26.dp)) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "닫기", tint = Color(0xFF94A3B8))
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        numberList.forEach { number -> HistoryBallItem(number = number) }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "이 번호로 1회부터 지금까지 실제로 있었던 모든 회차와 비교하면?",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    when {
                        isLoadingBacktest -> {
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
                        backtestError != null -> {
                            Text(backtestError ?: "", fontSize = 12.sp, color = Color(0xFFEF4444), lineHeight = 16.sp)
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
                            val result = backtestResult!!
                            Surface(color = Color(0xFFF3E8FF), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "총 ${"%,d".format(backtestTotalDraws)}개 회차 데이터 기준",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF7C3AED),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                HISTORY_BACKTEST_RANK_LABELS.forEachIndexed { index, label ->
                                    val count = result.rankCounts[index]
                                    val isExpanded = expandedRankIndex == index
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                                            .then(
                                                if (count > 0) Modifier.clickable {
                                                    expandedRankIndex = if (isExpanded) null else index
                                                    showAllInExpandedRank = false
                                                } else Modifier
                                            )
                                            .padding(horizontal = 12.dp, vertical = 10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = if (count > 0) "${count}회" else "0회",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (count > 0) Color(0xFF10B981) else Color(0xFF94A3B8)
                                                )
                                                if (count > 0) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = if (isExpanded) "▲" else "▼",
                                                        fontSize = 10.sp,
                                                        color = Color(0xFF94A3B8)
                                                    )
                                                }
                                            }
                                        }

                                        if (isExpanded) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            HorizontalDivider(color = Color(0xFFE2E8F0))
                                            Spacer(modifier = Modifier.height(8.dp))
                                            val fullList = result.matchedDrawsByRank[index]
                                            val visibleList = if (showAllInExpandedRank) fullList else fullList.take(5)
                                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                visibleList.forEach { draw ->
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(
                                                            text = "${draw.drawNo}회",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = Color(0xFF475569)
                                                        )
                                                        if (draw.date.isNotBlank()) {
                                                            Text(
                                                                text = draw.date,
                                                                fontSize = 11.sp,
                                                                color = Color(0xFF94A3B8)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            if (fullList.size > 5) {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = if (showAllInExpandedRank) "최근 5회만 보기 ▲" else "전체 ${fullList.size}회 보기 ›",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF7C3AED),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable { showAllInExpandedRank = !showAllInExpandedRank }
                                                        .padding(vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("낙첨", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                    Text("${result.rankCounts[5]}회", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "탭하면 최근 회차부터 볼 수 있어요",
                                fontSize = 10.sp,
                                color = Color(0xFFB18CF5)
                            )

                            Spacer(modifier = Modifier.height(10.dp))
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
