package com.rchugunov.dynamicpinsview.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.collection.LruCache
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import com.google.maps.android.ui.IconGenerator
import com.rchugunov.dynamicpinsview.BuildConfig
import com.rchugunov.dynamicpinsview.dpToPx
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import java.util.*
import java.util.concurrent.ConcurrentHashMap

internal class MapMarkersRenderer(
    private val context: Context,
    private val callback: Callback,
    map: GoogleMap,
    clusterManager: ClusterManager<MapMarker>,
) : DefaultClusterRenderer<MapMarker>(context, map, clusterManager) {

    private val mapMarkerView: MapMarkerView = MapMarkerView(context)
    private val markerIconGenerator = IconGenerator(context)

    private val loadedImages = LruCache<String, Bitmap>(30)
    private val pendingRequests = Collections.synchronizedMap(mutableMapOf<String, Target>())
    private val failedRequests = Collections.newSetFromMap<String>(ConcurrentHashMap())

    init {
        markerIconGenerator.setBackground(null)
        markerIconGenerator.setContentView(mapMarkerView)
        Picasso.get().setIndicatorsEnabled(true)
        Picasso.get().isLoggingEnabled = true
    }

    override fun onBeforeClusterItemRendered(clusterItem: MapMarker, markerOptions: MarkerOptions) {
        val data = getItemIcon(clusterItem)
        markerOptions
            .icon(data.bitmapDescriptor)
            .anchor(data.anchorU, data.anchorV)
    }

    override fun onClusterItemUpdated(clusterItem: MapMarker, marker: Marker) {
        val data = getItemIcon(clusterItem)
        marker.setIcon(data.bitmapDescriptor)
        marker.setAnchor(data.anchorU, data.anchorV)
    }

    private fun getItemIcon(marker: MapMarker): IconData {
        Log.v(TAG, "requested marker 1 ${marker.titleText}")
        val iconToShow: MapMarker.Icon = when (marker.icon) {
            is MapMarker.Icon.Placeholder -> {
                val cachedIcon = loadedImages.get(marker.icon.url)
                Log.v(TAG, "requested marker 2 ${marker.titleText} ${marker.icon.url} $cachedIcon")
                if (cachedIcon == null) {
                    loadBitmapImage(marker.icon.url)
                }
                cachedIcon?.let { MapMarker.Icon.BitmapIcon(marker.icon.url, it) } ?: marker.icon
            }

            else -> marker.icon
        }

        mapMarkerView.setContent(
            circle = MapMarkerView.CircleContent.Marker(
                mapMarkerIcon = iconToShow,
            ),
            title = marker.titleText,
            pinColor = marker.pinColor
        )
        val icon: Bitmap = markerIconGenerator.makeIcon()
        val middleBalloon = dpToPx(mapMarkerView.context, 24)
        return IconData(
            bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(icon),
            anchorU = middleBalloon / 2 / icon.width,
            anchorV = 1f
        )
    }

    override fun onBeforeClusterRendered(
        cluster: Cluster<MapMarker>,
        markerOptions: MarkerOptions
    ) {
        val data = getClusterIcon(cluster)
        markerOptions
            .icon(data.bitmapDescriptor)
            .anchor(data.anchorU, data.anchorV)
    }

    override fun onClusterUpdated(cluster: Cluster<MapMarker>, marker: Marker) {
        val data = getClusterIcon(cluster)
        marker.setIcon(data.bitmapDescriptor)
        marker.setAnchor(data.anchorU, data.anchorV)
    }

    private fun getClusterIcon(cluster: Cluster<MapMarker>): IconData {
        mapMarkerView.setContent(
            circle = MapMarkerView.CircleContent.Cluster(
                count = cluster.size
            ),
            title = null,
            pinColor = Color.parseColor("#D32F2F")
        )

        val icon: Bitmap = markerIconGenerator.makeIcon()
        val middleBalloon = dpToPx(context, 40)
        return IconData(
            bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(icon),
            anchorU = middleBalloon / 2 / icon.width,
            anchorV = 1f
        )
    }

    private fun loadBitmapImage(imageUrl: String) {
        if (imageUrl in pendingRequests) {
            Log.v(TAG, "pending requests: ${pendingRequests.size}")
            return
        }
        val size = dpToPx(context, 40).toInt()
        val target = IconTarget(imageUrl)
        pendingRequests[imageUrl] = target

        Log.v(TAG, "started loading $imageUrl")
        Picasso.get()
            .load(BuildConfig.IMAGES_URL + imageUrl)
            .resize(size, size)
            .centerCrop()
            .into(target)
    }

    private fun handleLoadedDrawable(imageUrl: String, bitmap: Bitmap) {
        val old = loadedImages.put(imageUrl, bitmap)
        if (old == null) {
            callback.onImageLoaded(icon = MapMarker.Icon.BitmapIcon(imageUrl, bitmap))
        }
    }

    override fun shouldRenderAsCluster(cluster: Cluster<MapMarker>): Boolean = cluster.size > 1

    private data class IconData(
        val bitmapDescriptor: BitmapDescriptor,
        val anchorU: Float,
        val anchorV: Float,
    )

    inner class IconTarget(private val imageUrl: String) : Target {
        override fun onBitmapLoaded(bitmap: Bitmap, from: Picasso.LoadedFrom) {
            pendingRequests.remove(imageUrl)
            Log.v(TAG, "finished loading $imageUrl")
            handleLoadedDrawable(imageUrl, bitmap)
        }

        override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
            Log.e(TAG, e?.message, e)
            pendingRequests.remove(imageUrl)
            failedRequests.add(imageUrl)
        }

        override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}
    }

    interface Callback {
        fun onImageLoaded(icon: MapMarker.Icon)
    }

    companion object {
        private const val TAG = "MapMarkersRenderer"
    }
}