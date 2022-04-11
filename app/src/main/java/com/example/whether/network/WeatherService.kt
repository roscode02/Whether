package com.example.whether.network

import com.example.whether.models.WhetherResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherService {
    @GET("2.5/weather")
    fun getWheather(
        @Query("lat") lat:Double,
        @Query("lon") lon : Double,
        @Query("units") units:String?,
        @Query("appid") appid:String?
    ):Call<WhetherResponse>

}