package com.rchugunov.dynamicpinsview.map

import com.google.android.gms.maps.GoogleMap
import com.google.maps.android.clustering.ClusterManager
import com.rchugunov.dynamicpinsview.listener.BoundariesListener
import com.rchugunov.dynamicpinsview.listener.OnCameraIdleListenerProxy

internal class MapHolder {

    private var googleMap: GoogleMap? = null
    private var clusterManager: ClusterManager<MapMarker>? = null

    inline fun executeOnClusterManager(consumer: (ClusterManager<MapMarker>) -> Unit) =
        clusterManager?.let(consumer)

    fun initialize(
        msMap: GoogleMap,
        clusterManager: ClusterManager<MapMarker>,
        boundariesListener: BoundariesListener
    ) {
        this.googleMap = msMap
        this.clusterManager = clusterManager

        msMap.setOnCameraIdleListener(
            OnCameraIdleListenerProxy(
                listOf(
                    boundariesListener,
                    clusterManager
                )
            )
        )
    }
}