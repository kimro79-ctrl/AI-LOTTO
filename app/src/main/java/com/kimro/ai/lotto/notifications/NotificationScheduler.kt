// File Path: app/src/main/java/com/kimro/ai/lotto/notifications/NotificationScheduler.kt
package com.kimro.ai.lotto.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

/**
 * "금요일 알림"을 예약/재예약하는 공용 로직. MainActivity(최초 실행)와 BootReceiver(재부팅 후)
 * 양쪽에서 똑같이 쓴다. AlarmManager.setAndAllowWhileIdle()은 분 단위 정확도까지는 보장 안 하지만
 * (몇 분 오차는 날 수 있음), 그 대신 Android 12+의 "정확한 알람" 특수 권한(SCHEDULE_EXACT_ALARM)이
 * 필요 없다. "내일 번호 뽑으세요" 리마인더는 정각일 필요가 없는 알림이라 이 방식으로 충분하다.
 *
 * 알림을 "반복"으로 등록하지 않고, 울릴 때마다(FridayAlarmReceiver 안에서) 다음 주 같은 요일/시각으로
 * 다시 1회성 예약을 거는 방식을 쓴다 - AlarmManager의 setRepeating()은 더 이상 정확한 반복을 보장하지
 * 않는다고 공식 문서에 나와 있어서, 매번 새로 계산해서 재예약하는 쪽이 더 안정적이다.
 */
object NotificationScheduler {

    private const val REQUEST_CODE_FRIDAY = 1001

    /** 다음으로 돌아올 "금요일 오후 5시" 시각(밀리초)을 계산한다. 오늘이 금요일 5시 이전이면 오늘, 아니면 다음 주. */
    private fun nextFriday5PM(): Long {
        val calendar = Calendar.getInstance()
        val targetHour = 17 // 오후 5시

        calendar.set(Calendar.HOUR_OF_DAY, targetHour)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val now = System.currentTimeMillis()

        // 이번 주 금요일로 맞춘 뒤, 이미 지났으면 하루씩 더해서 다음 금요일을 찾는다.
        while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.FRIDAY || calendar.timeInMillis <= now) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, targetHour)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    private fun buildPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, FridayAlarmReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_FRIDAY,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** 다음 금요일 5시로 알람을 (재)예약한다. 이미 걸려있어도 같은 요청코드라 덮어써질 뿐 중복되지 않는다. */
    fun scheduleNextFriday(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildPendingIntent(context)
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            nextFriday5PM(),
            pendingIntent
        )
    }
}
