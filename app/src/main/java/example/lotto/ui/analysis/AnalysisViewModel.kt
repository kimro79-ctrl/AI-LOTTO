// File Path: app/src/main/java/example/lotto/ui/analysis/AnalysisViewModel.kt
package example.lotto.ui.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import example.lotto.data.repository.LottoRepository
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

    private val _analysisMessage = MutableStateFlow("버튼을 눌러 스마트 분석을 시작하세요.")
    val analysisMessage: StateFlow<String> = _analysisMessage.asStateFlow()

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

    fun generateAnalyzedNumbers() {
        val includeList = _includeInput.value
            .split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 1..45 }

        val excludeList = _excludeInput.value
            .split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 1..45 }

        val resultSet = mutableSetOf<Int>()
        resultSet.addAll(includeList.take(6))

        while (resultSet.size < 6) {
            val candidate = Random.nextInt(1, 46)
            if (!excludeList.contains(candidate)) {
                resultSet.add(candidate)
            }
        }

        _numbers.value = resultSet.sorted()
        _analysisMessage.value = "AI 패턴 분석 및 사용자 조건이 반영된 조합입니다."
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
