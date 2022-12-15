package com.rchugunov.dynamicpinsview

import android.app.Application
import com.rchugunov.dynamicpinsview.data.MarkerData
import com.rchugunov.dynamicpinsview.data.MarkerLocationsService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MapApplication : Application() {

    private val context = CoroutineScope(Dispatchers.Default)

    private var _data: List<MarkerData> = emptyList()
    val data: List<MarkerData>
        get() = _data

    override fun onCreate() {
        super.onCreate()

        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(MarkerLocationsService::class.java)


        context.launch {
            _data = service.getLocations()
        }
    }
}