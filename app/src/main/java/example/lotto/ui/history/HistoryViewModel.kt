// File Path: app/src/main/java/com/example/lotto/ui/history/HistoryViewModel.kt
package com.example.lotto.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lotto.data.local.LottoEntity
import com.example.lotto.data.repository.LottoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: LottoRepository
) : ViewModel() {

    val historyList: StateFlow<List<LottoEntity>> = repository.getAllHistory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun deleteHistory(id: Long) {
        viewModelScope.launch {
            repository.deleteLotto(id)
        }
    }

    // 전체 삭제 함수 수정: 리포지토리를 거치지 않고 개별 항목을 순회하며 삭제하거나 직접 처리
    fun deleteAllHistory() {
        viewModelScope.launch {
            // 현재 리스트에 있는 모든 항목의 id를 가져와서 개별 삭제 함수를 실행합니다.
            historyList.value.forEach { item ->
                repository.deleteLotto(item.id)
            }
        }
    }
}
