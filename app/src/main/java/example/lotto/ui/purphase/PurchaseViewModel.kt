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

    val purchaseItems: StateFlow<List<PurchaseEntity>> = repository.getAllPurchases()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

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

    fun deletePurchase(purchase: PurchaseEntity) {
        viewModelScope.launch {
            repository.deletePurchase(purchase)
        }
    }
}
