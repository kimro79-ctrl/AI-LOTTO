// File Path: app/src/main/java/com/example/lotto/ui/analysis/AnalysisViewModel.kt
package com.example.lotto.ui.analysis

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class AnalysisViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    // 내역 화면과 동일한 SharedPreferences 파일명 및 키 사용
    private val sharedPreferences = application.getSharedPreferences("lotto_saved_prefs", Context.MODE_PRIVATE)

    private val _numberSets = MutableStateFlow<List<List<Int>>>(emptyList())
    val numberSets: StateFlow<List<List<Int>>> = _numberSets.asStateFlow()

    private val _selectedCondition = MutableStateFlow("홀짝 비율 균형 (3:3)")
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

            when {
                condition.contains("홀짝 비율 균형") -> {
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
            }

            while (resultSet.size < 6) {
                resultSet.add(Random.nextInt(1, 46))
            }
            generatedSets.add(resultSet.sorted())
        }

        _numberSets.value = generatedSets
    }

    // 메인 화면 번호 저장 기능 (SharedPreferences에 정확한 키로 누적 저장)
    fun saveNumbers() {
        val current = _numberSets.value
        if (current.isNotEmpty()) {
            val existing = sharedPreferences.getStringSet("saved_number_sets", mutableSetOf()) ?: mutableSetOf()
            val mutableSet = existing.toMutableSet()
            
            current.forEach { numbers ->
                mutableSet.add(numbers.joinToString(","))
            }
            
            // apply() 대신 commit()을 사용하여 즉시 디스크에 반영되도록 보장
            sharedPreferences.edit().putStringSet("saved_number_sets", mutableSet).commit()
            _saveMessage.value = "성공적으로 ${current.size}개의 조합이 저장되었습니다!"
        }
    }

    // 운세 화면 등 외부에서 직접 번호 세트를 전달받아 저장할 수 있는 공용 함수
    fun saveExternalNumbers(sets: List<List<Int>>) {
        if (sets.isNotEmpty()) {
            val existing = sharedPreferences.getStringSet("saved_number_sets", mutableSetOf()) ?: mutableSetOf()
            val mutableSet = existing.toMutableSet()
            
            sets.forEach { numbers ->
                mutableSet.add(numbers.joinToString(","))
            }
            
            sharedPreferences.edit().putStringSet("saved_number_sets", mutableSet).commit()
            _saveMessage.value = "성공적으로 ${sets.size}개의 조합이 저장되었습니다!"
        }
    }

    fun clearSaveMessage() {
        _saveMessage.value = null
    }
}
