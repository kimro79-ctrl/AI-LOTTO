// File Path: app/src/main/java/com/example/lotto/ui/analysis/AnalysisScreen.kt
package com.example.lotto.ui.analysis

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lotto.ui.main.AnalysisViewModel // 필요시 ViewModel 임포트

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    viewModel: AnalysisViewModel? = null // ViewModel 매개변수 추가로 에러 방지
) {
    var showConditionDialog by remember { mutableStateOf(false) }
    var showLogicInfoDialog by remember { mutableStateOf(false) }
    var selectedCondition by remember { mutableStateOf("고도화 종합 분석 (7대 로직 적용)") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "AI 스마트 로또 분석",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                },
                actions = {
                    IconButton(onClick = { showLogicInfoDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "7대 로직 설명",
                            tint = Color(0xFF0EA5E9)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF8FAFC)
                )
            )
        },
        containerColor = Color(0xFFF1F5F9)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0EA5E9)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "최근 당첨 번호",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "여기에 최근 회차 번호가 표시됩니다",
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }
            }

            OutlinedButton(
                onClick = { showConditionDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "분석 조건: $selectedCondition",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
            }
        }
    }

    if (showConditionDialog) {
        AlertDialog(
            onDismissRequest = { showConditionDialog = false },
            title = {
                Text(text = "분석 조건 선택", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ConditionOptionItem(
                        title = "고도화 종합 분석 (7대 로직 적용)",
                        isSelected = selectedCondition.contains("7대 로직")
                    ) {
                        selectedCondition = "고도화 종합 분석 (7대 로직 적용)"
                        showConditionDialog = false
                    }
                    
                    ConditionOptionItem(
                        title = "완전 무작위 추첨 (일반 자동)",
                        isSelected = selectedCondition.contains("무작위 추첨")
                    ) {
                        selectedCondition = "완전 무작위 추첨 (일반 자동)"
                        showConditionDialog = false
                    }

                    ConditionOptionItem(
                        title = "최근 당첨 번호 이월 패턴 분석",
                        isSelected = selectedCondition.contains("이월 패턴")
                    ) {
                        selectedCondition = "최근 당첨 번호 이월 패턴 분석"
                        showConditionDialog = false
                    }

                    ConditionOptionItem(
                        title = "홀짝 / 고저 균형 필터링",
                        isSelected = selectedCondition.contains("홀짝")
                    ) {
                        selectedCondition = "홀짝 / 고저 균형 필터링"
                        showConditionDialog = false
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showConditionDialog = false }) {
                    Text(text = "취소", color = Color.Gray)
                }
            }
        )
    }

    if (showLogicInfoDialog) {
        AlertDialog(
            onDismissRequest = { showLogicInfoDialog = false },
            title = {
                Text(text = "🧠 로또 7대 로직 시스템 안내", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "본 앱은 아래의 통계적 7대 로직을 적용하여 최적의 번호를 추출합니다.", fontSize = 13.sp, color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "1. 연속 숫자 분석: 과도한 연번 편중 방지", fontSize = 12.sp)
                    Text(text = "2. 끝자리 합 분포: 일의 자리 수 균형 조절", fontSize = 12.sp)
                    Text(text = "3. 과거 당첨 번호 중복 제한: 확률 낮은 중복 필터링", fontSize = 12.sp)
                    Text(text = "4. 미출현 횟수 분석: 콜드 넘버 출현 주기 계산", fontSize = 12.sp)
                    Text(text = "5. 재등장 간격 분석: 번호별 순환 패턴 파악", fontSize = 12.sp)
                    Text(text = "6. 합계 흐름 연속 패턴: 6개 번호 총합 구간 유지", fontSize = 12.sp)
                    Text(text = "7. 고저/홀짝 균형: 1~22 및 홀짝 비율 최적 대칭", fontSize = 12.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showLogicInfoDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9))
                ) {
                    Text(text = "확인", color = Color.White)
                }
            }
        )
    }
}

@Composable
fun ConditionOptionItem(title: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (isSelected) Color(0xFFE0F2FE) else Color.Transparent,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
            if (isSelected) {
                Text(text = "✓", color = Color(0xFF0EA5E9), fontWeight = FontWeight.Bold)
            }
        }
    }
}
