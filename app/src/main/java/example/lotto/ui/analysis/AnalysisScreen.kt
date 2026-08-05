// File Path: app/src/main/java/example/lotto/ui/analysis/AnalysisScreen.kt
package com.example.lotto.ui.analysis

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun AnalysisScreen(
    viewModel: AnalysisViewModel
) {
    val numberSets by viewModel.numberSets.collectAsState()
    val selectedCondition by viewModel.selectedCondition.collectAsState()
    val latestWinNumbers by viewModel.latestWinNumbers.collectAsState()
    val saveMessage by viewModel.saveMessage.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var showToast by remember { mutableStateOf(false) }
    var toastText by remember { mutableStateOf("") }

    // 저장 메시지 발생 시 짧게 팝업 띄우기
    LaunchedEffect(saveMessage) {
        if (!saveMessage.isNullOrEmpty()) {
            toastText = saveMessage!!
            showToast = true
            delay(1200) // 노출 시간 짧게 단축
            showToast = false
            viewModel.clearSaveMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 최근 당첨 번호 섹션
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "최근 제 1235회 당첨 번호",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        latestWinNumbers.forEach { num ->
                            AnalysisBallItem(number = num, size = 34)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 조건 선택 및 헤더 영역
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "스마트 패턴 분석 번호",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "적용 조건: $selectedCondition",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                Button(
                    onClick = { showDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "조건 변경", color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 5개 / 10개 조합 추출 버튼
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.generateSmartNumbers(5) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "5개 조합", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { viewModel.generateSmartNumbers(10) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text(text = "10개 조합", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 생성된 번호 리스트
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (numberSets.isNotEmpty()) {
                    itemsIndexed(numberSets) { index, numbers ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "추천 조합 ${index + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    numbers.forEach { num ->
                                        AnalysisBallItem(number = num, size = 32)
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
                                .height(180.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "위의 조합 버튼을 눌러 번호를 생성하세요",
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            // 번호 저장 버튼
            if (numberSets.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.saveNumbers() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(text = "이 번호 세트 저장하기", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 요청하신 7대 분석 카테고리가 모두 포함된 팝업 다이얼로그
        if (showDialog) {
            val conditions = listOf(
                "미출현 번호 적용",
                "홀짝 비율 적용 (3:3)",
                "고저 비율 적용 (저:고 = 3:3)",
                "연속번호 제외",
                "끝수 분석 분산",
                "이월수 적용 (직전번호 포함)",
                "최근 번호 제외"
            )

            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text(text = "분석 조건 선택", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "원하는 패턴 조건을 선택해주세요:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        conditions.forEach { condition ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.setCondition(condition)
                                        showDialog = false
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (selectedCondition == condition),
                                    onClick = {
                                        viewModel.setCondition(condition)
                                        showDialog = false
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = condition, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text(text = "닫기", fontWeight = FontWeight.Bold)
                    }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }

        // 저장 완료 팝업 (스낵바 형태)
        if (showToast) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 70.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xDD323232)),
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Text(
                        text = toastText,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun AnalysisBallItem(number: Int, size: Int) {
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
