// File Path: app/src/main/java/com/kimro/ai/lotto/ui/analysis/AnalysisViewModel.kt
package com.kimro.ai.lotto.ui.analysis

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kimro.ai.lotto.data.repository.LottoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.util.Calendar
import java.util.TimeZone
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
    val carryOverNumbers: List<Int>,   // 직전 회차 당첨번호와 겹치는 번호(이월수)
    val occupiedDecadeBins: Int,       // 1-10 / 11-20 / 21-30 / 31-40 / 41-45 중 몇 구간에 분포되어 있는지 (최대 5)
    val score: Int                     // 0~100 종합 점수
)

/**
 * 번호 조합 하나를 통계적으로 분석해서 점수까지 계산한다.
 * 실시간 DB 조회 없이 조합 자체의 패턴만으로 계산 가능한 지표들로 구성했다.
 */
fun analyzeLottoSet(numbers: List<Int>, latestWinNumbers: List<Int>): LottoSetAnalysis {
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

    val carryOverNumbers = sorted.filter { it in latestWinNumbers }

    val decadeBins = sorted.map { (it - 1) / 10 }.distinct().size

    var score = 0
    score += when (oddCount) {
        3 -> 25
        2, 4 -> 15
        else -> 5
    }
    score += when (lowCount) {
        3 -> 25
        2, 4 -> 15
        else -> 5
    }
    score += if (!hasConsecutive) 20 else 5
    score += if (!hasTooManySameEndDigits) 20 else 10
    score += if (isSumInNormalRange) 10 else 5
    score += if (decadeBins >= 4) 5 else 0

    return LottoSetAnalysis(
        oddCount = oddCount,
        evenCount = evenCount,
        lowCount = lowCount,
        highCount = highCount,
        hasConsecutive = hasConsecutive,
        hasTooManySameEndDigits = hasTooManySameEndDigits,
        sum = sum,
        isSumInNormalRange = isSumInNormalRange,
        carryOverNumbers = carryOverNumbers,
        occupiedDecadeBins = decadeBins,
        score = score.coerceAtMost(100)
    )
}

@HiltViewModel
class AnalysisViewModel @Inject constructor(
    application: Application,
    private val repository: LottoRepository
) : AndroidViewModel(application) {

    private val _numberSets = MutableStateFlow<List<List<Int>>>(emptyList())
    val numberSets: StateFlow<List<List<Int>>> = _numberSets.asStateFlow()

    private val _selectedCondition = MutableStateFlow("고도화 종합 분석 (7대 로직 적용)")
    val selectedCondition: StateFlow<String> = _selectedCondition.asStateFlow()

    private val _latestWinNumbers = MutableStateFlow(listOf(6, 7, 11, 15, 39, 43))
    val latestWinNumbers: StateFlow<List<Int>> = _latestWinNumbers.asStateFlow()

    // 실제로 조회에 성공한 회차 번호. 화면 상단 배너의 "OOOO회 당첨 번호" 표시에 사용된다.
    private val _latestRound = MutableStateFlow<Int?>(null)
    val latestRound: StateFlow<Int?> = _latestRound.asStateFlow()

    // 여러 번 재시도해도 최신 회차를 못 가져온 경우 true. 화면에서 "불러오는 중" 대신 실패 안내로 전환하는 데 쓴다.
    private val _latestRoundFetchFailed = MutableStateFlow(false)
    val latestRoundFetchFailed: StateFlow<Boolean> = _latestRoundFetchFailed.asStateFlow()

    // 실패했을 때 실제로 무슨 예외가 발생했는지(네트워크 오류/파싱 오류 등) 화면에서 바로 확인하기 위한 디버그용 메시지
    private val _lastFetchErrorDebug = MutableStateFlow<String?>(null)
    val lastFetchErrorDebug: StateFlow<String?> = _lastFetchErrorDebug.asStateFlow()

    private val _saveMessage = MutableStateFlow<String?>(null)
    val saveMessage: StateFlow<String?> = _saveMessage.asStateFlow()

    init {
        fetchLatestLottoNumber()
    }

    fun setCondition(condition: String) {
        _selectedCondition.value = condition
    }

    fun retryFetchLatestLottoNumber() {
        fetchLatestLottoNumber()
    }

    private fun fetchLatestLottoNumber() {
        _latestRoundFetchFailed.value = false
        _lastFetchErrorDebug.value = null
        viewModelScope.launch {
            var round = estimateLatestRound()
            var attemptsLeft = 15 // 추정 회차가 실제와 몇 회 차이나도 안전하게 찾아내도록 넉넉히 재시도
            var succeeded = false
            var lastError: String? = null

            while (attemptsLeft > 0 && !succeeded) {
                try {
                    val urlString = "https://www.dhlottery.co.kr/common.do?method=getLottoNumber&drwNo=$round"
                    val responseJson = withContext(Dispatchers.IO) {
                        URL(urlString).readText()
                    }

                    val jsonObject = JSONObject(responseJson)
                    if (jsonObject.getString("returnValue") == "success") {
                        val nums = listOf(
                            jsonObject.getInt("drwtNo1"),
                            jsonObject.getInt("drwtNo2"),
                            jsonObject.getInt("drwtNo3"),
                            jsonObject.getInt("drwtNo4"),
                            jsonObject.getInt("drwtNo5"),
                            jsonObject.getInt("drwtNo6")
                        )
                        _latestWinNumbers.value = nums
                        _latestRound.value = round
                        succeeded = true
                    } else {
                        // 아직 발표되지 않은 회차 -> 한 회차 낮춰서 다시 시도
                        lastError = "회차 $round 응답: returnValue=fail (해당 회차 미발표 또는 존재하지 않음)"
                        round -= 1
                        attemptsLeft -= 1
                    }
                } catch (e: Exception) {
                    // 네트워크 오류 등 - 더 낮은 회차로도 재시도해보되, 무한정 돌지 않도록 시도 횟수는 소모한다
                    lastError = "회차 $round 요청 중 예외: ${e.javaClass.simpleName} - ${e.message}"
                    e.printStackTrace()
                    round -= 1
                    attemptsLeft -= 1
                }
            }

            if (!succeeded) {
                // 끝까지 실패하면 "불러오는 중..."에 무한정 머무르지 않도록 실패 상태를 명확히 표시한다
                _latestRoundFetchFailed.value = true
                _lastFetchErrorDebug.value = lastError
            }
        }
    }

    /**
     * 로또 1회차 추첨일(2002-12-07, 매주 토요일 추첨)을 기준으로
     * 오늘 날짜까지 몇 주가 지났는지 계산해서 "현재 최신 회차"를 추정한다.
     * 실제 발표 여부는 fetchLatestLottoNumber에서 API 응답으로 재확인하고,
     * 아직 발표 전이면 한 회차씩 낮춰가며 보정한다.
     */
    private fun estimateLatestRound(): Int {
        val seoulTimeZone = TimeZone.getTimeZone("Asia/Seoul")

        val baseCalendar = Calendar.getInstance(seoulTimeZone).apply {
            set(2002, Calendar.DECEMBER, 7, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val nowCalendar = Calendar.getInstance(seoulTimeZone)

        val diffMillis = nowCalendar.timeInMillis - baseCalendar.timeInMillis
        val diffWeeks = (diffMillis / (7L * 24 * 60 * 60 * 1000)).toInt()

        return (diffWeeks + 1).coerceAtLeast(1)
    }

    fun generateSmartNumbers(setCount: Int) {
        val generatedSets = mutableListOf<List<Int>>()
        val latestWin = _latestWinNumbers.value

        for (i in 0 until setCount) {
            var validSet = false
            var resultSet = mutableSetOf<Int>()

            while (!validSet) {
                resultSet.clear()

                if (latestWin.isNotEmpty() && Random.nextBoolean()) {
                    val carryOver = latestWin.random()
                    resultSet.add(carryOver)
                }

                val recentExcluded = latestWin.toSet()
                val candidatePool = (1..45).filter { it !in recentExcluded }

                while (resultSet.size < 6) {
                    val candidate = candidatePool.random()
                    resultSet.add(candidate)
                }

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

                if (isOddEvenValid && isHighLowValid && !hasTooManySameEndDigits) {
                    validSet = true
                }
            }

            generatedSets.add(resultSet.sorted())
        }

        _numberSets.value = generatedSets
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
