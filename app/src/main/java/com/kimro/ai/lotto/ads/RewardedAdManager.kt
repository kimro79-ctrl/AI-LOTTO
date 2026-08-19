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
 * 몬테카를로 시뮬레이션 "실행" 버튼을 누를 때 보여줄 보상형 광고를 관리한다.
 *
 * ⚠️ 지금은 구글 공식 테스트 광고 단위 ID를 쓰고 있다. 실제 서비스 광고 단위 ID
 * (ca-app-pub-8544113192886422/8519174296)는 정식 출시 직전에 아래 REWARDED_AD_UNIT_ID만
 * 바꿔주면 된다. 비공개 테스트 중에 실제 ID를 쓰면 본인이 테스트하다 클릭해서
 * "무효 트래픽"으로 계정이 정지될 위험이 있다.
 */
object RewardedAdManager {

    // 구글 공식 테스트용 보상형 광고 단위 ID (Android). 실제 서비스 시 교체 필요.
    private const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    private var rewardedAd: RewardedAd? = null
    private var isLoading = false

    /** 화면에 진입하거나 광고를 소진한 직후 미리 다음 광고를 받아둔다 (사용자가 버튼 누를 때 대기 없이 뜨도록). */
    fun preload(context: Context) {
        if (rewardedAd != null || isLoading) return
        isLoading = true

        RewardedAd.load(
            context,
            TEST_REWARDED_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w("RewardedAdManager", "광고 로드 실패: ${error.message}")
                    rewardedAd = null
                    isLoading = false
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
        onRewardEarned: () -> Unit,
        onAdUnavailable: () -> Unit
    ) {
        val ad = rewardedAd
        if (ad == null) {
            onAdUnavailable()
            // 다음번을 위해 미리 다시 로드해둔다.
            preload(activity)
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                preload(activity) // 다음 실행을 위해 미리 로드
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                rewardedAd = null
                preload(activity)
                onAdUnavailable()
            }
        }

        ad.show(activity) { _ ->
            // 광고를 끝까지 시청해서 리워드 콜백이 호출된 경우에만 실제 기능을 실행한다.
            onRewardEarned()
        }
    }
}
