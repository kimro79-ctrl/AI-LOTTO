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

    private val _numbers = MutableStateFlow<List<Int>>(emptyList())
    val numbers: StateFlow<List<Int>> = _numbers.asStateFlow()

    private val _includeInput = MutableStateFlow("")
    val includeInput: StateFlow<String> = _includeInput.asStateFlow()

    private val _excludeInput = MutableStateFlow("")
    val excludeInput: StateFlow<String> = _excludeInput.asStateFlow()

    fun setIncludeInput(value: String) {
        _includeInput.value = value
    }

    fun setExcludeInput(value: String) {
        _excludeInput.value = value
    }

    fun generateSmartNumbers() {
        val includeList = _includeInput.value
            .split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 1..45 }

        val excludeList = _excludeInput.value
            .split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 1..45 }

        val resultSet = mutableSetOf<Int>()
        resultSet.addAll(includeList.take(10))

        while (resultSet.size < 10) {
            val candidate = Random.nextInt(1, 46)
            if (!excludeList.contains(candidate)) {
                resultSet.add(candidate)
            }
        }

        _numbers.value = resultSet.sorted()
    }

    fun saveNumbers() {
        val current = _numbers.value
        if (current.isNotEmpty()) {
            viewModelScope.launch {
                // 저장 로직
            }
        }
    }
}
