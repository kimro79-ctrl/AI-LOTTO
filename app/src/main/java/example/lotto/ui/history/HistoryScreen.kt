// File Path: app/src/main/java/com/example/lotto/ui/history/HistoryScreen.kt
package com.example.lotto.ui.history

import androidx.compose.foundation.background
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
import com.example.lotto.data.local.LottoEntity

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val historyList by viewModel.historyList.collectAsState()
    
    // 전체 삭제 확인 다이얼로그 상태 관리
    var showClearAllDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 상단 타이틀 및 전체 삭제 버튼 영역
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "저장된 번호 내역",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            )

            // 전체 삭제 버튼 (내역이 있을 때만 표시)[span_3](start_span)[span_3](end_span)
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
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = historyList,
                    key = { item -> item.id }
                ) { item ->
                    HistoryItem(
                        entity = item,
                        onDeleteClick = { viewModel.deleteHistory(item.id) }
                    )
                }
            }
        }
    }

    // 전체 삭제 확인 다이얼로그
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text("전체 삭제 경고") },
            text = { Text("저장된 모든 번호 내역을 영구적으로 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteAllHistory() // 전체 삭제 함수 연결 완료
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
}

@Composable
fun HistoryItem(
    entity: LottoEntity,
    onDeleteClick: () -> Unit
) {
    val typeLabel = when (entity.type) {
        "ANALYSIS" -> "스마트 분석"
        "FORTUNE" -> "운세 추천"
        else -> "QR 스캔"
    }

    val numberList = entity.numbers.split(",").mapNotNull { it.trim().toIntOrNull() }

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
                Text(
                    text = "[$typeLabel] ${entity.date}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    fontSize = 12.sp
                )
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
