package com.udacity.project4.locationreminders.geofence

import android.content.Context
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.GeofenceStatusCodes
import com.udacity.project4.R
import java.lang.Exception

object GeofenceErrorMessage {
    fun getErrorMessage(context: Context, ex: Exception) : String {
        return if (ex is ApiException)
            getErrorMessageFromCode(context, ex.statusCode)
        else
            context.getString(R.string.geofence_not_available)
    }

    fun getErrorMessageFromCode(context: Context, errorCode: Int) : String {
        return when(errorCode) {
            GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE ->
                context.getString(R.string.geofence_not_available)

            GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES ->
                context.getString(R.string.geofence_too_many_geofences)

            GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS ->
                context.getString(R.string.geofence_too_many_pending_intents)

            else -> context.getString(R.string.geofence_unknown_error)
        }
    }
}