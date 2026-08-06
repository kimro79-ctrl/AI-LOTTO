package com.kimro.ai.lotto.ui.purchase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kimro.ai.lotto.data.entity.PurchaseEntity
import com.kimro.ai.lotto.data.repository.PurchaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PurchaseViewModel @Inject constructor(
    private val repository: PurchaseRepository
) : ViewModel() {

    // UI에서 관찰할 전체 구매/저장 내역 (최신순)
    val purchaseItems: StateFlow<List<PurchaseEntity>> = repository.getAllPurchases()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * 1. 수동 구매(생성) 전용 메서드
     * 사용자가 직접 번호를 생성해서 담을 때 호출합니다.
     */
    fun createManualPurchase(round: Int, numbers: List<Int>) {
        val numbersString = numbers.joinToString(", ")
        val entity = PurchaseEntity(
            round = round,
            numbers = numbersString,
            rankResult = "미확인"
        )
        viewModelScope.launch {
            repository.insertPurchase(entity)
        }
    }

    /**
     * 2. QR 스캔 저장 전용 메서드
     * 카메라로 QR 코드를 인식하여 자동으로 저장할 때 호출합니다.
     */
    fun addPurchaseItem(round: Int, numbers: List<Int>) {
        val numbersString = numbers.joinToString(", ")
        val entity = PurchaseEntity(
            round = round,
            numbers = numbersString,
            rankResult = "미확인"
        )
        viewModelScope.launch {
            repository.insertPurchase(entity)
        }
    }

    /**
     * 3. 삭제 기능 메서드
     * 잘못 저장되거나 필요 없는 내역을 항목 단위로 삭제합니다.
     */
    fun deletePurchase(purchase: PurchaseEntity) {
        viewModelScope.launch {
            repository.deletePurchase(purchase)
        }
    }

    /**
     * ID를 이용한 삭제 메서드
     */
    fun deletePurchaseById(id: Long) {
        viewModelScope.launch {
            repository.deletePurchaseById(id)
        }
    }
}
