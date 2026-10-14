package br.com.helpdev.lapscounter.ui.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.View
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import java.lang.ref.WeakReference

object AdsManager {
    private const val TAG = "AdsManager"
    private const val LAST_INTERSTITIAL_SHOWN = "last_display_ad"

    private val policy = AdsPolicy()
    private val pendingBanners = mutableListOf<WeakReference<AdView>>()
    private val consentCallbacks = mutableListOf<(Boolean) -> Unit>()
    private var consentRequestInProgress = false
    private var consentInformation: ConsentInformation? = null
    private var mobileAdsInitializationInProgress = false
    private var mobileAdsReady = false
    private var interstitialAd: InterstitialAd? = null
    private var interstitialLoadedAt = 0L

    fun requestConsent(activity: Activity, onResolved: (Boolean) -> Unit = {}) {
        if (policy.mayRequestAds()) {
            ensureMobileAdsInitialized(activity.applicationContext, onResolved)
            return
        }
        if (consentRequestInProgress) {
            consentCallbacks += onResolved
            return
        }

        consentCallbacks += onResolved
        consentRequestInProgress = true
        val information = UserMessagingPlatform.getConsentInformation(activity)
        consentInformation = information
        val parameters = ConsentRequestParameters.Builder().build()
        information.requestConsentInfoUpdate(
            activity,
            parameters,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) Log.w(TAG, "Consent form unavailable: ${formError.message}")
                    finishConsent(activity, information.canRequestAds())
                }
            },
            { updateError ->
                Log.w(TAG, "Consent information update failed: ${updateError.message}")
                finishConsent(activity, information.canRequestAds())
            }
        )
    }

    private fun finishConsent(activity: Activity, canRequestAds: Boolean) {
        consentRequestInProgress = false
        policy.resolveConsent(canRequestAds)
        if (!canRequestAds) {
            pendingBanners.clear()
            notifyConsentCallbacks(false)
            return
        }
        ensureMobileAdsInitialized(activity.applicationContext) {
            if (it) loadPendingBanners()
            notifyConsentCallbacks(it)
        }
    }

    private fun ensureMobileAdsInitialized(context: Context, onReady: (Boolean) -> Unit) {
        if (mobileAdsReady) {
            onReady(true)
            return
        }
        consentCallbacks += onReady
        if (mobileAdsInitializationInProgress) return
        mobileAdsInitializationInProgress = true
        MobileAds.initialize(context) {
            mobileAdsInitializationInProgress = false
            mobileAdsReady = true
            loadPendingBanners()
            notifyConsentCallbacks(true)
        }
    }

    private fun notifyConsentCallbacks(allowed: Boolean) {
        val callbacks = consentCallbacks.toList()
        consentCallbacks.clear()
        callbacks.forEach { callback -> callback(allowed) }
    }

    fun loadBanner(adView: AdView) {
        if (!policy.mayRequestAds() || !mobileAdsReady) {
            pendingBanners += WeakReference(adView)
            return
        }
        loadBannerNow(adView)
    }

    private fun loadPendingBanners() {
        if (!policy.mayRequestAds() || !mobileAdsReady) return
        val queued = pendingBanners.toList()
        pendingBanners.clear()
        queued.mapNotNull { it.get() }.forEach(::loadBannerNow)
    }

    private fun loadBannerNow(adView: AdView) {
        val owner = adView.context as? Activity
        if (owner?.isFinishing == true || owner?.isDestroyed == true || adView.visibility == View.GONE) return
        adView.adListener = object : AdListener() {
            override fun onAdFailedToLoad(error: LoadAdError) {
                Log.w(TAG, "Banner failed to load: ${error.message}")
            }
        }
        adView.loadAd(AdRequest.Builder().build())
    }

    fun loadInterstitial(context: Context, onLoaded: (Boolean) -> Unit = {}) {
        if (!policy.mayRequestAds() || !mobileAdsReady) {
            onLoaded(false)
            return
        }
        val unitId = context.getString(br.com.helpdev.lapscounter.R.string.admob_interstitialad_id)
        InterstitialAd.load(
            context,
            unitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    interstitialLoadedAt = System.currentTimeMillis()
                    onLoaded(true)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    Log.w(TAG, "Interstitial failed to load: ${error.message}")
                    onLoaded(false)
                }
            }
        )
    }

    fun showInterstitial(activity: Activity, onDismissed: () -> Unit): Boolean {
        val completion = OnceAction(onDismissed)
        val ad = interstitialAd
        if (ad == null || activity.isFinishing || activity.isDestroyed) {
            completion.invoke()
            return false
        }

        val preferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(activity)
        val lastShownAt = preferences.getLong(LAST_INTERSTITIAL_SHOWN, 0L)
        if (!policy.mayShowInterstitial(lastShownAt, interstitialLoadedAt)) {
            interstitialAd = null
            completion.invoke()
            return false
        }

        interstitialAd = null
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                preferences.edit().putLong(LAST_INTERSTITIAL_SHOWN, System.currentTimeMillis()).apply()
            }

            override fun onAdDismissedFullScreenContent() {
                completion.invoke()
            }

            override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                Log.w(TAG, "Interstitial failed to show: ${error.message}")
                completion.invoke()
            }
        }
        return try {
            ad.show(activity)
            true
        } catch (exception: RuntimeException) {
            Log.w(TAG, "Interstitial show failed", exception)
            completion.invoke()
            false
        }
    }

    fun canDisplayInterstitialByTime(context: Context): Boolean {
        val lastShownAt = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
            .getLong(LAST_INTERSTITIAL_SHOWN, 0L)
        return policy.isWithinInterstitialFrequencyCap(lastShownAt)
    }

    fun privacyOptionsRequired(): Boolean =
        consentInformation?.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    fun showPrivacyOptions(activity: Activity, onComplete: (Boolean) -> Unit = {}) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            if (formError != null) Log.w(TAG, "Privacy options form failed: ${formError.message}")
            val canRequestAds = consentInformation?.canRequestAds() == true
            policy.resolveConsent(canRequestAds)
            onComplete(canRequestAds)
        }
    }
}
