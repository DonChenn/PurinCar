// Every kind of service the app tracks, with how often each one is due.
package com.example.purincar.data.maintenance

import androidx.annotation.StringRes
import com.example.purincar.R
import kotlinx.serialization.Serializable

@Serializable
enum class ServiceType(
    val label: String,
    @param:StringRes val labelRes: Int,
    val mileageInterval: Int,
    val dayInterval: Int
) {
    ENGINE_OIL("Engine Oil", R.string.service_engine_oil, 5_000, 180),
    TIRE_ROTATION("Tire Rotation", R.string.service_tire_rotation, 7_500, 180),
    AIR_FILTERS("Air Filters", R.string.service_air_filters, 15_000, 365),
    ENGINE_COOLANT("Engine Coolant", R.string.service_engine_coolant, 30_000, 730),
    BRAKE_FLUID("Brake Fluid", R.string.service_brake_fluid, 30_000, 730),
    BATTERY_FAN("Battery Fan", R.string.service_battery_fan, 30_000, 1_095),
    TRANSMISSION_FLUID("Transmission Fluid", R.string.service_transmission_fluid, 60_000, 1_460),
    SPARK_PLUGS("Spark Plugs", R.string.service_spark_plugs, 100_000, 1_825),
    MISCELLANEOUS("Miscellaneous", R.string.service_miscellaneous, 0, 0);

    val hasMileageInterval: Boolean get() = mileageInterval > 0
    val hasDayInterval: Boolean get() = dayInterval > 0

    companion object {
        // Finds the service type a label names, matching loosely the way older CSV files were written.
        fun fromLabel(label: String): ServiceType? {
            val trimmed = label.trim()
            if (trimmed.isEmpty()) return null
            return entries.firstOrNull { it.label.equals(trimmed, ignoreCase = true) }
                ?: entries.firstOrNull { it.label.contains(trimmed, ignoreCase = true) }
        }
    }
}
