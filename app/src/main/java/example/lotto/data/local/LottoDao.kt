package com.kimro.ai.lotto.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LottoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLotto(lotto: LottoEntity)

    @Query("SELECT * FROM lotto_history ORDER BY id DESC")
    fun getAllHistory(): Flow<List<LottoEntity>>

    @Query("DELETE FROM lotto_history WHERE id = :id")
    suspend fun deleteById(id: Long)
}

