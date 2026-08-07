package com.kimro.ai.lotto.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    // 현재 Room 관련 코드를 사용하지 않으므로 내용을 비워둡니다.
    // 추후 데이터베이스를 다시 사용하게 될 때 여기에 @Provides 코드를 추가하세요.
}
