// File Path: app/src/main/java/com/kimro/ai/lotto/di/AppModule.kt
package com.kimro.ai.lotto.di

import android.content.Context
import androidx.room.Room
import com.kimro.ai.lotto.data.local.LottoDao
import com.kimro.ai.lotto.data.local.LottoDatabase
import com.kimro.ai.lotto.data.repository.LottoRepository
import com.kimro.ai.lotto.data.repository.LottoRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideLottoDatabase(
        @ApplicationContext context: Context
    ): LottoDatabase {
        return Room.databaseBuilder(
            context,
            LottoDatabase::class.java,
            "lotto_db"
        )
            // round 컬럼을 추가하는 마이그레이션. 이걸 등록하지 않으면 버전이 올라갈 때
            // 기존 사용자 데이터가 통째로 삭제되므로 반드시 필요하다.
            .addMigrations(LottoDatabase.MIGRATION_1_2)
            .build()
    }

    @Provides
    @Singleton
    fun provideLottoDao(database: LottoDatabase): LottoDao {
        return database.lottoDao()
    }

    @Provides
    @Singleton
    fun provideLottoRepository(lottoDao: LottoDao): LottoRepository {
        return LottoRepositoryImpl(lottoDao)
    }
}
