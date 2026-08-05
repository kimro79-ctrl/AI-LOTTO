// File Path: app/src/main/java/com/example/lotto/ui/analysis/AnalysisViewModel.kt
package com.example.lotto.ui.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class AnalysisViewModel @Inject constructor() : ViewModel() {

    private val _numberSets = MutableStateFlow<List<List<Int>>>(emptyList())
    val numberSets: StateFlow<List<List<Int>>> = _numberSets.asStateFlow()

    // 조건 선택형 상태 (예: "홀짝 비율 균형", "고정수 포함(1고정)", "고정수 포함(2고정)", "이월수 집중")
    private val _selectedCondition = MutableStateFlow("홀짝 비율 균형 (3:3)")
    val selectedCondition: StateFlow<String> = _selectedCondition.asStateFlow()

    private val _latestWinNumbers = MutableStateFlow(listOf(6, 7, 11, 15, 39, 43))
    val latestWinNumbers: StateFlow<List<Int>> = _latestWinNumbers.asStateFlow()

    // 저장 완료 메시지 또는 상태
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
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun generateSmartNumbers(setCount: Int) {
        val condition = _selectedCondition.value
        val generatedSets = mutableListOf<List<Int>>()

        for (i in 0 until setCount) {
            val resultSet = mutableSetOf<Int>()

            // 선택된 조건에 따른 알고리즘 분기
            when {
                condition.contains("홀짝 비율 균형") -> {
                    // 홀수 3개, 짝수 3개 유도
                    val odds = (1..45).filter { it % 2 != 0 }.shuffled().take(3)
                    val evens = (1..45).filter { it % 2 == 0 }.shuffled().take(3)
                    resultSet.addAll(odds)
                    resultSet.addAll(evens)
                }
                condition.contains("고정수 포함(7, 14)") -> {
                    resultSet.add(7)
                    resultSet.add(14)
                }
                condition.contains("고정수 포함(1, 45)") -> {
                    resultSet.add(1)
                    resultSet.add(45)
                }
                else -> {
                    // 기본 무작위
                }
            }

            while (resultSet.size < 6) {
                resultSet.add(Random.nextInt(1, 46))
            }
            generatedSets.add(resultSet.sorted())
        }

        _numberSets.value = generatedSets
    }

    // 번호 저장 기능 구현 (Room DB 또는 SharedPreferences 연동 확장 포인트)
    fun saveNumbers() {
        val current = _numberSets.value
        if (current.isNotEmpty()) {
            viewModelScope.launch {
                // TODO: 실제 DB 저장소 연결 처리
                _saveMessage.value = "성공적으로 ${current.size}개의 조합이 저장되었습니다!"
            }
        }
    }

    fun clearSaveMessage() {
        _saveMessage.value = null
    }
}
