// File Path: app/src/main/java/com/kimro/ai/lotto/ads/InterstitialAdManager.kt
package com.kimro.ai.lotto.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlin.random.Random

/**
 * RewardedAdManager와 같은 로드/표시 패턴을 그대로 따르되, "보상형(다 봐야 보상)"이 아니라
 * "그냥 자동으로 뜨는" 전면 광고를 다룬다. 현재는 "번호 생성" 버튼 전용 전면 광고 하나만 쓴다.
 */
object InterstitialAdManager {

    // "번호 생성" 버튼 전용 전면 광고 단위 ID (2026-09-18 발급).
    const val AD_UNIT_GENERATE = "ca-app-pub-8544113192886422/6105719207"

    private val loadedAds = mutableMapOf<String, InterstitialAd?>()
    private val loadingFlags = mutableMapOf<String, Boolean>()

    /** 화면 진입 시 미리 받아둬서, 실제로 광고를 띄워야 하는 순간에 대기 없이 바로 뜨도록 한다. */
    fun preload(context: Context, adUnitId: String = AD_UNIT_GENERATE) {
        if (loadedAds[adUnitId] != null || loadingFlags[adUnitId] == true) return
        loadingFlags[adUnitId] = true

        InterstitialAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    loadedAds[adUnitId] = ad
                    loadingFlags[adUnitId] = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w("InterstitialAdManager", "광고 로드 실패($adUnitId): ${error.message}")
                    loadedAds[adUnitId] = null
                    loadingFlags[adUnitId] = false
                }
            }
        )
    }

    /**
     * 광고를 보여준다. 보상 개념이 없으니 닫히면(끝까지 보든 중간에 닫든) 그냥 [onAdClosed]를 호출한다.
     * 광고가 아직 준비 안 됐으면 사용자가 기능을 못 쓰게 막지 않고 바로 [onAdUnavailable]을 호출한다.
     */
    fun showAd(
        activity: Activity,
        adUnitId: String = AD_UNIT_GENERATE,
        onAdClosed: () -> Unit,
        onAdUnavailable: () -> Unit
    ) {
        val ad = loadedAds[adUnitId]
        if (ad == null) {
            onAdUnavailable()
            preload(activity, adUnitId)
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                loadedAds[adUnitId] = null
                preload(activity, adUnitId)
                onAdClosed()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                loadedAds[adUnitId] = null
                preload(activity, adUnitId)
                onAdUnavailable()
            }
        }

        ad.show(activity)
    }

    // ── "번호 생성" 버튼 전용 확률 + 쿨다운 로직 ─────────────────────────────
    // 매번 1/10 확률로 뽑으면 운 나쁘면 연달아 걸릴 수 있어서, 광고가 뜬 직후로부터
    // 최소 GENERATE_AD_COOLDOWN번은 무조건 광고 없이 넘어가도록 보장한다.
    private const val GENERATE_AD_PROBABILITY = 0.10
    private const val GENERATE_AD_COOLDOWN = 4
    private var clicksSinceLastAd = 0

    /**
     * "번호 생성" 버튼을 누를 때마다 호출한다. 이번 클릭에 전면 광고를 보여줘야 하면 true.
     * 쿨다운 중이면 항상 false이고, 쿨다운이 끝난 뒤부터는 클릭마다 1/10 확률로 true가 나온다.
     * true를 반환하는 순간 쿨다운 카운터를 리셋한다.
     */
    fun shouldShowGenerateAd(): Boolean {
        clicksSinceLastAd++
        if (clicksSinceLastAd <= GENERATE_AD_COOLDOWN) return false

        val hit = Random.nextDouble() < GENERATE_AD_PROBABILITY
        if (hit) clicksSinceLastAd = 0
        return hit
    }
}
