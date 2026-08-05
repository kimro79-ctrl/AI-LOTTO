package com.example.lotto.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [LottoEntity::class], version = 1, exportSchema = false)
abstract class LottoDatabase : RoomDatabase() {
    abstract fun lottoDao(): LottoDao
}

