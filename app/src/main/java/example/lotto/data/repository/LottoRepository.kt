// File Path: app/src/main/java/com/kimro/ai/lotto/data/repository/LottoRepository.kt
package com.kimro.ai.lotto.data.repository

import com.kimro.ai.lotto.data.local.LottoEntity
import kotlinx.coroutines.flow.Flow

interface LottoRepository {
    fun getAllHistory(): Flow<List<LottoEntity>>

    // round: 이 조합이 해당하는 회차. 모르면 생략(기본값 0 = 회차 미상)해도 기존 호출부는 그대로 동작한다.
    // conditionLabel: 저장 당시 실제로 선택했던 분석 조건 문구. 모르면 생략(기본값 "")해도 기존 호출부는 그대로 동작한다.
    suspend fun insertLotto(numbers: List<Int>, type: String, round: Int = 0, conditionLabel: String = "")

    suspend fun deleteLotto(id: Long)
}
