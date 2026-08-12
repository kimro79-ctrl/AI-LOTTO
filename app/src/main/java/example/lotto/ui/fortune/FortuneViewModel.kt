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

// 카드 1장에 대한 모든 콘텐츠(이름/키워드/풀이/운세점수/별점/행운 요소)를 담는 데이터 클래스.
// 전부 카드별로 미리 정해둔 고정 콘텐츠라 실시간 데이터 없이도 동작한다 - "오늘의 운세 점수" 등은
// 실제 확률/통계가 아니라 재미를 위한 콘텐츠 점수임을 화면에서 명확히 표기한다.
data class TarotCardInfo(
    val name: String,
    val keyword: String,
    val positiveMeaning: String,
    val negativeMeaning: String,
    val luckScore: Int,          // 0~100, 콘텐츠용 "오늘의 운세 점수"
    val wealthStars: Int,        // 1~5, 재물운
    val loveStars: Int,          // 1~5, 애정운
    val relationshipStars: Int,  // 1~5, 대인관계운
    val luckyColor: String,
    val luckyNumber: Int,
    val luckyDirection: String
)

@HiltViewModel
class FortuneViewModel @Inject constructor(
    private val repository: LottoRepository
) : ViewModel() {

    private val _tarotCardName = MutableStateFlow<String?>(null)
    val tarotCardName: StateFlow<String?> = _tarotCardName.asStateFlow()

    private val _tarotKeyword = MutableStateFlow<String?>(null)
    val tarotKeyword: StateFlow<String?> = _tarotKeyword.asStateFlow()

    private val _tarotMeaningPositive = MutableStateFlow<String?>(null)
    val tarotMeaningPositive: StateFlow<String?> = _tarotMeaningPositive.asStateFlow()

    private val _tarotMeaningNegative = MutableStateFlow<String?>(null)
    val tarotMeaningNegative: StateFlow<String?> = _tarotMeaningNegative.asStateFlow()

    private val _selectedCardInfo = MutableStateFlow<TarotCardInfo?>(null)
    val selectedCardInfo: StateFlow<TarotCardInfo?> = _selectedCardInfo.asStateFlow()

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

    // 사용자가 고른 조합 개수(5개/10개). 카드 선택 후에도 바꾸면 번호만 다시 생성한다.
    private var pendingSetCount: Int = 5

    private val tarotDeck = listOf(
        // ── 긍정 및 확장형 카드 ──
        TarotCardInfo(
            name = "0. 바보 (The Fool)",
            keyword = "새로운 시작 · 직관적 도전",
            positiveMeaning = "계획에 없던 새로운 시도나 직관적인 선택이 신선한 활력을 불어넣는 하루입니다. 기존의 틀에 박힌 사고방식에서 벗어나 가벼운 마음으로 도전해 보는 것이 유리합니다.",
            negativeMeaning = "준비되지 않은 상태에서 무모하게 금전적 투자를 하거나 즉흥적으로 지갑을 열면 낭패를 볼 수 있습니다. 가벼운 호기심은 좋으나 실속 없는 요행은 철저히 경계해야 합니다.",
            luckScore = 65, wealthStars = 2, loveStars = 3, relationshipStars = 3,
            luckyColor = "보라색", luckyNumber = 0, luckyDirection = "동쪽"
        ),
        TarotCardInfo(
            name = "1. 마법사 (The Magician)",
            keyword = "재능 발휘 · 기회의 활용",
            positiveMeaning = "당신이 가진 경험과 재능을 활용하여 주변 상황을 유리하게 주도할 수 있는 국면입니다. 아이디어가 반짝이는 날이므로 중요한 결정이나 계획을 추진하기에 아주 적합합니다.",
            negativeMeaning = "말만 앞서거나 지나친 요령을 피우다 오히려 주변의 신뢰를 잃을 수 있습니다. 눈앞의 작은 이익에 현혹되어 장기적인 리스크를 보지 못하는 실수를 조심하세요.",
            luckScore = 82, wealthStars = 4, loveStars = 3, relationshipStars = 4,
            luckyColor = "빨간색", luckyNumber = 1, luckyDirection = "남쪽"
        ),
        TarotCardInfo(
            name = "2. 여사제 (The High Priestess)",
            keyword = "철저한 관망 · 냉철한 직관",
            positiveMeaning = "감정에 치우치지 않고 냉정하게 상황을 분석하는 통찰력이 빛을 발하는 시기입니다. 불확실한 정보나 남의 말에 휘둘리지 않고 본인의 직관을 믿는 것이 가장 현명합니다.",
            negativeMeaning = "지나치게 마음의 문을 닫아걸거나 의심이 지나쳐 주변의 소중한 조언마저 차단할 수 있습니다. 지나친 신중함이 오히려 결정을 지연시켜 답답함을 유발할 수 있습니다.",
            luckScore = 70, wealthStars = 3, loveStars = 2, relationshipStars = 3,
            luckyColor = "남색", luckyNumber = 2, luckyDirection = "북쪽"
        ),
        TarotCardInfo(
            name = "3. 여황제 (The Empress)",
            keyword = "풍요로운 결실 · 안정적 흐름",
            positiveMeaning = "그동안 공들였던 일에서 물질적·심리적인 안정과 만족스러운 성과를 거두는 운세입니다. 금전적으로 여유가 생기거나 뜻밖의 소소한 이득이 들어올 수 있는 편안한 하루입니다.",
            negativeMeaning = "현재의 안락함에 취해 나태해지거나 불필요한 사치와 과소비로 지출이 급증할 수 있습니다. 수입이 늘어나는 만큼 자금 관리에 대한 긴장감을 유지할 필요가 있습니다.",
            luckScore = 88, wealthStars = 5, loveStars = 4, relationshipStars = 4,
            luckyColor = "초록색", luckyNumber = 3, luckyDirection = "서쪽"
        ),
        TarotCardInfo(
            name = "4. 황제 (The Emperor)",
            keyword = "확고한 통제 · 현실적 성취",
            positiveMeaning = "조직이나 자금 흐름을 주도적으로 통제하며 실질적인 성과와 권위를 쟁취하는 형국입니다. 철저한 원칙과 계획대로 밀어붙이면 원하던 목표를 확고하게 달성할 수 있습니다.",
            negativeMeaning = "권위적인 태도나 지나친 고집으로 인해 주변 사람들과 불필요한 마찰이 빚어질 수 있습니다. 융통성을 발휘하지 않으면 오히려 고립될 수 있으니 부드러운 대처가 필요합니다.",
            luckScore = 84, wealthStars = 4, loveStars = 3, relationshipStars = 3,
            luckyColor = "주황색", luckyNumber = 4, luckyDirection = "남쪽"
        ),
        TarotCardInfo(
            name = "7. 전차 (The Chariot)",
            keyword = "정면 돌파 · 속도감 있는 진행",
            positiveMeaning = "장기화되던 정체 구간을 시원하게 벗어나며 과감한 결단으로 승기를 잡는 기운입니다. 의욕이 최고조에 달하므로 밀어붙이는 일마다 가속도가 붙고 성취감이 큽니다.",
            negativeMeaning = "속도에만 집착한 나머지 주변의 위험 요소를 놓치거나 무리한 충돌을 유발할 수 있습니다. 감정이 앞서 성급하게 움직이면 자칫 실수가 생기니 호흡을 가다듬으세요.",
            luckScore = 91, wealthStars = 4, loveStars = 3, relationshipStars = 3,
            luckyColor = "은색", luckyNumber = 7, luckyDirection = "동쪽"
        ),
        TarotCardInfo(
            name = "10. 운명의 수레바퀴 (Wheel of Fortune)",
            keyword = "흐름의 반전 · 뜻밖의 행운",
            positiveMeaning = "정체되어 있던 상황의 흐름이 유리한 방향으로 급격하게 전환되는 강력한 대길운입니다. 예상치 못한 경로를 통해 재물이나 기회가 찾아올 수 있는 드라마틱한 시점입니다.",
            negativeMeaning = "상황 변화가 빠른 만큼 방심하는 사이에 유리했던 국면이 순식간에 불리해질 수도 있습니다. 행운만 믿고 아무런 대비를 하지 않으면 다 잡은 기회를 놓치기 십상입니다.",
            luckScore = 96, wealthStars = 5, loveStars = 4, relationshipStars = 4,
            luckyColor = "금색", luckyNumber = 10, luckyDirection = "중앙"
        ),
        TarotCardInfo(
            name = "19. 태양 (The Sun)",
            keyword = "최고의 성취 · 밝은 미래",
            positiveMeaning = "모든 어둠과 고민이 걷히고 명확한 해답과 눈부신 성과가 기다리고 있는 최고의 길운입니다. 막혀 있던 자금 흐름이나 인간관계가 시원하게 풀리며 자신감이 충만해집니다.",
            negativeMeaning = "지나친 낙관주의에 빠져 사소한 리스크나 디테일을 놓치고 지나갈 수 있습니다. 마지막 순간까지 방심하지 않고 주변을 살피는 신중함이 완벽한 성공을 완성합니다.",
            luckScore = 99, wealthStars = 5, loveStars = 5, relationshipStars = 5,
            luckyColor = "노란색", luckyNumber = 19, luckyDirection = "동쪽"
        ),

        // ── 신중/경고/부정적 카드 (추가 보강) ──
        TarotCardInfo(
            name = "5. 교황 (The Hierophant)",
            keyword = "고지식한 태도 · 형식주의",
            positiveMeaning = "원칙을 지키고 정석대로 절차를 밟아가면 안정적인 결과를 확보할 수 있는 날입니다.",
            negativeMeaning = "지나친 형식과 틀에 얽매여 유연한 대처를 하지 못하고 오히려 손해를 볼 수 있습니다. 주변의 새로운 변화나 조언을 무시하면 고립을 자초하게 됩니다.",
            luckScore = 58, wealthStars = 2, loveStars = 3, relationshipStars = 2,
            luckyColor = "회갈색", luckyNumber = 5, luckyDirection = "북쪽"
        ),
        TarotCardInfo(
            name = "12. 매달린 사람 (The Hanged Man)",
            keyword = "정체 · 답답함과 인내",
            positiveMeaning = "현재의 정체기가 오히려 숨 고르기를 하며 내실을 다지는 소중한 자양분이 될 수 있습니다.",
            negativeMeaning = "애써 노력해도 보상이 뒤따르지 않고 진퇴양난에 빠져 답답함이 극에 달하는 시기입니다. 억지로 상황을 뒤집으려 하면 피해만 커지므로 조용히 때를 기다려야 합니다.",
            luckScore = 40, wealthStars = 1, loveStars = 2, relationshipStars = 2,
            luckyColor = "청록색", luckyNumber = 12, luckyDirection = "서쪽"
        ),
        TarotCardInfo(
            name = "13. 죽음 (Death)",
            keyword = "관계의 단절 · 소모적 지출",
            positiveMeaning = "불필요하게 끌어오던 부실한 관계나 소모적인 일을 과감하게 끊어낼 수 있는 전환점입니다.",
            negativeMeaning = "예상치 못한 손실이 발생하거나 오랫동안 공들였던 계획이 수포로 돌아갈 수 있습니다. 미련을 버리지 못하면 더 큰 화를 부르니 냉정하게 손절해야 합니다.",
            luckScore = 30, wealthStars = 1, loveStars = 1, relationshipStars = 1,
            luckyColor = "검은색", luckyNumber = 13, luckyDirection = "서쪽"
        ),
        TarotCardInfo(
            name = "15. 악마 (The Devil)",
            keyword = "유혹과 집착 · 요행 경계",
            positiveMeaning = "스스로의 깊은 욕망이나 집착이 무엇인지 객관적으로 돌아볼 수 있는 계기가 됩니다.",
            negativeMeaning = "단기간에 큰돈을 벌려는 투기 심리나 요행에 눈이 멀어 무리수를 두기 쉬운 날입니다. 감언이설이나 불확실한 투자 제안은 철저하게 차단해야 자금을 지킵니다.",
            luckScore = 38, wealthStars = 1, loveStars = 2, relationshipStars = 2,
            luckyColor = "어두운붉은색", luckyNumber = 15, luckyDirection = "남쪽"
        ),
        TarotCardInfo(
            name = "16. 탑 (The Tower)",
            keyword = "갑작스러운 타격 · 계획 수정",
            positiveMeaning = "기존에 잘못 끼워졌던 단추나 썩어 있던 문제를 과감하게 도려낼 수 있는 기회입니다.",
            negativeMeaning = "예상치 못한 금전적 지출이나 사고, 혹은 믿었던 계획이 한순간에 틀어지는 충격을 겪을 수 있습니다. 오늘은 새로운 시도를 멈추고 리스크 관리에만 총력을 기울이세요.",
            luckScore = 25, wealthStars = 1, loveStars = 1, relationshipStars = 1,
            luckyColor = "차콜색", luckyNumber = 16, luckyDirection = "북쪽"
        ),
        TarotCardInfo(
            name = "18. 달 (The Moon)",
            keyword = "불안감 증폭 · 정보 혼선",
            positiveMeaning = "직관이나 감수성이 예민해져 남들이 보지 못하는 이면의 진실을 본능적으로 감지합니다.",
            negativeMeaning = "실체 없는 불안감과 의심이 커져 중요한 판단을 그릇치기 쉬운 형국입니다. 명확하지 않은 소문이나 루머에 현혹되어 자칫 금전적 손해를 볼 수 있으니 주의하세요.",
            luckScore = 45, wealthStars = 2, loveStars = 2, relationshipStars = 2,
            luckyColor = "짙은회색", luckyNumber = 18, luckyDirection = "북서쪽"
        )
    )

    /**
     * "오늘의 카드 뽑기" 버튼 클릭 시(또는 화면 진입 시) 호출.
     * 카드 3장을 무작위로 뽑아 화면에 보여주고, 이전 선택/결과는 초기화한다.
     */
    fun drawTarotCards(setCount: Int) {
        pendingSetCount = setCount

        _tarotCardOptions.value = tarotDeck.shuffled().take(3)
        _selectedCardIndex.value = null
        _selectedCardInfo.value = null
        _tarotCardName.value = null
        _tarotKeyword.value = null
        _tarotMeaningPositive.value = null
        _tarotMeaningNegative.value = null
        _generatedTarotSets.value = emptyList()
    }

    /**
     * 3장 중 하나를 클릭했을 때 호출.
     * index를 정확히 받아 선택된 카드만 색이 바뀌도록 하고,
     * 해당 카드의 전체 콘텐츠(풀이/운세점수/별점/행운요소) + 행운 번호 조합을 새로 생성한다.
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
        _selectedCardInfo.value = selectedCard
        _tarotCardName.value = selectedCard.name
        _tarotKeyword.value = selectedCard.keyword
        _tarotMeaningPositive.value = selectedCard.positiveMeaning
        _tarotMeaningNegative.value = selectedCard.negativeMeaning

        regenerateLuckyNumbers()
    }

    /**
     * 5개/10개 조합 개수를 바꿀 때 호출. 카드를 이미 선택한 상태라면 번호만 즉시 다시 생성하고,
     * 아직 카드를 선택하지 않았다면 다음 선택 시 반영될 개수만 기억해둔다.
     */
    fun updateSetCount(setCount: Int) {
        pendingSetCount = setCount
        if (_selectedCardInfo.value != null) {
            regenerateLuckyNumbers()
        }
    }

    private fun regenerateLuckyNumbers() {
        val sets = mutableListOf<List<Int>>()
        val seed = System.currentTimeMillis()
        
        // 선택된 카드의 행운 번호 확인 (1~45 범위 내일 때만 사용)
        val cardLuckyNumber = _selectedCardInfo.value?.luckyNumber ?: 0

        for (i in 0 until pendingSetCount) {
            val setRandom = Random(seed + i * 99)
            val resultSet = mutableSetOf<Int>()

            // 첫 번째와 두 번째 게임(인덱스 0, 1)에만 타로 행운 번호를 강제 포함시킴
            if (i < 2 && cardLuckyNumber in 1..45) {
                resultSet.add(cardLuckyNumber)
            }

            // 나머지 빈 자리를 1~45 사이의 랜덤 숫자로 채움 (나머지 게임은 완전 랜덤)
            while (resultSet.size < 6) {
                resultSet.add(setRandom.nextInt(1, 46))
            }
            
            sets.add(resultSet.sorted())
        }
        _generatedTarotSets.value = sets
    }

    fun saveAllTarotNumbers() {
        val currentSets = _generatedTarotSets.value
        if (currentSets.isNotEmpty()) {
            viewModelScope.launch {
                currentSets.forEach { numbers ->
                    repository.insertLotto(numbers, "TAROT")
                }
                _saveMessage.value = "✓ 내 번호에 저장되었습니다"
            }
        }
    }

    fun clearSaveMessage() {
        _saveMessage.value = null
    }
}
