package com.kimro.ai.lotto.ui.purchase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kimro.ai.lotto.data.PurchaseDao
import com.kimro.ai.lotto.data.PurchaseEntity
import com.kimro.ai.lotto.network.LottoApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject

data class PurchaseItem(
    val id: Long = 0L,
    val round: Int,
    val numbers: List<Int>,
    val rankResult: String = "미확인"
)

@HiltViewModel
class PurchaseViewModel @Inject constructor(
    private val purchaseDao: PurchaseDao // Room DB DAO 주입
) : ViewModel() {

    private val apiService: LottoApiService by lazy {
        Retrofit.Builder()
            .baseUrl(LottoApiService.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LottoApiService::class.java)
    }

    // Room DB의 데이터를 실시간으로 UI용 PurchaseItem 리스트로 변환
    val purchaseList: StateFlow<List<PurchaseItem>> = purchaseDao.getAllPurchases()
        .map { entities ->
            entities.map { entity ->
                PurchaseItem(
                    id = entity.id,
                    round = entity.round,
                    numbers = entity.numbers.split(",").mapNotNull { it.trim().toIntOrNull() },
                    rankResult = entity.rankResult
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 새로운 번호 저장 (번호 생성 화면이나 다이어그램에서 호출)
    fun addPurchaseItem(round: Int, numbers: List<Int>) {
        viewModelScope.launch {
            val numbersString = numbers.joinToString(",")
            val entity = PurchaseEntity(
                round = round,
                numbers = numbersString,
                rankResult = "미확인"
            )
            purchaseDao.insertPurchase(entity)
        }
    }

    // 특정 회차 당첨 결과 확인 및 DB 업데이트
    fun checkWinningResult(id: Long, targetRound: Int, userNumbers: List<Int>) {
        viewModelScope.launch {
            try {
                val response = apiService.getLottoResult(targetRound)
                if (response.returnValue == "success") {
                    val winningNumbers = listOf(
                        response.drwtNo1, response.drwtNo2, response.drwtNo3,
                        response.drwtNo4, response.drwtNo5, response.drwtNo6
                    )
                    val bonusNo = response.bnusNo

                    val rank = calculateRank(userNumbers, winningNumbers, bonusNo)
                    
                    // DB에 결과 업데이트
                    purchaseDao.updateRankResult(id, rank)
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
