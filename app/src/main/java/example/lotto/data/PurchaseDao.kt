// app/src/main/java/com/kimro/ai/lotto/data/PurchaseDao.kt
package com.kimro.ai.lotto.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PurchaseDao {
    @Query("SELECT * FROM purchase_table ORDER BY id DESC")
    fun getAllPurchases(): Flow<List<PurchaseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchase(purchase: PurchaseEntity)

    @Query("UPDATE purchase_table SET rankResult = :result WHERE id = :id")
    suspend fun updateRankResult(id: Long, result: String)
}
