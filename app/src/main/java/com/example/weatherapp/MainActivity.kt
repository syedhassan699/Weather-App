@file:Suppress("DEPRECATION")

package com.example.weatherapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.weatherapp.databinding.ActivityMainBinding
import com.example.weatherapp.models.WeatherResponse
import com.example.weatherapp.network.WeatherService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Suppress("NAME_SHADOWING")
class MainActivity : AppCompatActivity() {

    private var binding:ActivityMainBinding? = null
    private lateinit var mFusedLocationClient:FusedLocationProviderClient
    private var mProgressDialogue: Dialog? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (!isLocationEnabled()){
            Toast.makeText(this,
            "Your Location Provider is turned OFF Plz turn it ON",
            Toast.LENGTH_LONG).show()

            val intent =Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }
        else{
            Dexter.withActivity(this)
                .withPermissions(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
                .withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        if (report!!.areAllPermissionsGranted()) {
                            requestLocationData()
                        }
                        if (report.isAnyPermissionPermanentlyDenied){
                            Toast.makeText(
                                this@MainActivity,
                                "You have denied location permission.Please allow as it is mandatory for the App",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                    override fun onPermissionRationaleShouldBeShown(
                        permissions: MutableList<com.karumi.dexter.listener.PermissionRequest>?,
                        token: PermissionToken?
                    ) {
                        showRationalDialogForPermissions()
                    }
                }).onSameThread()
                .check()
        }
    }
    private fun showRationalDialogForPermissions() {
        AlertDialog.Builder(this)
            .setMessage("It Looks like you have turned off permissions required for this feature. It can be enabled under Application Settings")
            .setPositiveButton("GO TO SETTINGS"
            ) { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }.show()
    }
    @SuppressLint("InlinedApi", "MissingPermission")
    private fun requestLocationData(){
        val mLocationRequest = com.google.android.gms.location.LocationRequest()
        mLocationRequest.priority =com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY

        mFusedLocationClient.requestLocationUpdates(
        mLocationRequest,mLocationCallback,
        Looper.myLooper())

    }

    private val mLocationCallback = object : LocationCallback(){
        override fun onLocationResult(locationResult: LocationResult) {
            val mLastLocation: Location = locationResult.lastLocation!!
            val latitude = mLastLocation.latitude
            Log.i("Current Latitude" , "$latitude")

            val longitude = mLastLocation.longitude
            Log.i("Current Longitude" , "$longitude")
            getLocationWeatherDetails(latitude,longitude)
        }
    }
    fun getLocationWeatherDetails(latitude:Double,longitude:Double){
        if (Constants.isNetworkAvailable(this)){
            val retrofit:Retrofit=Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            val service:WeatherService=retrofit
                .create(WeatherService::class.java)
            val listCall:Call<WeatherResponse> = service.getWeather(
                latitude,longitude,Constants.METRIC_UNIT,Constants.APP_ID)

            showCustomProgressDialog()

            listCall.enqueue(object : Callback<WeatherResponse>{
                override fun onResponse(
                    call: Call<WeatherResponse>,
                    response: Response<WeatherResponse>,
                ) {
                    if (response.isSuccessful){
                        hideCustomProgressDialogue()
                        val weatherList:WeatherResponse?=response.body()
                       setupUI(weatherList!!)
                        Log.i("Response Weather","$weatherList")
                    }else{
                        when(response.code()){
                            400 ->{
                                Log.e("Error 400","Bad Connection")
                            }
                            404 ->{
                                Log.e("Error 404","Not Found")
                            }
                            else ->{
                                Log.e("Error ","Generic Error")
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    Log.e("Error", t.message.toString())
                    hideCustomProgressDialogue()
                }

            })

        }
        else{
            Toast.makeText(this, "No network available", Toast.LENGTH_SHORT).show()
        }
    }
    private fun isLocationEnabled():Boolean{

        val locationManager:LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
    private fun showCustomProgressDialog(){

        mProgressDialogue = Dialog(this)

        mProgressDialogue!!.setContentView(R.layout.dialogue_custom_progress)

        mProgressDialogue!!.show()
    }
    private fun hideCustomProgressDialogue(){
        if (mProgressDialogue!=null){
            mProgressDialogue!!.dismiss()
        }
    }
    @SuppressLint("SetTextI18n")
    private fun setupUI(weatherList:WeatherResponse){
        for (i in weatherList.weather.indices){
            Log.i("Weather Name:",weatherList.toString())

            binding?.tvMain?.text = weatherList.weather[i].main
            binding?.tvMainDescription?.text = weatherList.weather[i].description
            binding?.tvTemp?.text = weatherList.main.temp.toString() +
                    getUnit(application.resources.configuration.locales.toString())
            binding?.tvHumidity?.text = weatherList.main.humidity.toString() +"per cent"
            binding?.tvMin?.text = weatherList.main.temp_min.toString() + " min"
            binding?.tvMax?.text = weatherList.main.temp_max.toString() + " max"
            binding?.tvSpeed?.text = weatherList.wind.speed.toString()
            binding?.tvName?.text = weatherList.name
            binding?.tvCountry?.text = weatherList.sys.country

            binding?.tvSunriseTime?.text = unixTime(weatherList.sys.sunrise)
            binding?.tvSunsetTime?.text = unixTime(weatherList.sys.sunset)
        }
    }
    private fun getUnit(value: String): String {
        Log.i("unit", value)
        var value = "°C"
        if ("US" == value || "LR" == value || "MM" == value) {
            value = "°F"
        }
        return value
    }
    @SuppressLint("SimpleDateFormat")
    private fun unixTime(timeX:Long):String?{
        val date = Date(timeX * 1000L)
        val sdf = SimpleDateFormat("HH:mm", Locale.UK)
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(date)
    }
}