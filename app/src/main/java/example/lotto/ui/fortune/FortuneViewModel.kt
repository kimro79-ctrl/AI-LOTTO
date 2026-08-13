// File Path: app/src/main/java/com/kimro/ai/lotto/ui/fortune/FortuneViewModel.kt
package com.kimro.ai.lotto.ui.fortune

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kimro.ai.lotto.data.repository.LottoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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

// 달력에 표시할 하루치 출석 기록 (날짜별 뽑은 카드 이름 + 운세 점수)
data class FortuneCheckIn(val cardName: String, val keyword: String, val luckScore: Int)

@HiltViewModel
class FortuneViewModel @Inject constructor(
    application: Application,
    private val repository: LottoRepository
) : AndroidViewModel(application) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private fun todayKey(): String = dateFormat.format(Date())

    // 오늘 이미 카드를 뽑았는지, 그리고 지난 출석 기록(달력용)을 저장하기 위한 SharedPreferences.
    // Room DB를 새로 건드리지 않기 위해 간단한 JSON 문자열로 직접 저장/조회한다.
    private val prefs = application.getSharedPreferences("fortune_prefs", Context.MODE_PRIVATE)
    private val historyKey = "fortune_checkin_history"

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

    // 사용자가 선택한 카드의 index (0,1,2). 선택 전에는 null.
    // 오늘 이미 뽑았다면 앱을 다시 켜도 그 인덱스가 유지되어 재선택은 못 하고 결과만 보여준다.
    private val _selectedCardIndex = MutableStateFlow<Int?>(null)
    val selectedCardIndex: StateFlow<Int?> = _selectedCardIndex.asStateFlow()

    private val _generatedTarotSets = MutableStateFlow<List<List<Int>>>(emptyList())
    val generatedTarotSets: StateFlow<List<List<Int>>> = _generatedTarotSets.asStateFlow()

    private val _saveMessage = MutableStateFlow<String?>(null)
    val saveMessage: StateFlow<String?> = _saveMessage.asStateFlow()

    // 오늘 이미 카드를 뽑았는지 여부 (1일 1회 제한)
    private val _hasDrawnToday = MutableStateFlow(false)
    val hasDrawnToday: StateFlow<Boolean> = _hasDrawnToday.asStateFlow()

    // 날짜(yyyy-MM-dd) -> 그날의 출석 기록. 달력 화면에서 사용.
    private val _checkInHistory = MutableStateFlow<Map<String, FortuneCheckIn>>(emptyMap())
    val checkInHistory: StateFlow<Map<String, FortuneCheckIn>> = _checkInHistory.asStateFlow()

    // 사용자가 고른 조합 개수(5개/10개). 카드 선택 후에도 바꾸면 번호만 다시 생성한다.
    private var pendingSetCount: Int = 5

    private val tarotDeck = listOf(
        TarotCardInfo(
            name = "0. 바보 (The Fool)",
            keyword = "새로운 시작 · 가능성",
            positiveMeaning = "새로운 도전과 무한한 가능성, 뜻밖의 행운이 찾아오는 기운입니다.",
            negativeMeaning = "경솔한 판단이나 무모한 선택으로 좋은 기회를 놓칠 수 있으니 신중함이 필요합니다.",
            luckScore = 78, wealthStars = 3, loveStars = 4, relationshipStars = 3,
            luckyColor = "보라색", luckyNumber = 0, luckyDirection = "동쪽"
        ),
        TarotCardInfo(
            name = "1. 마법사 (The Magician)",
            keyword = "재능 · 창조",
            positiveMeaning = "당신의 뛰어난 창의력과 재능으로 원하는 결과를 이뤄내는 형국입니다.",
            negativeMeaning = "재능을 과신하거나 잔꾀를 부리다 오히려 손해를 볼 수 있는 기운이니 조심하세요.",
            luckScore = 85, wealthStars = 4, loveStars = 3, relationshipStars = 4,
            luckyColor = "빨간색", luckyNumber = 1, luckyDirection = "남쪽"
        ),
        TarotCardInfo(
            name = "2. 여사제 (The High Priestess)",
            keyword = "직관 · 통찰",
            positiveMeaning = "깊은 직관력과 통찰력이 빛을 발하여 숨겨진 행운을 찾는 날입니다.",
            negativeMeaning = "지나친 의심과 우유부단함으로 눈앞의 기회를 놓칠 수 있는 기운입니다.",
            luckScore = 74, wealthStars = 3, loveStars = 3, relationshipStars = 3,
            luckyColor = "남색", luckyNumber = 2, luckyDirection = "북쪽"
        ),
        TarotCardInfo(
            name = "3. 여황제 (The Empress)",
            keyword = "풍요 · 안정",
            positiveMeaning = "풍요와 안정, 물질적인 성취와 재물복이 가득 찬 운세입니다.",
            negativeMeaning = "나태함이나 불필요한 지출로 재물이 새어나갈 수 있으니 관리가 필요합니다.",
            luckScore = 88, wealthStars = 5, loveStars = 4, relationshipStars = 4,
            luckyColor = "초록색", luckyNumber = 3, luckyDirection = "서쪽"
        ),
        TarotCardInfo(
            name = "4. 황제 (The Emperor)",
            keyword = "리더십 · 결단",
            positiveMeaning = "확고한 리더십과 결단력으로 큰 성과와 재물을 거머쥐는 기운입니다.",
            negativeMeaning = "지나친 고집과 독단으로 주변과 마찰이 생기거나 기회를 놓칠 수 있습니다.",
            luckScore = 82, wealthStars = 4, loveStars = 3, relationshipStars = 3,
            luckyColor = "주황색", luckyNumber = 4, luckyDirection = "남쪽"
        ),
        TarotCardInfo(
            name = "7. 전차 (The Chariot)",
            keyword = "돌파 · 승리",
            positiveMeaning = "거침없는 돌파력과 승리의 에너지가 가득하여 과감한 선택이 빛을 봅니다.",
            negativeMeaning = "성급하게 밀어붙이다 방향을 잃거나 다툼이 생길 수 있으니 속도 조절이 필요합니다.",
            luckScore = 90, wealthStars = 4, loveStars = 3, relationshipStars = 3,
            luckyColor = "은색", luckyNumber = 7, luckyDirection = "동쪽"
        ),
        TarotCardInfo(
            name = "10. 운명의 수레바퀴 (Wheel of Fortune)",
            keyword = "전환 · 행운",
            positiveMeaning = "인생의 거대한 행운의 흐름이 당신을 향해 완벽하게 회전하고 있습니다.",
            negativeMeaning = "예상치 못한 변수로 흐름이 갑자기 뒤바뀔 수 있으니 방심은 금물입니다.",
            luckScore = 95, wealthStars = 5, loveStars = 4, relationshipStars = 4,
            luckyColor = "금색", luckyNumber = 10, luckyDirection = "중앙"
        ),
        TarotCardInfo(
            name = "19. 태양 (The Sun)",
            keyword = "성취 · 밝음",
            positiveMeaning = "만물을 비추는 밝은 에너지와 최고의 행운, 대길(大吉)의 기운입니다.",
            negativeMeaning = "지나친 자신감과 방심으로 사소한 실수가 생길 수 있으니 마무리까지 집중하세요.",
            luckScore = 98, wealthStars = 5, loveStars = 5, relationshipStars = 5,
            luckyColor = "노란색", luckyNumber = 19, luckyDirection = "동쪽"
        )
    )

    init {
        loadHistory()
        restoreTodayIfAlreadyDrawn()
    }

    private fun loadHistory() {
        val raw = prefs.getString(historyKey, null) ?: return
        try {
            val json = JSONObject(raw)
            val map = mutableMapOf<String, FortuneCheckIn>()
            json.keys().forEach { dateKey ->
                val obj = json.getJSONObject(dateKey)
                map[dateKey] = FortuneCheckIn(
                    cardName = obj.getString("card"),
                    keyword = obj.optString("keyword", ""),
                    luckScore = obj.getInt("score")
                )
            }
            _checkInHistory.value = map
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveHistory(map: Map<String, FortuneCheckIn>) {
        val json = JSONObject()
        map.forEach { (date, checkIn) ->
            val obj = JSONObject()
            obj.put("card", checkIn.cardName)
            obj.put("keyword", checkIn.keyword)
            obj.put("score", checkIn.luckScore)
            json.put(date, obj)
        }
        prefs.edit().putString(historyKey, json.toString()).apply()
    }

    /** 앱을 다시 켰을 때, 오늘 이미 카드를 뽑은 기록이 있으면 그 결과를 그대로 복원해서 보여준다. */
    private fun restoreTodayIfAlreadyDrawn() {
        val todayRecord = _checkInHistory.value[todayKey()] ?: return
        val matchedCard = tarotDeck.firstOrNull { it.name == todayRecord.cardName } ?: return

        _hasDrawnToday.value = true
        _tarotCardOptions.value = listOf(matchedCard) // 카드 후보 대신 확정된 결과만 유지
        _selectedCardIndex.value = 0
        _selectedCardInfo.value = matchedCard
        _tarotCardName.value = matchedCard.name
        _tarotKeyword.value = matchedCard.keyword
        _tarotMeaningPositive.value = matchedCard.positiveMeaning
        _tarotMeaningNegative.value = matchedCard.negativeMeaning
        regenerateLuckyNumbers()
    }

    /**
     * "오늘의 카드 뽑기" 버튼 클릭 시 호출. 오늘 이미 뽑았다면 아무 동작도 하지 않는다(1일 1회 제한).
     */
    fun drawTarotCards(setCount: Int) {
        if (_hasDrawnToday.value) return

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
     * 3장 중 하나를 클릭했을 때 호출. 오늘 이미 뽑았다면 무시한다.
     * 선택하는 순간 오늘 날짜로 기록이 확정 저장되며, 이후로는 재선택이 불가능해진다(1일 1회).
     */
    fun selectTarotCard(index: Int) {
        if (_hasDrawnToday.value) return

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

        // 오늘 날짜로 확정 저장 - 이 시점부터 오늘은 더 이상 다시 뽑을 수 없다.
        _hasDrawnToday.value = true
        val newHistory = _checkInHistory.value.toMutableMap()
        newHistory[todayKey()] = FortuneCheckIn(selectedCard.name, selectedCard.keyword, selectedCard.luckScore)
        _checkInHistory.value = newHistory
        saveHistory(newHistory)
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
