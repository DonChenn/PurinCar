// Smartcar endpoints, keys and the other fixed settings the app runs on.
package com.example.purincar.core.common

import com.example.purincar.BuildConfig

object AppConfig {
    const val SMARTCAR_API_BASE = "https://api.smartcar.com/v2.0/"
    const val SMARTCAR_TOKEN_URL = "https://auth.smartcar.com/oauth/token"
    const val SMARTCAR_CLIENT_ID = BuildConfig.SMARTCAR_CLIENT_ID
    const val SMARTCAR_CLIENT_SECRET = BuildConfig.SMARTCAR_CLIENT_SECRET
    const val SMARTCAR_REDIRECT_URI = "sc$SMARTCAR_CLIENT_ID://exchange"
    val SMARTCAR_SCOPES = arrayOf("read_vehicle_info", "read_fuel", "read_odometer", "read_security")

    const val KM_TO_MILES = 0.621371

    const val CSV_EXPORT_FILE_NAME = "car_records_export.csv"
    const val CSV_MIME_TYPE = "text/csv"

    const val MAINTENANCE_WORK_NAME = "maintenance_check"
}
