package com.rchugunov.dynamicpinsview

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.clustering.ClusterManager
import com.rchugunov.dynamicpinsview.data.MarkerData
import com.rchugunov.dynamicpinsview.data.icon
import com.rchugunov.dynamicpinsview.data.lat
import com.rchugunov.dynamicpinsview.data.lon
import com.rchugunov.dynamicpinsview.listener.BoundariesListener
import com.rchugunov.dynamicpinsview.map.MapHolder
import com.rchugunov.dynamicpinsview.map.MapMarker
import com.rchugunov.dynamicpinsview.map.MapMarkersRenderer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue


class MainActivity : AppCompatActivity(), OnMapReadyCallback, MapMarkersRenderer.Callback {

    private val mapHolder = MapHolder()
    private val boundariesFlow = MutableSharedFlow<LatLngBounds>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val scope = CoroutineScope(Dispatchers.Main.immediate)
    private var currentMarkersSet: MutableSet<MapMarker> = mutableSetOf()
    private val locations: List<MarkerData> by lazy {
        (application as MapApplication).data
    }

    private lateinit var mapRenderer: MapMarkersRenderer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        (supportFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment)
            .getMapAsync(this)


        scope.launch {
            boundariesFlow.collect { update -> reloadData(update) }
        }
    }

    private fun reloadData(bounds: LatLngBounds) {
        fun LatLngBounds.includesLocation(lat: Double, lon: Double): Boolean {
            return this.northeast.latitude > lat && this.southwest.latitude < lat &&
                    this.northeast.longitude > lon && this.southwest.longitude < lon

        }

        val locationsInArea = locations.filter { bounds.includesLocation(it.lat, it.lon) }
        locationsInArea.map { item ->
            MapMarker(
                location = LatLng(item.lat, item.lon),
                titleText = "Item ${item.hashCode()}",
                icon = MapMarker.Icon.Placeholder(item.icon),
                pinColor = COLORS[item.hashCode().absoluteValue.rem(10)]
            )
        }.apply { setMarkers(this) }
    }

    override fun onDestroy() {
        super.onDestroy()

        scope.coroutineContext.cancelChildren()
    }

    override fun onMapReady(map: GoogleMap) {
        map.uiSettings.apply {
            isMyLocationButtonEnabled = true
            isMapToolbarEnabled = true
        }

        val boundariesListener = BoundariesListener(map, boundariesFlow::tryEmit)
        val clusterManager = ClusterManager<MapMarker>(this, map)

        mapRenderer = MapMarkersRenderer(
            context = this,
            callback = this,
            map = map,
            clusterManager = clusterManager
        )
        clusterManager.renderer = mapRenderer
        mapHolder.initialize(
            msMap = map,
            clusterManager = clusterManager,
            boundariesListener = boundariesListener,
        )
    }

    private fun setMarkers(markers: List<MapMarker>) {
        val newMarkersSet = markers.toMutableSet()
        val removedElements = currentMarkersSet - newMarkersSet
        val addedElements = newMarkersSet - currentMarkersSet
        mapHolder.executeOnClusterManager { manager ->
            manager.apply {
                removedElements.forEach { removeItem(it) }
                addedElements.forEach { addItem(it) }
                cluster()
            }
        }
        currentMarkersSet = newMarkersSet
    }

    override fun onImageLoaded(icon: MapMarker.Icon) {
        mapHolder.executeOnClusterManager { manager ->
            currentMarkersSet.forEach { marker ->
                if (marker.icon.url == icon.url) {
                    Log.v("MapMarkersRenderer", "marker added ${icon.url}")
                    manager.updateItem(marker)
                    manager.cluster()
                }
            }
        }
    }

    companion object {
        private val COLORS = listOf(
            Color.parseColor("#6a0136"),
            Color.parseColor("#bfab25"),
            Color.parseColor("#b81365"),
            Color.parseColor("#026c7c"),
            Color.parseColor("#055864"),
            Color.parseColor("#58355e"),
            Color.parseColor("#e03616"),
            Color.parseColor("#fff689"),
            Color.parseColor("#cfffb0"),
            Color.parseColor("#5998c5"),
        )

        private const val TAG = "MainActivity_test"
    }
}