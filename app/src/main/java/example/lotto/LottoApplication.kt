package com.kimro.ai.lotto

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.kimro.ai.lotto.notifications.FridayAlarmReceiver
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class LottoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    // 알림 채널은 Android 8(API 26) 이상에서 필수다. 앱이 시작될 때 한 번만 등록해두면,
    // 이미 있는 채널에 또 만들어도 그냥 무시되니(덮어쓰기 아님) 매번 호출해도 안전하다.
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                FridayAlarmReceiver.CHANNEL_ID,
                "금요일 번호 생성 알림",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "매주 금요일, 내일 추첨하는 로또번호를 뽑아보라는 알림이에요."
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}
