package br.com.helpdev.lapscounter.ui.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdsPolicyTest {

    private var now = 1_000_000L
    private val policy = AdsPolicy(nowMillis = { now })

    @Test
    fun adsAreBlockedUntilConsentIsResolvedAndAllowed() {
        assertFalse(policy.mayRequestAds())
        policy.resolveConsent(canRequestAds = false)
        assertFalse(policy.mayRequestAds())
        policy.resolveConsent(canRequestAds = true)
        assertTrue(policy.mayRequestAds())
    }

    @Test
    fun interstitialFrequencyCapRequiresFifteenMinutes() {
        policy.resolveConsent(canRequestAds = true)
        assertTrue(policy.mayShowInterstitial(lastShownAt = 0L, loadedAt = now))
        assertFalse(policy.mayShowInterstitial(lastShownAt = now, loadedAt = now))

        now += AdsPolicy.MINIMUM_INTERSTITIAL_INTERVAL_MILLIS
        assertTrue(policy.mayShowInterstitial(lastShownAt = now - AdsPolicy.MINIMUM_INTERSTITIAL_INTERVAL_MILLIS, loadedAt = now))
    }

    @Test
    fun interstitialCannotBeShownAfterExpiryOrBeforeItWasLoaded() {
        policy.resolveConsent(canRequestAds = true)
        assertFalse(policy.mayShowInterstitial(lastShownAt = 0L, loadedAt = now + 1L))

        now += AdsPolicy.INTERSTITIAL_MAX_AGE_MILLIS + 1L
        assertFalse(policy.mayShowInterstitial(lastShownAt = 0L, loadedAt = 1_000_000L))
    }

    @Test
    fun oneShotActionRunsOnlyOnce() {
        var calls = 0
        val action = OnceAction { calls++ }

        action.invoke()
        action.invoke()

        assertEquals(1, calls)
    }
}
