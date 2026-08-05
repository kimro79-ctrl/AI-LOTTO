// File Path: app/src/main/java/com/example/lotto/ui/fortune/FortuneScreen.kt
package com.example.lotto.ui.fortune

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import java.util.Calendar
import kotlin.random.Random

@Composable
fun FortuneScreen() {
    var birthInput by remember { mutableStateOf("") }
    var fortuneResult by remember { mutableStateOf<String?>(null) }
    var fortuneDetails by remember { mutableStateOf<String?>(null) }
    var generatedFortuneSets by remember { mutableStateOf<List<List<Int>>>(emptyList()) }
    var selectedSetCount by remember { mutableStateOf(5) } // 5개 또는 10개 조합 선택

    // 요일별 디테일 운세 풀이 데이터
    val dayOfWeekFortunes = mapOf(
        Calendar.MONDAY to "월요일: 새로운 시작의 기운이 강합니다. 직관을 믿고 도전하면 뜻밖의 재물운이 따르는 하루입니다.",
        Calendar.TUESDAY to "화요일: 에너지가 넘치는 날이지만 성급한 지출은 금물입니다. 차분하게 주변을 살피면 행운이 찾아옵니다.",
        Calendar.WEDNESDAY to "수요일: 귀인의 도움을 받을 수 있는 형국입니다. 협력이나 공동의 선택에서 좋은 결과가 기대됩니다.",
        Calendar.THURSDAY to "목요일: 변화와 변동의 기운이 큽니다. 평소에 선택하지 않던 과감한 숫자가 행운을 가져다줍니다.",
        Calendar.FRIDAY to "금요일: 주말을 앞두고 마음의 여유가 생기는 날입니다. 문서운과 재물운의 조화가 매우 좋습니다.",
        Calendar.SATURDAY to "토요일: 황금 같은 추첨의 날! 직감적으로 끌리는 숫자가 곧 당첨 번호가 될 확률이 높은 대길(大吉)의 날입니다.",
        Calendar.SUNDAY to "일요일: 휴식과 충전의 날입니다. 과거의 데이터와 패턴을 분석하여 다음 주를 준비하면 좋습니다."
    )

    fun calculateFortune() {
        if (birthInput.length < 4) return

        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        
        fortuneResult = dayOfWeekFortunes[dayOfWeek] ?: "오늘 하루 평탄하고 안정적인 기운이 가득합니다."
        fortuneDetails = "생년월일(${birthInput})의 기운과 오늘의 천문 에너지를 결합하여 분석한 맞춤형 행운 가이드입니다."

        val sets = mutableListOf<List<Int>>()
        for (i in 0 until selectedSetCount) {
            val resultSet = mutableSetOf<Int>()
            // 생년월일 기반 시드값 반영 조합 생성
            val seed = birthInput.toIntOrNull() ?: 1234
            val random = Random(seed + i + System.currentTimeMillis())

            while (resultSet.size < 6) {
                resultSet.add(random.nextInt(1, 46))
            }
            sets.add(resultSet.sorted())
        }
        generatedFortuneSets = sets
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "요일별 맞춤 행운 운세",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = birthInput,
            onValueChange = { if (it.length <= 8) birthInput = it },
            label = { Text("생년월일 8자리 (예: 19950101)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 5개 조합 / 10개 조합 선택 버튼
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { selectedSetCount = 5 },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedSetCount == 5) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
                ),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(text = "5개 조합", fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { selectedSetCount = 10 },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedSetCount == 10) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
                ),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(text = "10개 조합", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { calculateFortune() },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Text(text = "운세 및 행운 번호 추출", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 결과 리스트 영역
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (fortuneResult != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "오늘의 디테일 운세 요약",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = fortuneResult!!, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = fortuneDetails!!, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
                }

                itemsIndexed(generatedFortuneSets) { index, numbers ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "운세 추천 조합 ${index + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                numbers.forEach { num ->
                                    FortuneBallItem(number = num, size = 32)
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
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "생년월일을 입력하고 운세를 확인하세요",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FortuneBallItem(number: Int, size: Int) {
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
