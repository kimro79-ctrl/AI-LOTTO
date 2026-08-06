// app/src/main/java/com/kimro/ai/lotto/data/AppDatabase.kt
package com.kimro.ai.lotto.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [PurchaseEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun purchaseDao(): PurchaseDao
}

