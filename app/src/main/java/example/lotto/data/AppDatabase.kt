package com.kimro.ai.lotto.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kimro.ai.lotto.data.dao.PurchaseDao
import com.kimro.ai.lotto.data.entity.PurchaseEntity

@Database(
    entities = [PurchaseEntity::class],
    version = 2, // 유니크 인덱스 적용을 위해 버전 1 -> 2로 증가
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun purchaseDao(): PurchaseDao

    companion object {
        const val DATABASE_NAME = "lotto_db"
    }
}
