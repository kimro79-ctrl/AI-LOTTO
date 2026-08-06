package com.kimro.ai.lotto.ui.purchase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kimro.ai.lotto.network.LottoApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

data class PurchaseItem(
    val round: Int,
    val numbers: List<Int>,
    val rankResult: String = "미확인"
)

class PurchaseViewModel : ViewModel() {

    private val apiService: LottoApiService by lazy {
        Retrofit.Builder()
            .baseUrl(LottoApiService.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LottoApiService::class.java)
    }

    private val _purchaseList = MutableStateFlow<List<PurchaseItem>>(emptyList())
    val purchaseList: StateFlow<List<PurchaseItem>> = _purchaseList

    fun loadPurchasedNumbers(savedItems: List<PurchaseItem>) {
        _purchaseList.value = savedItems
    }

    fun checkWinningResult(targetRound: Int) {
        viewModelScope.launch {
            try {
                val response = apiService.getLottoResult(targetRound)
                if (response.returnValue == "success") {
                    val winningNumbers = listOf(
                        response.drwtNo1, response.drwtNo2, response.drwtNo3,
                        response.drwtNo4, response.drwtNo5, response.drwtNo6
                    )
                    val bonusNo = response.bnusNo

                    val updatedList = _purchaseList.value.map { item ->
                        if (item.round == targetRound) {
                            val rank = calculateRank(item.numbers, winningNumbers, bonusNo)
                            item.copy(rankResult = rank)
                        } else {
                            item
                        }
                    }
                    _purchaseList.value = updatedList
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun calculateRank(userNumbers: List<Int>, winningNumbers: List<Int>, bonusNo: Int): String {
        val matchCount = userNumbers.intersect(winningNumbers.toSet()).size
        val hasBonus = userNumbers.contains(bonusNo)

        return when (matchCount) {
            6 -> "1등 당첨! 🎉"
            5 -> if (hasBonus) "2등 당첨! 🎊" else "3등 당첨!"
            4 -> "4등 당첨 (5만 원)"
            3 -> "5등 당첨 (5천 원)"
            else -> "낙첨"
        }
    }
}
