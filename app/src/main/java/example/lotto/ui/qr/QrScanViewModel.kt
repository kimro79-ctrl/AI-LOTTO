// File Path: app/src/main/java/com/kimro/ai/lotto/ui/qr/QrScanViewModel.kt
package com.kimro.ai.lotto.ui.qr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kimro.ai.lotto.data.repository.LottoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QrScanViewModel @Inject constructor(
    private val repository: LottoRepository
) : ViewModel() {

    private val _savedCount = MutableStateFlow(0)
    val savedCount: StateFlow<Int> = _savedCount.asStateFlow()

    private val _saveMessage = MutableStateFlow<String?>(null)
    val saveMessage: StateFlow<String?> = _saveMessage.asStateFlow()

    /**
     * QR에서 파싱된 (회차, 번호6개) 목록을 히스토리에 저장한다.
     * type을 "QR"로 저장하기 때문에, 나중에 자동 당첨 확인 기능은
     * type == "QR"이면서 round > 0인 항목만 대상으로 삼으면 된다.
     */
    fun saveScannedGames(games: List<Pair<Int, List<Int>>>) {
        if (games.isEmpty()) return
        viewModelScope.launch {
            games.forEach { (round, numbers) ->
                repository.insertLotto(numbers, "QR", round)
            }
            _savedCount.value += games.size
            _saveMessage.value = "${games.size}게임이 내역에 저장되었습니다 (${games.first().first}회차)"
        }
    }

    fun clearSaveMessage() {
        _saveMessage.value = null
    }
}
