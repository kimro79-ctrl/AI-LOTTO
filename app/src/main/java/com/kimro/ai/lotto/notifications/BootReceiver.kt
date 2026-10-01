// File Path: app/src/main/java/com/kimro/ai/lotto/notifications/BootReceiver.kt
package com.kimro.ai.lotto.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * AlarmManager에 걸어둔 알람은 기기가 재부팅되면 전부 사라진다. 그래서 부팅이 끝나는 시점에
 * 이 리시버가 깨어나서 금요일 알람을 다시 걸어준다.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            NotificationScheduler.scheduleNextFriday(context)
        }
    }
}
