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

    private val _saveMessage = MutableStateFlow<String?>(null)
    val saveMessage: StateFlow<String?> = _saveMessage.asStateFlow()

    // 즐겨찾는 번호(항상 포함) / 기피 번호(항상 제외) 설정을 앱을 껐다 켜도 유지하기 위해 SharedPreferences에 저장한다.
    private val prefs = application.getSharedPreferences("lotto_number_prefs", Context.MODE_PRIVATE)

    private val _favoriteNumbers = MutableStateFlow(loadNumberSet("favorite_numbers"))
    val favoriteNumbers: StateFlow<Set<Int>> = _favoriteNumbers.asStateFlow()

    private val _excludedNumbers = MutableStateFlow(loadNumberSet("excluded_numbers"))
    val excludedNumbers: StateFlow<Set<Int>> = _excludedNumbers.asStateFlow()

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

    fun generateSmartNumbers(setCount: Int) {
        val generatedSets = mutableListOf<List<Int>>()
        val favorites = _favoriteNumbers.value
        val excluded = _excludedNumbers.value
        // 즐겨찾기와 기피 목록을 제외한 나머지 후보 번호 풀
        val candidatePool = (1..45).filter { it !in excluded && it !in favorites }

        for (i in 0 until setCount) {
            var validSet = false
            var resultSet = mutableSetOf<Int>()
            var attempts = 0

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
