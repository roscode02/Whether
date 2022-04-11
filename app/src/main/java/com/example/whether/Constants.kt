package com.example.whether

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
// check internet or noy
object Constants {

    const val AppId : String = "a311a78382eb27f22615714e21d3cab3"
    const val BaseUrl : String = "http://api.openweathermap.org/data/"
    const val MetricUnit : String = "metric"
    const val PerferenceName = "weatherAppPreference"
    const val WeatherResponseData = "weather_response_data"


    fun isNetworkAvailable(context: Context):Boolean{
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val network = connectivityManager.activeNetwork ?: return false // if empty return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false // if empty return false

        return  when{
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ->  true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ->  true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ->  true
            else ->  false
        }

    }
}