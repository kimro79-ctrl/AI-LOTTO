// File Path: app/src/main/java/com/example/lotto/ui/analysis/AnalysisScreen.kt
package com.example.lotto.ui.analysis

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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

@Composable
fun AnalysisScreen(
    viewModel: AnalysisViewModel = hiltViewModel()
) {
    val numbers by viewModel.numbers.collectAsState()
    val includeInput by viewModel.includeInput.collectAsState()
    val excludeInput by viewModel.excludeInput.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var selectedCountMode by remember { mutableStateOf(6) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // 4. 메인 페이지 최근 당첨번호 배너
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "최근 당첨 번호 배너",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(3, 12, 21, 30, 37, 42).forEach { num ->
                        SmallBallItem(number = num)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "스마트 패턴 분석 번호 생성",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // 1. 사용자 분석 설정 팝업 오픈 버튼
            Surface(
                onClick = { showDialog = true },
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.padding(4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "설정",
                        modifier = Modifier.size(16.dp)
                    )
                    Text(text = "조건 설정", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. 5개 설정 / 10개 설정 배너
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { selectedCountMode = 5 },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedCountMode == 5) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
                ),
                modifier = Modifier.weight(1.dp)
            ) {
                Text(text = "5개 설정 모드")
            }
            Button(
                onClick = { selectedCountMode = 10 },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedCountMode == 10) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
                ),
                modifier = Modifier.weight(1.dp)
            ) {
                Text(text = "10개 설정 모드")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "분석 조건 (${selectedCountMode}개 모드): 홀짝 비율 / 고정수 및 제외수 적용",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (numbers.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        numbers.take(6).forEach { num ->
                            LottoBallItem(number = num)
                        }
                    }
                } else {
                    Text(
                        text = "버튼을 눌러 번호를 생성하세요",
                        color = Color.Gray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { viewModel.generateSmartNumbers() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "스마트 분석 번호 추출")
        }

        if (numbers.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = { viewModel.saveNumbers() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "이 번호 저장하기")
            }
        }
    }

    // 1. 사용자 분석 설정 팝업 창
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(text = "상세 분석 조건 설정") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = "번호를 쉼표(,)로 구분해서 입력해주세요. (예: 7, 14, 28)")
                    
                    OutlinedTextField(
                        value = includeInput,
                        onValueChange = { value -> viewModel.setIncludeInput(value) },
                        label = { Text("반드시 포함할 고정수") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = excludeInput,
                        onValueChange = { value -> viewModel.setExcludeInput(value) },
                        label = { Text("제외할 번호") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showDialog = false }
                ) {
                    Text("적용하기")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDialog = false }
                ) {
                    Text("취소")
                }
            }
        )
    }
}

@Composable
fun LottoBallItem(number: Int) {
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
            .size(40.dp)
            .background(color = ballColor, shape = CircleShape)
    ) {
        Text(
            text = number.toString(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

@Composable
fun SmallBallItem(number: Int) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(26.dp)
            .background(color = MaterialTheme.colorScheme.primary, shape = CircleShape)
    ) {
        Text(
            text = number.toString(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )
    }
}
