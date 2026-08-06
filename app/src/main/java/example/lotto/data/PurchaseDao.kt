package com.kimro.ai.lotto.data

import androidx.room.Dao
import androidx.room.Delete
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

    // 항목 단건 삭제 기능 추가
    @Delete
    suspend fun deletePurchase(purchase: PurchaseEntity)

    // 전체 내역 삭제 기능 추가 (필요시 사용)
    @Query("DELETE FROM purchase_table")
    suspend fun deleteAllPurchases()
}
