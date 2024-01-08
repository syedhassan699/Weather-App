package com.example.weatherapp

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

@Suppress("DEPRECATION")
object Constants {

    const val APP_ID:String = "233f30efce07458247dfee88a9805a43"
    const val BASE_URL: String = "https://api.openweathermap.org/data/2.5/"
    const val METRIC_UNIT: String = "Metric"
    @SuppressLint("ObsoleteSdkInt")
    fun isNetworkAvailable(context: Context):Boolean{
        val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE)
        as ConnectivityManager

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            val network =connectivityManager.activeNetwork?: return  false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network)?: return false
            return when {
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
        }
        else{
            val networkInfo = connectivityManager.activeNetworkInfo
            return networkInfo!= null && networkInfo.isConnectedOrConnecting
        }
    }
}