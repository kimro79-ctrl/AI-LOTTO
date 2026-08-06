package com.kimro.ai.lotto.network

import com.kimro.ai.lotto.data.LottoResult
import retrofit2.http.GET
import retrofit2.http.Query

interface LottoApiService {
    @GET("common.do?method=getLottoNumber")
    suspend fun getLottoResult(
        @Query("drwNo") drwNo: Int
    ): LottoResult

    companion object {
        const val BASE_URL = "https://www.dhlottery.co.kr/"
    }
}
