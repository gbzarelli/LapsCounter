package br.com.helpdev.lapscounter.sport

object LapDistancePreferenceAdapter {
    fun toDisplayValue(distanceMeters: Float, unit: DistanceDisplayUnit): Float =
        distanceMeters / unit.metersPerUnit

    fun toStoredMeters(displayValue: Float, unit: DistanceDisplayUnit): Float {
        require(displayValue.isFinite() && displayValue > 0f) {
            "Lap distance must be a finite, positive value"
        }
        return displayValue * unit.metersPerUnit
    }
}
