// File Path: app/src/main/java/com/kimro/ai/lotto/ui/analysis/AnalysisViewModel.kt
package com.kimro.ai.lotto.ui.analysis

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
import javax.inject.Inject
import kotlin.random.Random

/**
 * 생성된 번호 조합 1개에 대한 통계 분석 결과.
 * 화면에서 "전문 분석 리포트"처럼 보여주기 위한 지표들을 담는다.
 */
data class LottoSetAnalysis(
    val oddCount: Int,
    val evenCount: Int,
    val lowCount: Int,      // 1~22
    val highCount: Int,     // 23~45
    val hasConsecutive: Boolean,
    val hasTooManySameEndDigits: Boolean,
    val sum: Int,
    val isSumInNormalRange: Boolean,   // 100~175: 역대 당첨번호 총합의 대다수가 속하는 구간
    val occupiedDecadeBins: Int,       // 1-10 / 11-20 / 21-30 / 31-40 / 41-45 중 몇 구간에 분포되어 있는지 (최대 5)
    val acValue: Int,                  // AC(Arithmetic Complexity)값. 0~10, 높을수록 번호 간 규칙성이 낮음(무작위성 높음)
    val isAcGood: Boolean,             // AC값 7 이상이면 "양호"로 판단 (통계적으로 흔히 쓰이는 기준)
    val stdDeviation: Double,          // 6개 번호의 표준편차 - 번호가 얼마나 고르게 퍼져있는지
    val avgGap: Double,                // 정렬된 번호 사이 평균 간격
    val score: Int                     // 0~100 종합 점수
)

/**
 * 번호 조합 하나를 통계적으로 분석해서 점수까지 계산한다.
 * 외부 데이터(실제 당첨번호) 없이, 조합 자체의 패턴만으로 계산 가능한 지표들로만 구성했다.
 */
fun analyzeLottoSet(numbers: List<Int>): LottoSetAnalysis {
    val sorted = numbers.sorted()

    val oddCount = sorted.count { it % 2 != 0 }
    val evenCount = sorted.size - oddCount

    val lowCount = sorted.count { it in 1..22 }
    val highCount = sorted.size - lowCount

    var hasConsecutive = false
    for (i in 0 until sorted.size - 1) {
        if (sorted[i + 1] - sorted[i] == 1) {
            hasConsecutive = true
            break
        }
    }

    val endDigits = sorted.map { it % 10 }
    val hasTooManySameEndDigits = endDigits.groupBy { it }.any { it.value.size >= 3 }

    val sum = sorted.sum()
    val isSumInNormalRange = sum in 100..175

    val decadeBins = sorted.map { (it - 1) / 10 }.distinct().size

    // AC값(Arithmetic Complexity): 6개 번호에서 나올 수 있는 모든 쌍의 차이값 중,
    // 서로 다른 값이 몇 개인지를 센 뒤 5를 뺀 값(6개 숫자의 최소 가능 조합 수가 5이므로).
    // 1,2,3,4,5,6처럼 규칙적인 조합은 0에 가깝고, 무작위성이 높을수록 7~10에 가까워진다.
    val pairwiseDiffs = mutableSetOf<Int>()
    for (i in sorted.indices) {
        for (j in i + 1 until sorted.size) {
            pairwiseDiffs.add(sorted[j] - sorted[i])
        }
    }
    val acValue = (pairwiseDiffs.size - 5).coerceIn(0, 10)
    val isAcGood = acValue >= 7

    // 표준편차: 6개 번호가 평균으로부터 얼마나 퍼져있는지
    val mean = sorted.average()
    val variance = sorted.sumOf { (it - mean) * (it - mean) } / sorted.size
    val stdDeviation = kotlin.math.sqrt(variance)

    // 정렬된 번호 사이의 평균 간격
    val gaps = (0 until sorted.size - 1).map { sorted[it + 1] - sorted[it] }
    val avgGap = gaps.average()

    var score = 0
    score += when (oddCount) {
        3 -> 20
        2, 4 -> 12
        else -> 4
    }
    score += when (lowCount) {
        3 -> 20
        2, 4 -> 12
        else -> 4
    }
    score += if (!hasConsecutive) 15 else 4
    score += if (!hasTooManySameEndDigits) 15 else 8
    score += if (isSumInNormalRange) 8 else 3
    score += if (decadeBins >= 4) 7 else 2
    score += if (isAcGood) 15 else if (acValue >= 4) 8 else 2

    return LottoSetAnalysis(
        oddCount = oddCount,
        evenCount = evenCount,
        lowCount = lowCount,
        highCount = highCount,
        hasConsecutive = hasConsecutive,
        hasTooManySameEndDigits = hasTooManySameEndDigits,
        sum = sum,
        isSumInNormalRange = isSumInNormalRange,
        occupiedDecadeBins = decadeBins,
        acValue = acValue,
        isAcGood = isAcGood,
        stdDeviation = stdDeviation,
        avgGap = avgGap,
        score = score.coerceAtMost(100)
    )
}

// 분석 조건 문구를 상수로 빼서 화면과 ViewModel이 같은 문자열을 참조하게 한다.
// 예전에 문자열을 각자 하드코딩했다가 오타/불일치로 조건이 실제 로직에 반영 안 되는 버그가 있었어서,
// 이제부터는 반드시 이 상수들만 사용한다.
const val CONDITION_ADVANCED = "고도화 종합 분석 (7대 로직 적용)"
const val CONDITION_SAKAI = "사카이 분석 (최근 출현 패턴)"
const val CONDITION_RANDOM = "완전 무작위 추첨 (일반 자동)"
const val CONDITION_AC_FILTER = "AC값(번호 복잡도) 기반 필터링"
const val CONDITION_BALANCE_FILTER = "홀짝 / 고저 균형 필터링"
const val CONDITION_END_DIGIT_FILTER = "끝수 및 연속 번호 조합 제한"

@HiltViewModel
class AnalysisViewModel @Inject constructor(
    application: Application,
    private val repository: LottoRepository
) : AndroidViewModel(application) {

    private val _numberSets = MutableStateFlow<List<List<Int>>>(emptyList())
    val numberSets: StateFlow<List<List<Int>>> = _numberSets.asStateFlow()

    private val _selectedCondition = MutableStateFlow(CONDITION_ADVANCED)
    val selectedCondition: StateFlow<String> = _selectedCondition.asStateFlow()

    private val _saveMessage = MutableStateFlow<String?>(null)
    val saveMessage: StateFlow<String?> = _saveMessage.asStateFlow()

    // 사카이 분석 실행 중 로딩 상태 및 결과 요약 메시지("최근 N주 기준 M개 번호 활용" 등)
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _sakaiInfoMessage = MutableStateFlow<String?>(null)
    val sakaiInfoMessage: StateFlow<String?> = _sakaiInfoMessage.asStateFlow()

    // 즐겨찾는 번호(항상 포함) / 기피 번호(항상 제외) 설정을 앱을 껐다 켜도 유지하기 위해 SharedPreferences에 저장한다.
    private val prefs = application.getSharedPreferences("lotto_number_prefs", Context.MODE_PRIVATE)

    private val _favoriteNumbers = MutableStateFlow(loadNumberSet("favorite_numbers"))
    val favoriteNumbers: StateFlow<Set<Int>> = _favoriteNumbers.asStateFlow()

    private val _excludedNumbers = MutableStateFlow(loadNumberSet("excluded_numbers"))
    val excludedNumbers: StateFlow<Set<Int>> = _excludedNumbers.asStateFlow()

    // 몬테카를로 시뮬레이션은 하루 5회까지 광고 없이 무료로 쓸 수 있고, 그 이후는 매번 광고를 봐야 한다.
    // 자정이 지나면 자동으로 다시 5회가 채워진다. SharedPreferences에 "오늘 날짜"와 "오늘 쓴 횟수"를 저장해서
    // 앱을 껐다 켜도, 하루가 지났는지 정확히 판단한다.
    private val simCreditPrefs = application.getSharedPreferences("sim_credit_prefs", Context.MODE_PRIVATE)
    private val dailyFreeSimLimit = 5

    private val _freeSimUsesToday = MutableStateFlow(0)
    val freeSimUsesToday: StateFlow<Int> = _freeSimUsesToday.asStateFlow()

    init {
        refreshFreeSimUsesForToday()
    }

    private fun todayDateKey(): String =
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())

    /** 저장된 날짜가 오늘이 아니면(자정이 지났으면) 사용 횟수를 0으로 리셋한다. */
    private fun refreshFreeSimUsesForToday() {
        val today = todayDateKey()
        val savedDate = simCreditPrefs.getString("free_sim_date", "")
        if (savedDate != today) {
            simCreditPrefs.edit().putString("free_sim_date", today).putInt("free_sim_used", 0).apply()
            _freeSimUsesToday.value = 0
        } else {
            _freeSimUsesToday.value = simCreditPrefs.getInt("free_sim_used", 0)
        }
    }

    /** 오늘 무료 횟수가 남아있으면 1회 소비하고 true(광고 없이 바로 실행), 다 썼으면 false(광고 시청 필요)를 반환한다. */
    fun consumeFreeSimCredit(): Boolean {
        refreshFreeSimUsesForToday() // 자정을 넘겼을 수도 있으니 매번 날짜를 다시 확인한다.
        return if (_freeSimUsesToday.value < dailyFreeSimLimit) {
            val newCount = _freeSimUsesToday.value + 1
            _freeSimUsesToday.value = newCount
            simCreditPrefs.edit().putInt("free_sim_used", newCount).apply()
            true
        } else {
            false
        }
    }

    private fun loadNumberSet(key: String): Set<Int> {
        val raw = prefs.getString(key, "") ?: ""
        if (raw.isBlank()) return emptySet()
        return raw.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
    }

    private fun saveNumberSet(key: String, numbers: Set<Int>) {
        prefs.edit().putString(key, numbers.joinToString(",")).apply()
    }

    /** 번호를 즐겨찾기에 추가/제거한다. 즐겨찾기는 최대 6개까지만 허용(6개 조합 전부를 못 채우면 안 되므로). */
    fun toggleFavoriteNumber(number: Int) {
        val current = _favoriteNumbers.value
        val newFavorites = if (number in current) {
            current - number
        } else {
            if (current.size >= 6) return
            current + number
        }
        _favoriteNumbers.value = newFavorites
        saveNumberSet("favorite_numbers", newFavorites)

        // 즐겨찾기로 지정하면 기피 목록에서는 자동으로 빠진다 (동시에 둘 다일 수 없음)
        if (number in newFavorites && number in _excludedNumbers.value) {
            val newExcluded = _excludedNumbers.value - number
            _excludedNumbers.value = newExcluded
            saveNumberSet("excluded_numbers", newExcluded)
        }
    }

    /** 번호를 기피 목록에 추가/제거한다. */
    fun toggleExcludedNumber(number: Int) {
        val current = _excludedNumbers.value
        val newExcluded = if (number in current) current - number else current + number
        _excludedNumbers.value = newExcluded
        saveNumberSet("excluded_numbers", newExcluded)

        // 기피로 지정하면 즐겨찾기 목록에서는 자동으로 빠진다
        if (number in newExcluded && number in _favoriteNumbers.value) {
            val newFavorites = _favoriteNumbers.value - number
            _favoriteNumbers.value = newFavorites
            saveNumberSet("favorite_numbers", newFavorites)
        }
    }

    fun setCondition(condition: String) {
        _selectedCondition.value = condition
    }

    /**
     * 사카이 분석: 실제 있었던 최근 26주 당첨 데이터를 기준으로,
     * 통계적 평균 출현 횟수(약 3~4회)에 가까운 번호들을 우선 후보로 삼고,
     * 직전 회차 번호 중 1~2개를 "이월수"로 확률적으로 포함시키는 전통적인 기법이다.
     * ⚠️ 실제 과거 기록을 쓰긴 하지만, 로또는 매회 독립적인 무작위 추첨이라
     * 이 방식이 당첨 확률을 높인다는 통계적 근거는 없다 - 역사적으로 통용되는 "선택 기법"일 뿐이다.
     */
    fun generateSakaiNumbers(setCount: Int) {
        _isGenerating.value = true
        _sakaiInfoMessage.value = null
        viewModelScope.launch {
            try {
                val allDraws = fetchHistoricalDraws()
                val recentWeeks = allDraws.sortedByDescending { it.drawNo }.take(26)

                if (recentWeeks.isEmpty()) {
                    _saveMessage.value = "과거 데이터를 불러오지 못해 사카이 분석을 적용할 수 없습니다."
                    _isGenerating.value = false
                    return@launch
                }

                val favorites = _favoriteNumbers.value
                val excluded = _excludedNumbers.value

                val counts = IntArray(46)
                recentWeeks.forEach { draw -> draw.numbers.forEach { if (it in 1..45) counts[it]++ } }

                val avg = recentWeeks.size * 6 / 45.0
                val lowerBound = (avg - 1).toInt().coerceAtLeast(0)
                val upperBound = (avg + 1).toInt()

                var candidatePool = (1..45).filter { counts[it] in lowerBound..upperBound }
                if (candidatePool.size < 10) candidatePool = (1..45).toList() // 후보가 너무 적으면 안전하게 전체로 대체
                candidatePool = candidatePool.filter { it !in excluded } // 기피 번호는 후보에서 제외

                val lastDrawNumbers = recentWeeks.maxByOrNull { it.drawNo }?.numbers?.filter { it !in excluded } ?: emptyList()

                val generatedSets = mutableListOf<List<Int>>()
                repeat(setCount) {
                    val resultSet = mutableSetOf<Int>()
                    resultSet.addAll(favorites) // 즐겨찾기 번호는 항상 강제 포함

                    // 이월수: 직전 회차 번호 중 1개를 절반 확률로 포함 (사카이 방식의 특징) - 기피 번호는 후보에서 이미 제외됨
                    if (lastDrawNumbers.isNotEmpty() && Random.nextBoolean()) {
                        resultSet.add(lastDrawNumbers.random())
                    }

                    val pool = candidatePool.filter { it !in resultSet }.ifEmpty {
                        (1..45).filter { n -> n !in resultSet && n !in excluded }
                    }
                    val poolIterator = pool.shuffled().iterator()
                    while (resultSet.size < 6 && poolIterator.hasNext()) {
                        resultSet.add(poolIterator.next())
                    }
                    // 후보 풀만으로 6개를 못 채우는 극단적인 경우를 대비한 안전장치 (기피 번호는 여전히 피한다)
                    var fallbackAttempts = 0
                    while (resultSet.size < 6 && fallbackAttempts < 200) {
                        val candidate = (1..45).random()
                        if (candidate !in excluded) resultSet.add(candidate)
                        fallbackAttempts++
                    }

                    generatedSets.add(resultSet.sorted())
                }

                _numberSets.value = generatedSets
                _sakaiInfoMessage.value = "최근 ${recentWeeks.size}주 데이터 기준 · 평균 출현 ${"%.1f".format(avg)}회에 가까운 번호 ${candidatePool.size}개 활용"
            } catch (e: Exception) {
                _saveMessage.value = "과거 데이터를 불러오지 못해 사카이 분석을 적용할 수 없습니다."
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun generateSmartNumbers(setCount: Int) {
        val generatedSets = mutableListOf<List<Int>>()
        val favorites = _favoriteNumbers.value
        val excluded = _excludedNumbers.value
        // 즐겨찾기와 기피 목록을 제외한 나머지 후보 번호 풀
        val candidatePool = (1..45).filter { it !in excluded && it !in favorites }

        // 조합끼리 전부 "저구간 위주"로 비슷하게 나오는 걸 막기 위한 구간 앵커.
        // 각 조합마다 최소 1개는 지정된 구간에서 나오도록 강제해서, setCount개 전체가 골고루 퍼지게 한다.
        // 패턴: [1~15, 1~15, 10~30, 10~30, 전체 무작위] 를 순서대로 반복 (5개면 1번, 10개면 2번 돈다).
        val anchorPattern = listOf(1..15, 1..15, 10..30, 10..30, 1..45)

        for (i in 0 until setCount) {
            var validSet = false
            var resultSet = mutableSetOf<Int>()
            var attempts = 0
            val anchorRange = anchorPattern[i % anchorPattern.size]

            // 즐겨찾기 번호가 홀짝/고저 조건과 충돌하면 무한루프에 빠질 수 있어 시도 횟수에 상한을 둔다.
            while (!validSet && attempts < 500) {
                attempts++
                resultSet.clear()
                resultSet.addAll(favorites) // 즐겨찾기 번호는 항상 포함

                while (resultSet.size < 6 && candidatePool.isNotEmpty()) {
                    val candidate = candidatePool.random()
                    resultSet.add(candidate)
                }

                if (resultSet.size < 6) break // 후보 풀이 부족한 극단적인 경우 - 있는 그대로 사용

                val sortedList = resultSet.sorted()

                val oddCount = sortedList.count { it % 2 != 0 }
                val isOddEvenValid = oddCount in 2..4

                val lowCount = sortedList.count { it in 1..22 }
                val isHighLowValid = lowCount in 2..4

                var hasConsecutive = false
                for (j in 0 until sortedList.size - 1) {
                    if (sortedList[j + 1] - sortedList[j] == 1) {
                        hasConsecutive = true
                        break
                    }
                }

                val endDigits = sortedList.map { it % 10 }
                val hasTooManySameEndDigits = endDigits.groupBy { it }.any { it.value.size >= 3 }

                // 이 조합에 배정된 구간(anchorRange) 안의 번호가 최소 1개는 있는지 확인 (전체 무작위 구간은 항상 통과).
                val hasAnchorNumber = anchorRange == 1..45 || sortedList.any { it in anchorRange }

                if (isOddEvenValid && isHighLowValid && !hasTooManySameEndDigits && hasAnchorNumber) {
                    validSet = true
                }
            }

            generatedSets.add(resultSet.sorted())
        }

        _numberSets.value = generatedSets
    }

    /**
     * 즐겨찾기/기피 번호를 반영하면서, 주어진 조건(validator)을 만족하는 조합을 setCount개 생성하는 공통 로직.
     * "완전 무작위", "AC값 필터링", "홀짝/고저 균형", "끝수·연속 제한" 4가지 조건이 이 함수를 공유한다.
     * validator가 없으면(null) 조건 없이 즉시 채택 - 완전 무작위 추첨용.
     */
    private fun generateWithValidator(setCount: Int, validator: ((List<Int>) -> Boolean)?) {
        val favorites = _favoriteNumbers.value
        val excluded = _excludedNumbers.value
        val candidatePool = (1..45).filter { it !in excluded && it !in favorites }

        val generatedSets = mutableListOf<List<Int>>()
        for (i in 0 until setCount) {
            var validSet = false
            var resultSet = mutableSetOf<Int>()
            var attempts = 0

            while (!validSet && attempts < 500) {
                attempts++
                resultSet.clear()
                resultSet.addAll(favorites)

                while (resultSet.size < 6 && candidatePool.isNotEmpty()) {
                    resultSet.add(candidatePool.random())
                }

                if (resultSet.size < 6) break

                validSet = validator?.invoke(resultSet.sorted()) ?: true
            }

            generatedSets.add(resultSet.sorted())
        }

        _numberSets.value = generatedSets
    }

    /** 완전 무작위 추첨 - 통계적 필터를 전혀 적용하지 않는다 (즐겨찾기/기피 번호만 반영). */
    fun generateRandomNumbers(setCount: Int) {
        generateWithValidator(setCount, validator = null)
    }

    /** AC값(번호 복잡도) 7 이상인 조합만 채택한다. */
    fun generateAcFilteredNumbers(setCount: Int) {
        generateWithValidator(setCount) { sorted ->
            val pairwiseDiffs = mutableSetOf<Int>()
            for (i in sorted.indices) {
                for (j in i + 1 until sorted.size) {
                    pairwiseDiffs.add(sorted[j] - sorted[i])
                }
            }
            val acValue = (pairwiseDiffs.size - 5).coerceIn(0, 10)
            acValue >= 7
        }
    }

    /** 홀짝 정확히 3:3, 고저(1~22 / 23~45) 정확히 3:3인 조합만 채택한다 (기본 로직보다 더 엄격한 균형). */
    fun generateBalancedNumbers(setCount: Int) {
        generateWithValidator(setCount) { sorted ->
            val oddCount = sorted.count { it % 2 != 0 }
            val lowCount = sorted.count { it in 1..22 }
            oddCount == 3 && lowCount == 3
        }
    }

    /** 연속번호가 전혀 없고, 끝자리 숫자가 단 하나도 겹치지 않는 조합만 채택한다. */
    fun generateEndDigitFilteredNumbers(setCount: Int) {
        generateWithValidator(setCount) { sorted ->
            var hasConsecutive = false
            for (j in 0 until sorted.size - 1) {
                if (sorted[j + 1] - sorted[j] == 1) {
                    hasConsecutive = true
                    break
                }
            }
            val endDigits = sorted.map { it % 10 }
            val allEndDigitsDistinct = endDigits.toSet().size == endDigits.size
            !hasConsecutive && allEndDigitsDistinct
        }
    }

    fun saveNumbers() {
        val current = _numberSets.value
        if (current.isNotEmpty()) {
            viewModelScope.launch {
                current.forEach { numbers ->
                    repository.insertLotto(numbers, "ANALYSIS")
                }
                _saveMessage.value =
                    "성공적으로 ${current.size}개의 조합이 내역에 저장되었습니다!"
            }
        }
    }

    /**
     * 조합 하나만 골라서 내역에 저장한다. (전체 저장과 별개로, 마음에 드는 조합만 개별 저장할 때 사용)
     */
    fun saveSingleSet(numbers: List<Int>) {
        viewModelScope.launch {
            repository.insertLotto(numbers, "ANALYSIS")
            _saveMessage.value = "이 조합이 내역에 저장되었습니다!"
        }
    }

    fun saveExternalNumbers(sets: List<List<Int>>) {
        if (sets.isNotEmpty()) {
            viewModelScope.launch {
                sets.forEach { numbers ->
                    repository.insertLotto(numbers, "FORTUNE")
                }
                _saveMessage.value =
                    "성공적으로 ${sets.size}개의 조합이 내역에 저장되었습니다!"
            }
        }
    }

    fun clearSaveMessage() {
        _saveMessage.value = null
    }
}
