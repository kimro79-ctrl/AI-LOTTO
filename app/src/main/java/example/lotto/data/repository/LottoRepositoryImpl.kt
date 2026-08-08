// File Path: app/src/main/java/com/kimro/ai/lotto/data/repository/LottoRepositoryImpl.kt
package com.kimro.ai.lotto.data.repository

import com.kimro.ai.lotto.data.local.LottoDao
import com.kimro.ai.lotto.data.local.LottoEntity
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class LottoRepositoryImpl @Inject constructor(
    private val lottoDao: LottoDao
) : LottoRepository {

    override fun getAllHistory(): Flow<List<LottoEntity>> {
        return lottoDao.getAllHistory()
    }

    override suspend fun insertLotto(numbers: List<Int>, type: String, round: Int) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val dateString = dateFormat.format(Date())
        val numbersString = numbers.joinToString(",")

        val entity = LottoEntity(
            numbers = numbersString,
            type = type,
            date = dateString,
            round = round
        )
        lottoDao.insertLotto(entity)
    }

    override suspend fun deleteLotto(id: Long) {
        lottoDao.deleteById(id)
    }
}
