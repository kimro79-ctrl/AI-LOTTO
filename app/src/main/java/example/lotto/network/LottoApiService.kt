package com.kimro.ai.lotto.network

import retrofit2.http.GET
import retrofit2.http.Query

interface LottoApiService {
    @GET("common.do?method=getLottoNumber")
    suspend fun getLottoResult(
        @Query("drwNo") drwNo: Int
    ): LottoResult
}

data class LottoResult(
    val returnValue: String?,
    val drwNo: Int?,
    val drwNoDate: String?,
    val totSellamnt: Long?,
    val firstWinamnt: Long?,
    val firstPrzwnerCo: Int?,
    val drwtNo1: Int?,
    val drwtNo2: Int?,
    val drwtNo3: Int?,
    val drwtNo4: Int?,
    val drwtNo5: Int?,
    val drwtNo6: Int?,
    val bnusNo: Int?
)
