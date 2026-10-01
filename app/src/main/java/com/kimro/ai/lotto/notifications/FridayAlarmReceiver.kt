// File Path: app/src/main/java/com/kimro/ai/lotto/notifications/FridayAlarmReceiver.kt
package com.kimro.ai.lotto.notifications

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.kimro.ai.lotto.MainActivity
import com.kimro.ai.lotto.R

/**
 * 매주 금요일 오후 5시, NotificationScheduler가 예약해둔 알람이 울리면 이 리시버가 받아서
 * 알림을 띄운다. 알림을 띄운 직후 바로 "다음 주 금요일"로 재예약해서, 한 번 울리고 끝나지 않고
 * 계속 매주 반복되게 한다.
 */
class FridayAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "friday_reminder_channel"
        private const val NOTIFICATION_ID = 2001

        // 알림을 탭했을 때 MainActivity로 "분석 탭 열어줘"라고 전달하는 데 쓰는 키/값.
        const val EXTRA_OPEN_SCREEN = "open_screen"
        const val SCREEN_ANALYSIS = "analysis"
    }

    override fun onReceive(context: Context, intent: Intent) {
        showNotification(context)
        // 울린 직후 바로 다음 주 금요일로 다시 걸어둔다 (반복 알람 대신 매번 재예약하는 방식).
        NotificationScheduler.scheduleNextFriday(context)
    }

    private fun showNotification(context: Context) {
        // Android 13(API 33) 이상은 알림 표시 권한이 없으면 알림 자체가 안 뜬다.
        // (권한 요청은 MainActivity에서 하지만, 사용자가 거부했을 경우를 대비해 한 번 더 방어적으로 체크한다.)
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_SCREEN, SCREEN_ANALYSIS)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("AI로또 6/45")
            .setContentText("내일 추첨하는 로또번호 선택하세요")
            .setStyle(NotificationCompat.BigTextStyle().bigText("내일 추첨하는 로또번호 선택하세요"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
