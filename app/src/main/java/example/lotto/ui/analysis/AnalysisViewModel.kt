// File Path: app/src/main/java/com/example/lotto/ui/analysis/AnalysisViewModel.kt
package com.example.lotto.ui.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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

    // 1235회 실제 당첨 번호 반영
    private val _latestWinNumbers = MutableStateFlow(listOf(6, 7, 11, 15, 39, 43))
    val latestWinNumbers: StateFlow<List<Int>> = _latestWinNumbers.asStateFlow()

    fun setIncludeInput(value: String) {
        _includeInput.value = value
    }

    fun setExcludeInput(value: String) {
        _excludeInput.value = value
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
