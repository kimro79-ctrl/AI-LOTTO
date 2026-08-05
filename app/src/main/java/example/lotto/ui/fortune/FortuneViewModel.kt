// File Path: app/src/main/java/com/example/lotto/ui/fortune/FortuneViewModel.kt
package com.example.lotto.ui.fortune

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lotto.data.repository.LottoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class FortuneViewModel @Inject constructor(
    private val repository: LottoRepository
) : ViewModel() {

    private val _fortuneText = MutableStateFlow("")
    val fortuneText: StateFlow<String> = _fortuneText.asStateFlow()

    private val _numbers = MutableStateFlow<List<Int>>(emptyList())
    val numbers: StateFlow<List<Int>> = _numbers.asStateFlow()

    fun generateFortuneNumbers(birthDate: String) {
        val dateValue = birthDate.toIntOrNull() ?: 0
        val dayOfYear = LocalDate.now().dayOfYear
        val seed = dateValue + dayOfYear
        val random = Random(seed)

        _fortuneText.value = when (random.nextInt(1, 5)) {
            1 -> "금전운이 강하게 트이는 날입니다. 직관에 따른 번호를 추천합니다."
            2 -> "안정적인 기운이 감도는 날입니다. 균형 잡힌 번호 조합입니다."
            3 -> "변화의 기운이 큽니다. 평소에 선택하지 않던 숫자가 행운을 가져옵니다."
            else -> "귀인의 도움이 있는 날입니다. 주변의 기운과 잘 어우러집니다."
        }

        val resultSet = mutableSetOf<Int>()
        while (resultSet.size < 6) {
            resultSet.add(random.nextInt(1, 46))
        }
        _numbers.value = resultSet.sorted()
    }

    fun saveFortuneNumbers() {
        val current = _numbers.value
        if (current.size == 6) {
            viewModelScope.launch {
                repository.insertLotto(current, "FORTUNE")
            }
        }
    }
}

