package com.kimro.ai.lotto.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lotto_history")
data class LottoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val numbers: String, // 예: "3,12,19,24,31,40"
    val type: String,    // "ANALYSIS" (패턴분석), "FORTUNE" (운세), "QR" (스캔)
    val date: String     // 생성/저장 날짜
)

