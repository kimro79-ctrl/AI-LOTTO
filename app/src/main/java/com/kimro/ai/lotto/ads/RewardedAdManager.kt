// File Path: app/src/main/java/com/kimro/ai/lotto/ads/RewardedAdManager.kt
package com.kimro.ai.lotto.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * 앱 안의 여러 "광고 보고 이용하기" 기능(몬테카를로 시뮬레이션, 유전 알고리즘 최적화,
 * 역발상 기댓값 분석 등)이 각자 다른 광고 단위 ID를 쓰면서도 같은 매니저를 공유할 수 있도록,
 * 광고 단위 ID별로 로드 상태를 따로 관리한다.
 */
object RewardedAdManager {

    // 실제 서비스용 "몬테카를로 보상형" 광고 단위 ID (Android).
    const val AD_UNIT_MONTE_CARLO = "ca-app-pub-8544113192886422/8519174296"

    // 실제 서비스용 "유전알고리즘 보상형" / "기댓값분석 보상형" 광고 단위 ID (2026-08-27 발급).
    const val AD_UNIT_GENETIC_ALGORITHM = "ca-app-pub-8544113192886422/4118646274"
    const val AD_UNIT_EXPECTED_VALUE = "ca-app-pub-8544113192886422/2781513879"

    private val loadedAds = mutableMapOf<String, RewardedAd?>()
    private val loadingFlags = mutableMapOf<String, Boolean>()

    /** 화면에 진입하거나 광고를 소진한 직후 미리 다음 광고를 받아둔다 (사용자가 버튼 누를 때 대기 없이 뜨도록). */
    fun preload(context: Context, adUnitId: String = AD_UNIT_MONTE_CARLO) {
        if (loadedAds[adUnitId] != null || loadingFlags[adUnitId] == true) return
        loadingFlags[adUnitId] = true

        RewardedAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    loadedAds[adUnitId] = ad
                    loadingFlags[adUnitId] = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w("RewardedAdManager", "광고 로드 실패($adUnitId): ${error.message}")
                    loadedAds[adUnitId] = null
                    loadingFlags[adUnitId] = false
                }
            }
        )
    }

    /**
     * 광고를 보여준다. 광고를 끝까지 봐서 보상을 받으면 [onRewardEarned]를 호출한다.
     * 광고가 아직 준비되지 않았거나 로드에 실패한 경우에는, 사용자가 기능 자체를
     * 못 쓰게 되는 걸 막기 위해 [onAdUnavailable]을 호출해 그냥 바로 진행시킨다.
     */
    fun showAd(
        activity: Activity,
        adUnitId: String = AD_UNIT_MONTE_CARLO,
        onRewardEarned: () -> Unit,
        onAdUnavailable: () -> Unit
    ) {
        val ad = loadedAds[adUnitId]
        if (ad == null) {
            onAdUnavailable()
            // 다음번을 위해 미리 다시 로드해둔다.
            preload(activity, adUnitId)
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                loadedAds[adUnitId] = null
                preload(activity, adUnitId) // 다음 실행을 위해 미리 로드
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                loadedAds[adUnitId] = null
                preload(activity, adUnitId)
                onAdUnavailable()
            }
        }

        ad.show(activity) { _ ->
            // 광고를 끝까지 시청해서 리워드 콜백이 호출된 경우에만 실제 기능을 실행한다.
            onRewardEarned()
        }
    }
}
