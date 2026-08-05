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

    private val _includeInput = MutableStateFlow("")
    val includeInput: StateFlow<String> = _includeInput.asStateFlow()

    private val _excludeInput = MutableStateFlow("")
    val excludeInput: StateFlow<String> = _excludeInput.asStateFlow()

    // 최신 당첨 번호 상태 (기본값 설정 후 API로 자동 갱신)
    private val _latestWinNumbers = MutableStateFlow(listOf(6, 7, 11, 15, 39, 43))
    val latestWinNumbers: StateFlow<List<Int>> = _latestWinNumbers.asStateFlow()

    init {
        fetchLatestLottoNumber()
    }

    fun setIncludeInput(value: String) {
        _includeInput.value = value
    }

    fun setExcludeInput(value: String) {
        _excludeInput.value = value
    }

    // 동행복권 공식 API를 호출하여 가장 최신 당첨 번호와 회차를 자동으로 가져옴
    private fun fetchLatestLottoNumber() {
        viewModelScope.launch {
            try {
                // 대략적인 최신 회차 추정 후 동적으로 최신 회차 탐색 또는 최근 회차 조회 API 활용
                // 여기서는 가장 최신 회차 번호(예: 1235회 이상)를 역산하거나 최신 데이터를 가져오는 로직 수행
                val targetRound = 1235 // 필요에 따라 현재 날짜 기준 계산 혹은 최신 회차 연동
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
                // 네트워크 오류 시 기본값 유지
            }
        }
    }

    fun generateSmartNumbers(countMode: Int) {
        val includeList = _includeInput.value
            .split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 1..45 }

        val excludeList = _excludeInput.value
            .split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 1..45 }

        val generatedSets = mutableListOf<List<Int>>()
        
        for (i in 0 until 5) {
            val resultSet = mutableSetOf<Int>()
            resultSet.addAll(includeList.take(countMode))

            while (resultSet.size < countMode) {
                val candidate = Random.nextInt(1, 46)
                if (!excludeList.contains(candidate)) {
                    resultSet.add(candidate)
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
                // 저장 로직 구현
            }
        }
    }
}
