// File Path: app/src/main/java/com/kimro/ai/lotto/data/local/LottoDatabase.kt
package com.kimro.ai.lotto.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [LottoEntity::class], version = 2, exportSchema = false)
abstract class LottoDatabase : RoomDatabase() {
    abstract fun lottoDao(): LottoDao

    companion object {
        /**
         * v1 -> v2: 저장된 번호 조합이 어느 회차에 해당하는지 알 수 있도록 round 컬럼을 추가한다.
         * 기존 행들은 전부 round = 0(회차 미상)으로 채워지며, 기존 데이터는 그대로 보존된다.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE lotto_history ADD COLUMN round INTEGER NOT NULL DEFAULT 0"
                )
            }
        }
    }
}
