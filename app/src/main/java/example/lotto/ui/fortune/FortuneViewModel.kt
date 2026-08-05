// File Path: app/src/main/java/com/example/lotto/ui/fortune/FortuneViewModel.kt
package com.example.lotto.ui.fortune

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lotto.data.repository.LottoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

data class SajuFortune(val title: String, val summary: String, val detail: String)

@HiltViewModel
class FortuneViewModel @Inject constructor(
    private val repository: LottoRepository
) : ViewModel() {

    private val _fortuneTitle = MutableStateFlow<String?>(null)
    val fortuneTitle: StateFlow<String?> = _fortuneTitle.asStateFlow()

    private val _fortuneResult = MutableStateFlow<String?>(null)
    val fortuneResult: StateFlow<String?> = _fortuneResult.asStateFlow()

    private val _fortuneDetails = MutableStateFlow<String?>(null)
    val fortuneDetails: StateFlow<String?> = _fortuneDetails.asStateFlow()

    private val _generatedFortuneSets = MutableStateFlow<List<List<Int>>>(emptyList())
    val generatedFortuneSets: StateFlow<List<List<Int>>> = _generatedFortuneSets.asStateFlow()

    private val _saveMessage = MutableStateFlow<String?>(null)
    val saveMessage: StateFlow<String?> = _saveMessage.asStateFlow()

    private val sajuPool = listOf(
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

    fun calculateSajuFortune(birthInput: String, setCount: Int) {
        if (birthInput.length < 4) return

        val birthInt = birthInput.toIntOrNull() ?: 1234
        val random = Random(birthInt + System.currentTimeMillis() % 1000)
        
        val selectedSaju = sajuPool[random.nextInt(sajuPool.size)]

        _fortuneTitle.value = selectedSaju.title
        _fortuneResult.value = selectedSaju.summary
        _fortuneDetails.value = selectedSaju.detail

        val sets = mutableListOf<List<Int>>()
        for (i in 0 until setCount) {
            val resultSet = mutableSetOf<Int>()
            val setRandom = Random(birthInt + i * 79 + System.currentTimeMillis())

            while (resultSet.size < 6) {
                resultSet.add(setRandom.nextInt(1, 46))
            }
            sets.add(resultSet.sorted())
        }
        _generatedFortuneSets.value = sets
    }

    fun saveAllFortuneNumbers() {
        val currentSets = _generatedFortuneSets.value
        if (currentSets.isNotEmpty()) {
            viewModelScope.launch {
                currentSets.forEach { numbers ->
                    repository.insertLotto(numbers, "FORTUNE")
                }
                _saveMessage.value = "성공적으로 ${currentSets.size}개의 운세 조합이 내역에 저장되었습니다!"
            }
        }
    }

    fun clearSaveMessage() {
        _saveMessage.value = null
    }
}
