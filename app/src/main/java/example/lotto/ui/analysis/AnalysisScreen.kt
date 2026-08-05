// File Path: app/src/main/java/example/lotto/ui/analysis/AnalysisScreen.kt
package com.example.lotto.ui.analysis

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

@Composable
fun AnalysisScreen(
    viewModel: AnalysisViewModel
) {
    // 기존 ViewModel에 정의된 상태 변수명들에 맞춤
    val drawNumber by viewModel.drawNumber.collectAsState()
    val drawDate by viewModel.drawDate.collectAsState()
    val lottoLists by viewModel.lottoLists.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(16.dp)
    ) {
        // 1. 상단 회차 및 날짜 표시 영역 (요청하신 "1235회 당첨번호" 형태)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "${drawNumber}회 당첨번호",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = drawDate,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF64748B)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 번호 생성 버튼 (기존 함수명 generateLottoNumbers 사용)
        Button(
            onClick = { viewModel.generateLottoNumbers() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(text = "행운의 번호 생성하기", color = Color.White, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. 로또 번호 생성 결과 카드 섹션 (위아래 스크롤 가능)
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(lottoLists) { numbers ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        numbers.forEach { number ->
                            LottoBallItem(number = number)
                        }
                    }
                }
            }
        }
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
            .size(36.dp)
            .background(color = ballColor, shape = CircleShape)
    ) {
        Text(
            text = number.toString(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}
