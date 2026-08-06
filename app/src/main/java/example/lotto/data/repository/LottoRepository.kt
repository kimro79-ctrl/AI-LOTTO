package com.kimro.ai.lotto.data.repository

import com.kimro.ai.lotto.data.local.LottoEntity
import kotlinx.coroutines.flow.Flow

interface LottoRepository {
    fun getAllHistory(): Flow<List<LottoEntity>>
    suspend fun insertLotto(numbers: List<Int>, type: String)
    suspend fun deleteLotto(id: Long)
}

