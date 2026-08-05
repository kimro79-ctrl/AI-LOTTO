// File Path: app/src/main/java/com/example/lotto/ui/analysis/AnalysisViewModel.kt
package com.example.lotto.ui.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lotto.data.repository.LottoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class AnalysisViewModel @Inject constructor(
    private val repository: LottoRepository
) : ViewModel() {

    private val _numbers = MutableStateFlow<List<Int>>(emptyList())
    val numbers: StateFlow<List<Int>> = _numbers.asStateFlow()

    fun generateSmartNumbers(
        excludeNumbers: List<Int> = emptyList(),
        fixedNumbers: List<Int> = emptyList()
    ) {
        val result = mutableSetOf<Int>()
        result.addAll(fixedNumbers)

        while (result.size < 6) {
            val nextNum = Random.nextInt(1, 46)
            if (nextNum !in excludeNumbers) {
                result.add(nextNum)
            }
        }

        val sortedList = result.sorted()
        _numbers.value = sortedList
    }

    fun saveNumbers() {
        val current = _numbers.value
        if (current.size == 6) {
            viewModelScope.launch {
                repository.insertLotto(current, "ANALYSIS")
            }
        }
    }
}

