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
        ).build()
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
