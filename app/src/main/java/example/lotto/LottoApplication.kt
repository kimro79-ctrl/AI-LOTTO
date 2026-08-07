package com.kimro.ai.lotto

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class LottoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 필요한 초기화 로직이 있다면 여기에 작성합니다.
    }
}
