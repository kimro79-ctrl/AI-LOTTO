// File Path: app/src/main/java/example/lotto/ui/analysis/AnalysisViewModel.kt
package com.example.lotto.ui.analysis

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lotto.data.local.LottoEntity
import com.example.lotto.data.repository.LottoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.random.Random

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

    private val _saveMessage = MutableStateFlow<String?>(null)
    val saveMessage: StateFlow<String?> = _saveMessage.asStateFlow()

    init {
        fetchLatestLottoNumber()
    }

    fun setCondition(condition: String) {
        _selectedCondition.value = condition
    }

    private fun fetchLatestLottoNumber() {
        viewModelScope.launch {
            try {
                val targetRound = 1235
                val urlString = "https://www.dhlottery.co.kr/common.do?method=getLottoNumber&drwNo=$targetRound"
                val responseJson = withContext(Dispatchers.IO) {
                    URL(urlString).readText()
                }

                val jsonObject = JSONObject(responseJson as String)
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
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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
                val currentDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                current.forEach { numbers ->
                    val entity = LottoEntity(
                        type = "ANALYSIS",
                        numbers = numbers.joinToString(","),
                        date = currentDate
                    )
                    repository.insertLotto(entity)
                }
                _saveMessage.value = "성공적으로 ${current.size}개의 조합이 내역에 저장되었습니다!"
            }
        }
    }

    fun saveExternalNumbers(sets: List<List<Int>>) {
        if (sets.isNotEmpty()) {
            viewModelScope.launch {
                val currentDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                sets.forEach { numbers ->
                    val entity = LottoEntity(
                        type = "FORTUNE",
                        numbers = numbers.joinToString(","),
                        date = currentDate
                    )
                    repository.insertLotto(entity)
                }
                _saveMessage.value = "성공적으로 ${sets.size}개의 조합이 내역에 저장되었습니다!"
            }
        }
    }

    fun clearSaveMessage() {
        _saveMessage.value = null
    }
}
