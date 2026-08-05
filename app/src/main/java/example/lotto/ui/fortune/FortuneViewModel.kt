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
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class FortuneViewModel @Inject constructor(
    private val repository: LottoRepository
) : ViewModel() {

    private val _fortuneTitle = MutableStateFlow<String?>(null)
    val fortuneTitle: StateFlow<String?> = _fortuneTitle.asStateFlow()

    private val _fortuneResult = MutableStateFlow<String?>(null)
    val fortuneResult: StateFlow<String?> = _fortuneResult.asStateFlow()

    private val _fortuneDetails = MutableStateFlow<String?>(null)
    val fortuneDetails: StateFlow<String?> = _fortuneDetails.asStateFlow()

    private val _generatedFortuneSets = MutableStateFlow<List<List<Int>>>(emptyList())
    val generatedFortuneSets: StateFlow<List<List<Int>>> = _generatedFortuneSets.asStateFlow()

    private val _saveMessage = MutableStateFlow<String?>(null)
    val saveMessage: StateFlow<String?> = _saveMessage.asStateFlow()

    private val heavenlyStems = listOf("갑", "을", "병", "정", "무", "기", "경", "신", "임", "계")
    private val earthlyBranches = listOf("자", "축", "인", "묘", "진", "사", "오", "미", "신", "유", "술", "해")

    fun calculateSajuFortune(birthInput: String, setCount: Int) {
        if (birthInput.length != 8) return

        val year = birthInput.substring(0, 4).toIntOrNull() ?: 2000
        val month = birthInput.substring(4, 6).toIntOrNull() ?: 1
        val day = birthInput.substring(6, 8).toIntOrNull() ?: 1

        val yearStem = heavenlyStems[(year - 4) % 10]
        val yearBranch = earthlyBranches[(year - 4) % 12]
        val monthStem = heavenlyStems[(year * 2 + month) % 10]
        val monthBranch = earthlyBranches[(month + 1) % 12]
        val dayStemIndex = (year * 365 + day) % 10
        val dayBranchIndex = (year * 365 + day) % 12
        
        val dayStem = heavenlyStems[dayStemIndex]
        val dayBranch = earthlyBranches[dayBranchIndex]

        val dominantElement = when (dayStem) {
            "갑", "을" -> "목(木) - 생동하는 성장과 재물"
            "병", "정" -> "화(火) - 뻗어나가는 열정과 행운"
            "무", "기" -> "토(土) - 든든한 안정과 횡재수"
            "경", "신" -> "금(金) - 예리한 직관과 결실"
            else -> "수(水) - 지혜로운 유연함과 재물 흐름"
        }

        // 수정됨: 변수와 한글 조사가 붙어있어 중괄호로 정상 처리
        _fortuneTitle.value = "[${year}년 ${month}월 ${day}일생 만세력 사주 원국]"
        _fortuneResult.value = "사주 일간(日干)이 $dayStem(${dominantElement})의 기운을 받아 오늘은 재물과 직관력이 크게 열리는 형국입니다."
        _fortuneDetails.value = "연주(${yearStem}${yearBranch})와 월주(${monthStem}${monthBranch}), 일주(${dayStem}${dayBranch})의 오행 조화가 원만하여 과감한 선택이 좋은 결과로 이어집니다."

        val seed = birthInput.toInt() + dayStemIndex * 100 + dayBranchIndex
        val sets = mutableListOf<List<Int>>()
        for (i in 0 until setCount) {
            val resultSet = mutableSetOf<Int>()
            val setRandom = Random(seed + i * 73 + System.currentTimeMillis())

            while (resultSet.size < 6) {
                resultSet.add(setRandom.nextInt(1, 46))
            }
            sets.add(resultSet.sorted())
        }
        _generatedFortuneSets.value = sets
    }

    fun saveAllFortuneNumbers() {
        val currentSets = _generatedFortuneSets.value
        if (currentSets.isNotEmpty()) {
            viewModelScope.launch {
                currentSets.forEach { numbers ->
                    repository.insertLotto(numbers, "FORTUNE")
                }
                _saveMessage.value = "성공적으로 ${currentSets.size}개의 운세 조합이 내역에 저장되었습니다!"
            }
        }
    }

    fun clearSaveMessage() {
        _saveMessage.value = null
    }
}
