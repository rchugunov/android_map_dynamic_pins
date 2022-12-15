package com.rchugunov.dynamicpinsview.data

import retrofit2.http.GET

interface MarkerLocationsService {

    @GET("locations.json")
    suspend fun getLocations(): List<MarkerData>
}