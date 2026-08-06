package com.kimro.ai.lotto.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kimro.ai.lotto.data.entity.PurchaseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PurchaseDao {

    // 최신 등록된 항목이 맨 위로 오도록 내림차순 정렬 조회
    @Query("SELECT * FROM purchase_table ORDER BY id DESC")
    fun getAllPurchases(): Flow<List<PurchaseEntity>>

    // 중복 데이터(회차+번호 조합)가 이미 존재하면 무시하고 삽입
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPurchase(purchase: PurchaseEntity)

    // 특정 항목 삭제
    @Delete
    suspend fun deletePurchase(purchase: PurchaseEntity)

    // ID로 특정 항목 삭제
    @Query("DELETE FROM purchase_table WHERE id = :id")
    suspend fun deletePurchaseById(id: Long)

    // 전체 내역 초기화
    @Query("DELETE FROM purchase_table")
    suspend fun deleteAllPurchases()
}
