// File Path: app/src/main/java/com/kimro/ai/lotto/data/local/LottoEntity.kt
package com.kimro.ai.lotto.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lotto_history")
data class LottoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val numbers: String, // 예: "3,12,19,24,31,40"
    val type: String,    // "ANALYSIS" (패턴분석), "FORTUNE" (운세), "QR" (스캔) 등
    val date: String,    // 생성/저장 날짜

    // 이 조합이 해당하는 로또 회차. 0이면 "아직 특정 회차와 연결되지 않음(회차 미상)"을 의미한다.
    // - QR 스캔으로 저장된 항목: 실제 회차 번호가 들어감
    // - AI/타로 등 자동 생성 항목: 생성 당시에는 몰라서 0으로 저장되고, 추후 당첨 확인 로직에서
    //   "가장 가까운 다음 회차"로 매핑하는 방식으로 보완 예정
    val round: Int = 0
)
