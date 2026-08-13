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
import kotlin.math.abs
import kotlin.random.Random

// 카드 1장에 대한 모든 콘텐츠
data class TarotCardInfo(
    val name: String,
    val keyword: String,
    val positiveMeaning: String,
    val negativeMeaning: String,
    val luckScore: Int,
    val wealthStars: Int,
    val loveStars: Int,
    val relationshipStars: Int,
    val luckyColor: String,
    val luckyNumber: Int,
    val luckyDirection: String
)

// 달력에 표시할 하루치 기록
data class FortuneCheckIn(
    val cardName: String,
    val keyword: String,
    val luckScore: Int
)

@HiltViewModel
class FortuneViewModel @Inject constructor(
    application: Application,
    private val repository: LottoRepository
) : AndroidViewModel(application) {

    private val dateFormat =
        SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.getDefault()
        )

    private fun todayKey(): String =
        dateFormat.format(Date())

    private val prefs =
        application.getSharedPreferences(
            "fortune_prefs",
            Context.MODE_PRIVATE
        )

    private val historyKey =
        "fortune_checkin_history"

    // ============================================================
    // 오늘의 타로
    // ============================================================

    private val _tarotCardName =
        MutableStateFlow<String?>(null)

    val tarotCardName:
        StateFlow<String?> =
        _tarotCardName.asStateFlow()

    private val _tarotKeyword =
        MutableStateFlow<String?>(null)

    val tarotKeyword:
        StateFlow<String?> =
        _tarotKeyword.asStateFlow()

    private val _tarotMeaningPositive =
        MutableStateFlow<String?>(null)

    val tarotMeaningPositive:
        StateFlow<String?> =
        _tarotMeaningPositive.asStateFlow()

    private val _tarotMeaningNegative =
        MutableStateFlow<String?>(null)

    val tarotMeaningNegative:
        StateFlow<String?> =
        _tarotMeaningNegative.asStateFlow()

    private val _selectedCardInfo =
        MutableStateFlow<TarotCardInfo?>(null)

    val selectedCardInfo:
        StateFlow<TarotCardInfo?> =
        _selectedCardInfo.asStateFlow()

    // 화면에 뿌릴 3장의 타로 카드
    private val _tarotCardOptions =
        MutableStateFlow<List<TarotCardInfo>>(
            emptyList()
        )

    val tarotCardOptions:
        StateFlow<List<TarotCardInfo>> =
        _tarotCardOptions.asStateFlow()

    // 선택된 카드 index
    private val _selectedCardIndex =
        MutableStateFlow<Int?>(null)

    val selectedCardIndex:
        StateFlow<Int?> =
        _selectedCardIndex.asStateFlow()

    // ============================================================
    // 로또 조합
    // ============================================================

    private val _generatedTarotSets =
        MutableStateFlow<List<List<Int>>>(
            emptyList()
        )

    val generatedTarotSets:
        StateFlow<List<List<Int>>> =
        _generatedTarotSets.asStateFlow()

    // ============================================================
    // 오늘의 행운번호 3개
    // ============================================================

    private val _luckyNumbers =
        MutableStateFlow<List<Int>>(
            emptyList()
        )

    val luckyNumbers:
        StateFlow<List<Int>> =
        _luckyNumbers.asStateFlow()

    // ============================================================
    // 저장 메시지
    // ============================================================

    private val _saveMessage =
        MutableStateFlow<String?>(null)

    val saveMessage:
        StateFlow<String?> =
        _saveMessage.asStateFlow()

    // ============================================================
    // 오늘 이미 뽑았는지
    // ============================================================

    private val _hasDrawnToday =
        MutableStateFlow(false)

    val hasDrawnToday:
        StateFlow<Boolean> =
        _hasDrawnToday.asStateFlow()

    // ============================================================
    // 달력 기록
    // ============================================================

    private val _checkInHistory =
        MutableStateFlow<Map<String, FortuneCheckIn>>(
            emptyMap()
        )

    val checkInHistory:
        StateFlow<Map<String, FortuneCheckIn>> =
        _checkInHistory.asStateFlow()

    // 5개 / 10개
    private var pendingSetCount: Int = 5

    // ============================================================
    // 타로 덱
    // 기존 카드 데이터 그대로 유지
    // ============================================================

    private val tarotDeck =
        listOf(

            TarotCardInfo(
                name = "0. 바보 (The Fool)",
                keyword = "새로운 시작 · 가능성",
                positiveMeaning =
                    "새로운 도전과 무한한 가능성, 뜻밖의 행운이 찾아오는 기운입니다.",
                negativeMeaning =
                    "경솔한 판단이나 무모한 선택으로 좋은 기회를 놓칠 수 있으니 신중함이 필요합니다.",
                luckScore = 78,
                wealthStars = 3,
                loveStars = 4,
                relationshipStars = 3,
                luckyColor = "보라색",
                luckyNumber = 0,
                luckyDirection = "동쪽"
            ),

            TarotCardInfo(
                name = "1. 마법사 (The Magician)",
                keyword = "재능 · 창조",
                positiveMeaning =
                    "당신의 뛰어난 창의력과 재능으로 원하는 결과를 이뤄내는 형국입니다.",
                negativeMeaning =
                    "재능을 과신하거나 잔꾀를 부리다 오히려 손해를 볼 수 있는 기운이니 조심하세요.",
                luckScore = 85,
                wealthStars = 4,
                loveStars = 3,
                relationshipStars = 4,
                luckyColor = "빨간색",
                luckyNumber = 1,
                luckyDirection = "남쪽"
            ),

            TarotCardInfo(
                name = "2. 여사제 (The High Priestess)",
                keyword = "직관 · 통찰",
                positiveMeaning =
                    "깊은 직관력과 통찰력이 빛을 발하여 숨겨진 행운을 찾는 날입니다.",
                negativeMeaning =
                    "지나친 의심과 우유부단함으로 눈앞의 기회를 놓칠 수 있는 기운입니다.",
                luckScore = 74,
                wealthStars = 3,
                loveStars = 3,
                relationshipStars = 3,
                luckyColor = "남색",
                luckyNumber = 2,
                luckyDirection = "북쪽"
            ),

            TarotCardInfo(
                name = "3. 여황제 (The Empress)",
                keyword = "풍요 · 안정",
                positiveMeaning =
                    "풍요와 안정, 물질적인 성취와 재물복이 가득 찬 운세입니다.",
                negativeMeaning =
                    "나태함이나 불필요한 지출로 재물이 새어나갈 수 있으니 관리가 필요합니다.",
                luckScore = 88,
                wealthStars = 5,
                loveStars = 4,
                relationshipStars = 4,
                luckyColor = "초록색",
                luckyNumber = 3,
                luckyDirection = "서쪽"
            ),

            TarotCardInfo(
                name = "4. 황제 (The Emperor)",
                keyword = "리더십 · 결단",
                positiveMeaning =
                    "확고한 리더십과 결단력으로 큰 성과와 재물을 거머쥐는 기운입니다.",
                negativeMeaning =
                    "지나친 고집과 독단으로 주변과 마찰이 생기거나 기회를 놓칠 수 있습니다.",
                luckScore = 82,
                wealthStars = 4,
                loveStars = 3,
                relationshipStars = 3,
                luckyColor = "주황색",
                luckyNumber = 4,
                luckyDirection = "남쪽"
            ),

            TarotCardInfo(
                name = "7. 전차 (The Chariot)",
                keyword = "돌파 · 승리",
                positiveMeaning =
                    "거침없는 돌파력과 승리의 에너지가 가득하여 과감한 선택이 빛을 봅니다.",
                negativeMeaning =
                    "성급하게 밀어붙이다 방향을 잃거나 다툼이 생길 수 있으니 속도 조절이 필요합니다.",
                luckScore = 90,
                wealthStars = 4,
                loveStars = 3,
                relationshipStars = 3,
                luckyColor = "은색",
                luckyNumber = 7,
                luckyDirection = "동쪽"
            ),

            TarotCardInfo(
                name = "10. 운명의 수레바퀴 (Wheel of Fortune)",
                keyword = "전환 · 행운",
                positiveMeaning =
                    "인생의 거대한 행운의 흐름이 당신을 향해 완벽하게 회전하고 있습니다.",
                negativeMeaning =
                    "예상치 못한 변수로 흐름이 갑자기 뒤바뀔 수 있으니 방심은 금물입니다.",
                luckScore = 95,
                wealthStars = 5,
                loveStars = 4,
                relationshipStars = 4,
                luckyColor = "금색",
                luckyNumber = 10,
                luckyDirection = "중앙"
            ),

            TarotCardInfo(
                name = "19. 태양 (The Sun)",
                keyword = "성취 · 밝음",
                positiveMeaning =
                    "만물을 비추는 밝은 에너지와 최고의 행운, 대길(大吉)의 기운입니다.",
                negativeMeaning =
                    "지나친 자신감과 방심으로 사소한 실수가 생길 수 있으니 마무리까지 집중하세요.",
                luckScore = 98,
                wealthStars = 5,
                loveStars = 5,
                relationshipStars = 5,
                luckyColor = "노란색",
                luckyNumber = 19,
                luckyDirection = "동쪽"
            )
        )

    // ============================================================
    // 초기화
    // ============================================================

    init {
        loadHistory()
        restoreTodayIfAlreadyDrawn()
    }

    // ============================================================
    // 달력 기록 불러오기
    // ============================================================

    private fun loadHistory() {

        val raw =
            prefs.getString(
                historyKey,
                null
            ) ?: return

        try {

            val json =
                JSONObject(raw)

            val map =
                mutableMapOf<String, FortuneCheckIn>()

            json.keys().forEach { dateKey ->

                val obj =
                    json.getJSONObject(dateKey)

                map[dateKey] =
                    FortuneCheckIn(
                        cardName =
                            obj.getString("card"),
                        keyword =
                            obj.optString(
                                "keyword",
                                ""
                            ),
                        luckScore =
                            obj.getInt("score")
                    )
            }

            _checkInHistory.value = map

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ============================================================
    // 달력 기록 저장
    // ============================================================

    private fun saveHistory(
        map: Map<String, FortuneCheckIn>
    ) {

        val json =
            JSONObject()

        map.forEach { (date, checkIn) ->

            val obj =
                JSONObject()

            obj.put(
                "card",
                checkIn.cardName
            )

            obj.put(
                "keyword",
                checkIn.keyword
            )

            obj.put(
                "score",
                checkIn.luckScore
            )

            json.put(
                date,
                obj
            )
        }

        prefs.edit()
            .putString(
                historyKey,
                json.toString()
            )
            .apply()
    }

    // ============================================================
    // 오늘 이미 뽑은 경우 복원
    // ============================================================

    private fun restoreTodayIfAlreadyDrawn() {

        val todayRecord =
            _checkInHistory.value[
                todayKey()
            ] ?: return

        val matchedCard =
            tarotDeck.firstOrNull {
                it.name ==
                    todayRecord.cardName
            } ?: return

        _hasDrawnToday.value =
            true

        _tarotCardOptions.value =
            listOf(matchedCard)

        _selectedCardIndex.value =
            0

        _selectedCardInfo.value =
            matchedCard

        _tarotCardName.value =
            matchedCard.name

        _tarotKeyword.value =
            matchedCard.keyword

        _tarotMeaningPositive.value =
            matchedCard.positiveMeaning

        _tarotMeaningNegative.value =
            matchedCard.negativeMeaning

        regenerateLuckyNumbers()
    }

    // ============================================================
    // 카드 뽑기
    // ============================================================

    fun drawTarotCards(
        setCount: Int
    ) {

        if (_hasDrawnToday.value)
            return

        pendingSetCount =
            setCount

        _tarotCardOptions.value =
            tarotDeck
                .shuffled()
                .take(3)

        _selectedCardIndex.value =
            null

        _selectedCardInfo.value =
            null

        _tarotCardName.value =
            null

        _tarotKeyword.value =
            null

        _tarotMeaningPositive.value =
            null

        _tarotMeaningNegative.value =
            null

        _generatedTarotSets.value =
            emptyList()

        _luckyNumbers.value =
            emptyList()
    }

    // ============================================================
    // 카드 선택
    // ============================================================

    fun selectTarotCard(
        index: Int
    ) {

        if (_hasDrawnToday.value)
            return

        if (_tarotCardOptions.value.isEmpty()) {

            _tarotCardOptions.value =
                tarotDeck
                    .shuffled()
                    .take(3)
        }

        val options =
            _tarotCardOptions.value

        if (index !in options.indices)
            return

        _selectedCardIndex.value =
            index

        val selectedCard =
            options[index]

        _selectedCardInfo.value =
            selectedCard

        _tarotCardName.value =
            selectedCard.name

        _tarotKeyword.value =
            selectedCard.keyword

        _tarotMeaningPositive.value =
            selectedCard.positiveMeaning

        _tarotMeaningNegative.value =
            selectedCard.negativeMeaning

        // 행운번호 + 로또 조합 생성
        regenerateLuckyNumbers()

        // 오늘 날짜로 확정
        _hasDrawnToday.value =
            true

        val newHistory =
            _checkInHistory.value
                .toMutableMap()

        newHistory[todayKey()] =
            FortuneCheckIn(
                cardName =
                    selectedCard.name,
                keyword =
                    selectedCard.keyword,
                luckScore =
                    selectedCard.luckScore
            )

        _checkInHistory.value =
            newHistory

        saveHistory(
            newHistory
        )
    }

    // ============================================================
    // 5개 / 10개 조합 변경
    // ============================================================

    fun updateSetCount(
        setCount: Int
    ) {

        pendingSetCount =
            setCount

        if (_selectedCardInfo.value != null) {
            regenerateLuckyNumbers()
        }
    }

    // ============================================================
    // 핵심:
    //
    // 1. 타로 카드의 luckyNumber 기반
    // 2. 행운번호 3개 생성
    // 3. 생성된 여러 조합 중 정확히 2세트에만 반영
    // 4. 반영되는 두 세트는 서로 연속되지 않음
    // ============================================================

    private fun regenerateLuckyNumbers() {

        val selectedCard =
            _selectedCardInfo.value
                ?: return

        val setCount =
            pendingSetCount.coerceIn(
                1,
                10
            )

        /*
         * 카드의 luckyNumber가 1~45이면 그대로 사용.
         * 바보 카드처럼 0이면 7을 기본 행운번호로 사용.
         */
        val baseLuckyNumber =
            if (
                selectedCard.luckyNumber
                    in 1..45
            ) {
                selectedCard.luckyNumber
            } else {
                7
            }

        /*
         * 카드의 행운번호를 중심으로
         * 오늘만의 랜덤 시드를 만든다.
         */
        val seed =
            System.currentTimeMillis() +
                baseLuckyNumber * 997L +
                selectedCard.luckScore * 37L

        val random =
            Random(seed)

        // ========================================================
        // 행운번호 3개 생성
        // ========================================================

        val luckySet =
            mutableSetOf<Int>()

        // 첫 번째는 타로 카드의 행운번호
        luckySet.add(
            baseLuckyNumber
        )

        /*
         * 나머지 2개는 기존 행운번호와
         * 너무 가까운 숫자를 피한다.
         *
         * 즉 7,8,9 같은 연속 번호가 나오지 않도록 함.
         */
        var guardCount = 0

        while (
            luckySet.size < 3 &&
            guardCount < 500
        ) {

            guardCount++

            val candidate =
                random.nextInt(
                    1,
                    46
                )

            val tooClose =
                luckySet.any {
                    abs(it - candidate) <= 2
                }

            if (!tooClose) {
                luckySet.add(
                    candidate
                )
            }
        }

        /*
         * 혹시 랜덤 조건에서 문제가 생기더라도
         * 안전하게 3개를 완성한다.
         */
        if (luckySet.size < 3) {

            for (
                candidate in 1..45
            ) {

                if (
                    luckySet.size >= 3
                ) break

                if (
                    luckySet.none {
                        abs(
                            it - candidate
                        ) <= 2
                    }
                ) {
                    luckySet.add(
                        candidate
                    )
                }
            }
        }

        val todayLuckyNumbers =
            luckySet
                .take(3)
                .sorted()

        _luckyNumbers.value =
            todayLuckyNumbers

        // ========================================================
        // 행운번호를 반영할 2개 조합 위치 결정
        // ========================================================

        val luckySetPositions =
            when {

                setCount <= 1 ->
                    listOf(0)

                setCount == 2 ->
                    /*
                     * 2개밖에 없으면 두 세트 모두 필요하므로
                     * "비연속" 조건을 적용할 수 없음.
                     *
                     * 다만 화면은 5/10개만 사용하므로
                     * 실제 사용 환경에서는 이 경우가 없다.
                     */
                    listOf(
                        0,
                        1
                    )

                else -> {

                    val first =
                        random.nextInt(
                            0,
                            setCount
                        )

                    val candidates =
                        (0 until setCount)
                            .filter {
                                abs(
                                    it - first
                                ) > 1
                            }

                    val second =
                        candidates[
                            random.nextInt(
                                candidates.size
                            )
                        ]

                    listOf(
                        first,
                        second
                    ).sorted()
                }
            }

        // ========================================================
        // 실제 로또 조합 생성
        // ========================================================

        val generatedSets =
            mutableListOf<List<Int>>()

        for (
            index in 0 until setCount
        ) {

            val setRandom =
                Random(
                    seed +
                        (index + 1) * 100003L
                )

            val resultSet =
                mutableSetOf<Int>()

            /*
             * 행운번호 반영 세트:
             * 행운번호 3개를 모두 넣는다.
             */
            if (
                index in luckySetPositions
            ) {

                resultSet.addAll(
                    todayLuckyNumbers
                )
            }

            /*
             * 나머지 번호는 랜덤으로 채운다.
             */
            while (
                resultSet.size < 6
            ) {

                resultSet.add(
                    setRandom.nextInt(
                        1,
                        46
                    )
                )
            }

            generatedSets.add(
                resultSet
                    .take(6)
                    .sorted()
            )
        }

        _generatedTarotSets.value =
            generatedSets
    }

    // ============================================================
    // 번호 저장
    // ============================================================

    fun saveAllTarotNumbers() {

        val currentSets =
            _generatedTarotSets.value

        if (currentSets.isNotEmpty()) {

            viewModelScope.launch {

                currentSets.forEach { numbers ->

                    repository.insertLotto(
                        numbers,
                        "TAROT"
                    )
                }

                _saveMessage.value =
                    "✓ 내 번호에 저장되었습니다"
            }
        }
    }

    // ============================================================
    // 메시지 초기화
    // ============================================================

    fun clearSaveMessage() {
        _saveMessage.value =
            null
    }
}
