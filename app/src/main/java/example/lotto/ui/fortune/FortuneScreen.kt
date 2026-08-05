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
import kotlin.random.Random

@Composable
fun FortuneScreen() {
    var birthInput by remember { mutableStateOf("") }
    var fortuneTitle by remember { mutableStateOf<String?>(null) }
    var fortuneResult by remember { mutableStateOf<String?>(null) }
    var fortuneDetails by remember { mutableStateOf<String?>(null) }
    var generatedFortuneSets by remember { mutableStateOf<List<List<Int>>>(emptyList()) }
    var selectedSetCount by remember { mutableStateOf(5) } // 5개 또는 10개 조합 선택

    // 사주 오행 및 천간리지 풀이 데이터 풀(Pool)
    data class SajuFortune(val title: String, val summary: String, val detail: String)

    val sajuPool = listOf(
        SajuFortune(
            title = "[청룡(木)의 생동하는 사주 기운]",
            summary = "입력하신 사주에 나무(木)의 성질이 강하게 작용하여, 만물이 자라나듯 재물운과 명예운이 크게 상승하는 형국입니다.",
            detail = "막혀 있던 자금 흐름이 시원하게 풀리며, 직관적으로 떠오르는 번호에 강력한 행운의 생명력이 실립니다."
        ),
        SajuFortune(
            title = "[태양(火)의 뻗어나가는 사주 기운]",
            summary = "불(火)의 에너지가 충만하여 활동 범위가 넓어지고 주변의 이목과 행운을 한 몸에 받는 대길(大吉)의 사주입니다.",
            detail = "과감하고 화려한 숫자 조합에서 대박의 행운이 터져 나올 확률이 매우 높은 하루입니다."
        ),
        SajuFortune(
            title = "[대지(土)의 든든한 재물 사주 기운]",
            summary = "흙(土)의 안정된 기운이 재물을 단단하게 갈무리해주어 문서운과 횡재수가 묵직하게 따르는 사주입니다.",
            detail = "중후하고 균형 잡힌 번호 배치 속에서 안정적인 당첨의 기쁨을 맞이할 수 있는 에너지가 깃들어 있습니다."
        ),
        SajuFortune(
            title = "[황금(金)의 예리한 결실 사주 기운]",
            summary = "단단한 쇠(金)의 예리함이 날카로운 직관력을 만들어내어 숨겨진 행운의 번호를 정확히 포착하는 사주입니다.",
            detail = "규칙적이거나 간결한 패턴 속에서 뜻밖의 큰 재물이 들어오는 형상을 띠고 있습니다."
        ),
        SajuFortune(
            title = "[흐르는 물(水)의 지혜로운 사주 기운]",
            summary = "깊은 물(水)의 유연하고 지혜로운 기운이 유연한 대처와 함께 의외의 행운을 강력하게 끌어당기는 사주입니다.",
            detail = "전체적으로 유순하고 골고루 퍼진 숫자 선택이 최고의 결과를 안겨다 줍니다."
        )
    )

    fun calculateSajuFortune() {
        if (birthInput.length < 4) return

        // 입력한 생년월일을 숫자로 변환하여 시드값으로 활용 (고유한 사주 결과 도출)
        val birthInt = birthInput.toIntOrNull() ?: 1234
        val random = Random(birthInt + System.currentTimeMillis() % 1000)
        
        val selectedSaju = sajuPool[random.nextInt(sajuPool.size)]

        fortuneTitle = selectedSaju.title
        fortuneResult = selectedSaju.summary
        fortuneDetails = selectedSaju.detail

        val sets = mutableListOf<List<Int>>()
        for (i in 0 until selectedSetCount) {
            val resultSet = mutableSetOf<Int>()
            // 생년월일과 조합 인덱스를 결합한 난수 생성
            val setRandom = Random(birthInt + i * 79 + System.currentTimeMillis())

            while (resultSet.size < 6) {
                resultSet.add(setRandom.nextInt(1, 46))
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
            text = "생년월일 사주 운세",
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
            onClick = { calculateSajuFortune() },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Text(text = "사주 풀이 및 행운 번호 추출", fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
                                text = fortuneTitle ?: "",
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
                                text = "사주 맞춤 조합 ${index + 1}",
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
                            text = "생년월일을 입력하고 사주 풀이를 확인하세요",
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
