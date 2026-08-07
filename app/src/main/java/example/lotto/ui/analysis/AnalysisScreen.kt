package com.kimro.ai.lotto.ui.analysis

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AnalysisScreen() {
    var selectedTab by remember { mutableStateOf("고도화 종합 분석") }
    var numbers by remember { mutableStateOf(listOf(7, 14, 22, 31, 38, 42)) }
    var isAnimating by remember { mutableStateOf(false) }

    // 그라데이션 브러시 정의 (세련된 블루-퍼플 계열)
    val cardGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6), Color(0xFF3B82F6))
    )

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFF8FAFC), Color(0xFFEEF2FF))
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundGradient)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 상단 타이틀
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "AI 스마트 로또 분석",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "최신 통계 알고리즘 기반 적중률 극대화",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    
                    IconButton(
                        onClick = { /* 설정 또는 새로고침 */ },
                        modifier = Modifier
                            .background(Color.White, CircleShape)
                            .shadow(4.dp, CircleShape)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "AI", tint = Color(0xFF6366F1))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // [핵심] 그라데이션 및 입체 그림자가 적용된 메인 결과 카드
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 12.dp, shape = RoundedCornerShape(24.dp))
                        .clip(RoundedCornerShape(24.dp))
                        .background(cardGradient)
                        .padding(24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "🔥 1235회 추천 조합",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                            
                            Text(
                                text = "신뢰도 98.2%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFE0E7FF)
                            )
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // 로또 번호 공 리스트
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            numbers.forEach { number ->
                                LottoBall(number = number)
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // [핵심] 구체적인 AI 분석 근거 배지 (툴팁 형태)
                        Surface(
                            color = Color.Black.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "최근 20회차 출현 빈도 & 홀짝 비율(4:2) 최적화 적용",
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 분석 조건 선택 버튼 (디자인 개선)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(16.dp))
                        .clickable { /* 조건 선택 팝업 오픈 */ },
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "적용된 분석 로직",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = selectedTab,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                        }
                        Text(
                            text = "변경하기 >",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6366F1)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // 번호 생성 버튼 (그라데이션 입체 버튼)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(8.dp, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .background(cardGradient)
                        .clickable {
                            // 번호 갱신 시뮬레이션
                            numbers = (1..45).shuffled().take(6).sorted()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI 행운 번호 조합 생성",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// 로또 공 디자인 컴포넌트 (색상별 번호 스타일링)
@Composable
fun LottoBall(number: Int) {
    val ballColor = when (number) {
        in 1..10 -> Color(0xFFFACC15)   // 노란색
        in 11..20 -> Color(0xFF38BDF8)  // 파란색
        in 21..30 -> Color(0xFFF87171)  // 빨간색
        in 31..40 -> Color(0xFF94A3B8)  // 회색
        else -> Color(0xFF4ADE80)       // 초록색
    }

    Box(
        modifier = Modifier
            .size(44.dp)
            .shadow(4.dp, CircleShape)
            .background(ballColor, CircleShape)
            .border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number.toString(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}
