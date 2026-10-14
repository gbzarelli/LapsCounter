package br.com.helpdev.lapscounter.utils

import android.content.Context
import androidx.preference.PreferenceManager
import br.com.helpdev.lapscounter.R

fun getLapDistance(context: Context) = PreferenceManager.getDefaultSharedPreferences(context)
    .getFloat(
        context.getString(R.string.pref_lap_distance_name),
        context.resources.getInteger(R.integer.pref_lap_distance_default_value).toFloat()
    )

fun isAudioEnable(context: Context) = PreferenceManager.getDefaultSharedPreferences(context)
    .getBoolean(
        context.getString(R.string.pref_audio_click_name),
        context.resources.getBoolean(R.bool.pref_audio_click_value)
    )

fun getStringLapDistance(context: Context): String =
    getStringLapDistance(context, getLapDistance(context))

fun getStringLapDistance(context: Context, lapDistance: Float): String =
    context.getString(R.string.info_distance_lap, lapDistance)

fun countLastLap(context: Context) = PreferenceManager.getDefaultSharedPreferences(context)
    .getBoolean(context.getString(R.string.pref_count_last_lap_name), true)

fun isFirstOpen(context: Context, classe: Class<*>) =
    PreferenceManager.getDefaultSharedPreferences(context)
        .getBoolean(classe.simpleName, true)

fun setFirstOpen(context: Context, classe: Class<*>) {
    PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(classe.simpleName, false).apply()
}
