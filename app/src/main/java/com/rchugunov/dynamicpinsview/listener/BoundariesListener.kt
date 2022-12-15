package com.rchugunov.dynamicpinsview.listener

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLngBounds

internal class BoundariesListener(
    private val map: GoogleMap,
    private val boundariesSupplier: (LatLngBounds) -> Unit
) : GoogleMap.OnCameraIdleListener {

    override fun onCameraIdle() {
        val boundaries = map.projection.visibleRegion.latLngBounds
        boundariesSupplier(boundaries)
    }
}