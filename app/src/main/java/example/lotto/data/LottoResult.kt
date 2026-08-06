package com.kimro.ai.lotto.data

data class LottoResult(
    val drwNo: Int,           // 회차
    val drwNoDate: String,    // 추첨일
    val totSellamnt: Long,    // 총 판매금액
    val firstWinamnt: Long,   // 1등 당첨금액
    val firstPrzwnerCo: Int,  // 1등 당첨 인원
    val drwtNo1: Int,
    val drwtNo2: Int,
    val drwtNo3: Int,
    val drwtNo4: Int,
    val drwtNo5: Int,
    val drwtNo6: Int,
    val bnusNo: Int,          // 보너스 번호
    val returnValue: String   // 성공 여부 ("success")
)
