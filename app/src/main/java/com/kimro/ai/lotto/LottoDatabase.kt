// File Path: app/src/main/java/com/kimro/ai/lotto/data/local/LottoDatabase.kt
package com.kimro.ai.lotto.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [LottoEntity::class], version = 3, exportSchema = false)
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

        /**
         * v2 -> v3: 저장 당시 사용자가 실제로 선택했던 분석 조건 문구를 남기기 위해 conditionLabel 컬럼을 추가한다.
         * 기존 행들은 전부 빈 문자열로 채워지며, 화면에서는 빈 값일 경우 type 기반 기본 라벨로 대체해서 보여준다.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE lotto_history ADD COLUMN conditionLabel TEXT NOT NULL DEFAULT ''"
                )
            }
        }
    }
}
