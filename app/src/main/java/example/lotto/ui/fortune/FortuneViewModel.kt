// File Path: app/src/main/java/com/kimro/ai/lotto/ui/fortune/FortuneViewModel.kt
package com.kimro.ai.lotto.ui.fortune

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kimro.ai.lotto.data.repository.LottoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

// 카드 이름 + 긍정(정방향) / 부정(역방향) 풀이를 함께 담는 데이터 클래스
data class TarotCardInfo(
    val name: String,
    val positiveMeaning: String,
    val negativeMeaning: String
)

@HiltViewModel
class FortuneViewModel @Inject constructor(
    private val repository: LottoRepository
) : ViewModel() {

    private val _tarotCardName = MutableStateFlow<String?>(null)
    val tarotCardName: StateFlow<String?> = _tarotCardName.asStateFlow()

    private val _tarotMeaningPositive = MutableStateFlow<String?>(null)
    val tarotMeaningPositive: StateFlow<String?> = _tarotMeaningPositive.asStateFlow()

    private val _tarotMeaningNegative = MutableStateFlow<String?>(null)
    val tarotMeaningNegative: StateFlow<String?> = _tarotMeaningNegative.asStateFlow()

    // 화면에 뿌려줄 3장의 타로 카드 후보
    private val _tarotCardOptions = MutableStateFlow<List<TarotCardInfo>>(emptyList())
    val tarotCardOptions: StateFlow<List<TarotCardInfo>> = _tarotCardOptions.asStateFlow()

    // 사용자가 선택한 카드의 index (0,1,2). 선택 전에는 null. 재선택 자유로움
    private val _selectedCardIndex = MutableStateFlow<Int?>(null)
    val selectedCardIndex: StateFlow<Int?> = _selectedCardIndex.asStateFlow()

    private val _generatedTarotSets = MutableStateFlow<List<List<Int>>>(emptyList())
    val generatedTarotSets: StateFlow<List<List<Int>>> = _generatedTarotSets.asStateFlow()

    private val _saveMessage = MutableStateFlow<String?>(null)
    val saveMessage: StateFlow<String?> = _saveMessage.asStateFlow()

    // 사용자가 고른 조합 개수(5개/10개)를 카드 선택 시점까지 기억해두기 위한 값
    private var pendingSetCount: Int = 5

    private val tarotDeck = listOf(
        TarotCardInfo(
            "0. 바보 (The Fool)",
            "새로운 도전과 무한한 가능성, 뜻밖의 행운이 찾아오는 기운입니다.",
            "경솔한 판단이나 무모한 선택으로 좋은 기회를 놓칠 수 있으니 신중함이 필요합니다."
        ),
        TarotCardInfo(
            "1. 마법사 (The Magician)",
            "당신의 뛰어난 창의력과 재능으로 원하는 결과를 이뤄내는 형국입니다.",
            "재능을 과신하거나 잔꾀를 부리다 오히려 손해를 볼 수 있는 기운이니 조심하세요."
        ),
        TarotCardInfo(
            "2. 여사제 (The High Priestess)",
            "깊은 직관력과 통찰력이 빛을 발하여 숨겨진 행운을 찾는 날입니다.",
            "지나친 의심과 우유부단함으로 눈앞의 기회를 놓칠 수 있는 기운입니다."
        ),
        TarotCardInfo(
            "3. 여황제 (The Empress)",
            "풍요와 안정, 물질적인 성취와 재물복이 가득 찬 운세입니다.",
            "나태함이나 불필요한 지출로 재물이 새어나갈 수 있으니 관리가 필요합니다."
        ),
        TarotCardInfo(
            "4. 황제 (The Emperor)",
            "확고한 리더십과 결단력으로 큰 성과와 재물을 거머쥐는 기운입니다.",
            "지나친 고집과 독단으로 주변과 마찰이 생기거나 기회를 놓칠 수 있습니다."
        ),
        TarotCardInfo(
            "7. 전차 (The Chariot)",
            "거침없는 돌파력과 승리의 에너지가 가득하여 과감한 선택이 빛을 봅니다.",
            "성급하게 밀어붙이다 방향을 잃거나 다툼이 생길 수 있으니 속도 조절이 필요합니다."
        ),
        TarotCardInfo(
            "10. 운명의 수레바퀴 (Wheel of Fortune)",
            "인생의 거대한 행운의 흐름이 당신을 향해 완벽하게 회전하고 있습니다.",
            "예상치 못한 변수로 흐름이 갑자기 뒤바뀔 수 있으니 방심은 금물입니다."
        ),
        TarotCardInfo(
            "19. 태양 (The Sun)",
            "만물을 비추는 밝은 에너지와 최고의 행운, 대길(大吉)의 기운입니다.",
            "지나친 자신감과 방심으로 사소한 실수가 생길 수 있으니 마무리까지 집중하세요."
        )
    )

    /**
     * "뽑기" 버튼 클릭 시(또는 화면 진입 시) 호출.
     * 카드 3장을 무작위로 뽑아 화면에 보여주고, 이전 선택/결과는 초기화한다.
     */
    fun drawTarotCards(setCount: Int) {
        pendingSetCount = setCount

        _tarotCardOptions.value = tarotDeck.shuffled().take(3)
        _selectedCardIndex.value = null
        _tarotCardName.value = null
        _tarotMeaningPositive.value = null
        _tarotMeaningNegative.value = null
        _generatedTarotSets.value = emptyList()
    }

    /**
     * 3장 중 하나를 클릭했을 때 호출.
     * index를 정확히 받아 선택된 카드만 색이 바뀌도록 하고,
     * 해당 카드의 긍정/부정 풀이 + 행운 번호 조합을 새로 생성한다.
     * 다른 카드를 다시 클릭하면 몇 번이든 선택을 바꿀 수 있다(재선택 허용).
     */
    fun selectTarotCard(index: Int) {
        // 아직 카드를 뽑지 않았다면(카드 후보가 없다면) 새로 뽑아서 채워준다
        if (_tarotCardOptions.value.isEmpty()) {
            _tarotCardOptions.value = tarotDeck.shuffled().take(3)
        }

        val options = _tarotCardOptions.value
        if (index !in options.indices) return

        _selectedCardIndex.value = index

        val selectedCard = options[index]
        _tarotCardName.value = "[선택한 타로: ${selectedCard.name}]"
        _tarotMeaningPositive.value = selectedCard.positiveMeaning
        _tarotMeaningNegative.value = selectedCard.negativeMeaning

        val sets = mutableListOf<List<Int>>()
        val seed = System.currentTimeMillis()

        for (i in 0 until pendingSetCount) {
            val setRandom = Random(seed + i * 99)
            val resultSet = mutableSetOf<Int>()

            while (resultSet.size < 6) {
                resultSet.add(setRandom.nextInt(1, 46))
            }
            sets.add(resultSet.sorted())
        }
        _generatedTarotSets.value = sets
    }

    fun updatePendingSetCount(setCount: Int) {
        pendingSetCount = setCount
    }

    fun saveAllTarotNumbers() {
        val currentSets = _generatedTarotSets.value
        if (currentSets.isNotEmpty()) {
            viewModelScope.launch {
                currentSets.forEach { numbers ->
                    repository.insertLotto(numbers, "TAROT")
                }
                _saveMessage.value = "성공적으로 ${currentSets.size}개의 타로 운세 조합이 내역에 저장되었습니다!"
            }
        }
    }

    fun clearSaveMessage() {
        _saveMessage.value = null
    }
}
