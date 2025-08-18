// utils/LocationUtils.kt
package com.example.thelastone.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.util.Log
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

private const val TAG = "LocationUtils"

/**
 * 回傳夠新的座標：
 * 1) 先拿 lastLocation，若「存在且未超過 maxAgeMillis」就用；
 * 2) 否則用 getCurrentLocation() 取一次即時定位（5 秒超時）。
 */
@SuppressLint("MissingPermission")
suspend fun getLastKnownLatLngOrNull(
    context: Context,
    maxAgeMillis: Long = 2 * 60_000 // 2 分鐘
): Pair<Double, Double>? {
    val client = LocationServices.getFusedLocationProviderClient(context)

    fun isSuspectMountainView(lat: Double, lng: Double): Boolean {
        // Googleplex 37.4219983, -122.084；抓個 ~2km 的容忍
        return kotlin.math.abs(lat - 37.4219983) < 0.02 &&
                kotlin.math.abs(lng + 122.084)   < 0.02
    }

    // 1) 優先拿一次即時定位（6 秒 timeout）
    val token = CancellationTokenSource()
    val current: Location? = withTimeoutOrNull(6_000) {
        suspendCancellableCoroutine { cont ->
            client.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                token.token
            ).addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resume(null) }
            cont.invokeOnCancellation { token.cancel() }
        }
    }
    if (current != null && !isSuspectMountainView(current.latitude, current.longitude)) {
        Log.d("LocationUtils", "use currentLocation=(${current.latitude}, ${current.longitude})")
        return current.latitude to current.longitude
    }

    // 2) 退回 lastLocation（需夠新 & 不是 Mountain View 預設）
    val last: Location? = suspendCancellableCoroutine { cont ->
        client.lastLocation
            .addOnSuccessListener { cont.resume(it) }
            .addOnFailureListener { cont.resume(null) }
    }
    if (last != null) {
        val age = System.currentTimeMillis() - last.time
        val valid = age in 0..maxAgeMillis && !isSuspectMountainView(last.latitude, last.longitude)
        Log.d("LocationUtils", "lastLocation=(${last.latitude}, ${last.longitude}), age=${age}ms, valid=$valid")
        if (valid) return last.latitude to last.longitude
    }

    Log.w("LocationUtils", "No reliable location (current/last invalid or Mountain View default)")
    return null
}
