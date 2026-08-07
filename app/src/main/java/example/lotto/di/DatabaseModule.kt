package com.kimro.ai.lotto.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    // Dao와 Database 코드를 모두 제거했으므로, 
    // 빌드 에러가 나지 않도록 모듈 내용만 남겨둡니다.
}
