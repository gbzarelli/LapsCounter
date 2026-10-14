package br.com.helpdev.lapscounter.ui.ads

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import br.com.helpdev.lapscounter.R
import br.com.helpdev.lapscounter.utils.isFirstOpen
import br.com.helpdev.lapscounter.utils.setFirstOpen

abstract class InterstitialActivity : AppCompatActivity() {

    private var adDialogInformation: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initAd()
    }

    private fun initAd() {
        AdsManager.requestConsent(this) { allowed ->
            if (!allowed || isFinishing || isDestroyed || !AdsManager.canDisplayInterstitialByTime(this)) return@requestConsent
            AdsManager.loadInterstitial(this) { loaded ->
                if (!loaded || isFinishing || isDestroyed) return@loadInterstitial
                if (isFirstOpen(this, javaClass)) showMessageToOpenAd()
                else AdsManager.showInterstitial(this) { }
            }
        }
    }

    private fun showMessageToOpenAd() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.app_name)
        builder.setMessage(R.string.first_show_ad)
        builder.setPositiveButton(android.R.string.ok) { _, _ ->
            AdsManager.showInterstitial(this) { }
            setFirstOpen(this, javaClass)
        }
        builder.create().also { adDialogInformation = it }.show()
    }

    override fun onPause() {
        super.onPause()
        adDialogInformation?.dismiss()
        adDialogInformation = null
    }
}
