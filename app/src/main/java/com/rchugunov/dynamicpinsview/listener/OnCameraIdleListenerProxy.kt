package com.rchugunov.dynamicpinsview.listener

import com.google.android.gms.maps.GoogleMap

internal class OnCameraIdleListenerProxy(private val listeners: List<GoogleMap.OnCameraIdleListener>) :
    GoogleMap.OnCameraIdleListener {

    override fun onCameraIdle() {
        listeners.forEach { it.onCameraIdle() }
    }
}