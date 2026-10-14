package br.com.helpdev.lapscounter.ui.ads

internal class AdsPolicy(
    private val nowMillis: () -> Long = System::currentTimeMillis
) {
    private var consentResolved = false
    private var consentAllowsAds = false

    fun resolveConsent(canRequestAds: Boolean) {
        consentResolved = true
        consentAllowsAds = canRequestAds
    }

    fun mayRequestAds() = consentResolved && consentAllowsAds

    fun isWithinInterstitialFrequencyCap(lastShownAt: Long): Boolean {
        val sinceLastShow = nowMillis() - lastShownAt
        return lastShownAt == 0L || sinceLastShow >= MINIMUM_INTERSTITIAL_INTERVAL_MILLIS || sinceLastShow < 0L
    }

    fun mayShowInterstitial(lastShownAt: Long, loadedAt: Long): Boolean {
        if (!mayRequestAds()) return false
        val now = nowMillis()
        val age = now - loadedAt
        if (age < 0 || age >= INTERSTITIAL_MAX_AGE_MILLIS) return false
        return isWithinInterstitialFrequencyCap(lastShownAt)
    }

    companion object {
        const val MINIMUM_INTERSTITIAL_INTERVAL_MILLIS = 15 * 60 * 1000L
        const val INTERSTITIAL_MAX_AGE_MILLIS = 60 * 60 * 1000L
    }
}

internal class OnceAction(private val action: () -> Unit) {
    private var invoked = false

    @Synchronized
    fun invoke() {
        if (invoked) return
        invoked = true
        action()
    }
}
