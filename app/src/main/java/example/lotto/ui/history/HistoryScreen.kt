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
import androidx.compose.foundation.layout.width
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
    val savedLottoList by viewModel.savedLottoList.collectAsState(initial = emptyList())
    // 다이얼로그 상태 관리
    var showDeleteDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<LottoEntity?>(null) }
    var showClearAllDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // --- 상단 타이틀 및 전체 삭제 버튼 영역 수정됨 ---
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
                fontSize = 22.sp // 타이틀 텍스트 크기 축소 (기본값보다 작게)
            )

            // 전체 삭제 버튼 (내역이 있을 때만 표시)
            if (savedLottoList.isNotEmpty()) {
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
        // -----------------------------------------------

        if (savedLottoList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "저장된 번호 내역이 없습니다.",
                    color = Color.Gray,
                    fontSize = 16.sp
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(savedLottoList) { item ->
                    LottoHistoryItem(
                        item = item,
                        onDeleteClick = {
                            itemToDelete = item
                            showDeleteDialog = true
                        }
                    )
                }
            }
        }
    }

    // 개별 삭제 확인 다이얼로그
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("삭제 확인") },
            text = { Text("선택한 번호 내역을 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(onClick = {
                    itemToDelete?.let { viewModel.deleteLotto(it) }
                    showDeleteDialog = false
                    itemToDelete = null
                }) {
                    Text("삭제")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("취소")
                }
            }
        )
    }

    // 전체 삭제 확인 다이얼로그
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text("전체 삭제 경고") },
            text = { Text("저장된 모든 번호 내역을 영구적으로 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteAllLotto()
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
fun LottoHistoryItem(
    item: LottoEntity,
    onDeleteClick: () -> Unit
) {
    // 쉼표로 구분된 번호 문자열 생성
    val numbers = listOf(item.num1, item.num2, item.num3, item.num4, item.num5, item.num6)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0EAF5)) // 연한 보라색 배경 유지
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // --- 항목 정보 텍스트 크기 축소 ---
                Column {
                    Text(
                        text = if (item.type == "QR") "[QR 스캔]" else "[운세 추출]",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontSize = 12.sp, // 유형 텍스트 크기 축소
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.date,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.DarkGray,
                        fontSize = 13.sp // 날짜 텍스트 크기 축소
                    )
                }
                // -----------------------------------

                // 삭제 버튼
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

            Spacer(modifier = Modifier.height(10.dp))

            // 로또 번호 공
 표시
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                numbers.forEach { num ->
                    LottoBall(number = num)
                }
            }
        }
    }
}

@Composable
fun LottoBall(number: Int) {
    val backgroundColor = when (number) {
        in 1..10 -> Color(0xFFFFC107) // 노란색
        in 11..20 -> Color(0xFF2196F3) // 파란색
        in 21..30 -> Color(0xFFF44336) // 빨간색
        in 31..40 -> Color(0xFF9E9E9E) // 회색
        else -> Color(0xFF4CAF50) // 초록색 (41~45)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(36.dp)
            .background(color = backgroundColor, shape = CircleShape)
    ) {
        Text(
            text = number.toString(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
    }
}
