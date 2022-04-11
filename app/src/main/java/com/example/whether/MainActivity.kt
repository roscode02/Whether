package com.example.whether

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationManager
import android.location.LocationRequest
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.whether.databinding.ActivityMainBinding
import com.example.whether.models.WhetherResponse
import com.example.whether.network.WeatherService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient
    private var progressDialog :Dialog? = null
    private lateinit var mSharedPreferences: SharedPreferences         // it is used to store the data in  phone incase internet is not on
    private lateinit var binding :ActivityMainBinding
    private var ivMain :ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)



        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        mSharedPreferences = getSharedPreferences(Constants.PerferenceName , Context.MODE_PRIVATE)
        if(!isLocationEnabled()){
            Toast.makeText(this , "TURNON GPS" , Toast.LENGTH_LONG).show()

            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS) // Automatically opens up location setting on phone
            startActivity(intent)
        }else{
            Dexter.withActivity(this)
                .withPermissions(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
                .withListener(object : MultiplePermissionsListener{
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        if (report!!.areAllPermissionsGranted()){
                                requestLocation()
                        }
                        if(report.isAnyPermissionPermanentlyDenied){
                            Toast.makeText(this@MainActivity , "Permission denied" , Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onPermissionRationaleShouldBeShown(
                        permissions: MutableList<PermissionRequest>?,
                        token: PermissionToken?
                    ) {
                        ShowRationalDialogForPermission()
                    }

                }).onSameThread().check()
        }
    }

    private fun getLocationWeaterDeatils( latitue:Double , longitude :Double){ // checks if internet is connected or not
        if(Constants.isNetworkAvailable(this)){
            // if internet is connected then proccess then call retrofit
            val retrofit : Retrofit = Retrofit.Builder()
                .baseUrl(Constants.BaseUrl)
                .addConverterFactory(GsonConverterFactory.create()).build()

            val service : WeatherService = retrofit.create<WeatherService>(WeatherService::class.java) // connects the @get class

            val listcall :Call<WhetherResponse> = service.getWheather(                // calls the class that  and passes all the parameter
                latitue , longitude , Constants.MetricUnit , Constants.AppId
            )
            showProgressDialog()          // will show a progress till everything is loaded from api
            listcall.enqueue(object  :Callback<WhetherResponse>{
                override fun onResponse(
                    call: Call<WhetherResponse>,
                    response: Response<WhetherResponse>
                ) {
                    if(response.isSuccessful) {
                        endProgreeDialog()  // when everything is loaded  from api cancel progress
                        val weatherList: WhetherResponse? = response.body()

                        val weatherResponseJsonString = Gson().toJson(weatherList) // this and next four line represent how you store the string in sharedPreferences
                        val editor = mSharedPreferences.edit()
                        editor.putString(Constants.WeatherResponseData , weatherResponseJsonString)
                        editor.apply()
                        setUpUi(weatherList!!)

                        Log.i("Response result", "$weatherList")
                    }else{
                        val rc = response.code()
                        when(rc){
                            400 -> {
                                Log.e("Error" , "Bad Connection")
                            }
                            404 ->{
                                Log.e("Erorr " , "404 error")
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<WhetherResponse>, t: Throwable) {
                   Log.e("Error" , t!!.message.toString())
                    endProgreeDialog()  // when everything is loaded  from api cancel progress
                }

            })

        }
    }

    @SuppressLint("MissingPermission") //it is needed
    private fun requestLocation(){ // this is used to collect location details
        val mlocationRequest = com.google.android.gms.location.LocationRequest()

        mlocationRequest.priority = com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY

        mFusedLocationProviderClient.requestLocationUpdates(
            mlocationRequest , mLocationCallBack , Looper.myLooper()
        )
    }

    private val mLocationCallBack = object  : LocationCallback(){ // this function actually grant permission
        override fun onLocationResult(locationResult: LocationResult) {
            val mLastLocation : Location = locationResult.lastLocation
            val latitude = mLastLocation.latitude

            val longitude = mLastLocation.longitude
            getLocationWeaterDeatils(latitude , longitude)

        }
    }
   private  fun ShowRationalDialogForPermission(){
       AlertDialog.Builder(this)
           .setMessage("Permission turned of")
           .setPositiveButton("Go to Setting"){
               _ , _ ->
               try{
                   val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                   val uri = Uri.fromParts("package" , packageName , null) // open the settings for that particular app
                   intent.data = uri
                   startActivity(intent)
               }catch (e : ActivityNotFoundException){
                   e.printStackTrace()
               }
           }
           .setNegativeButton("Cancel"){
               dialog , _ ->
               dialog.dismiss()
           }.show()
   }
   private fun showProgressDialog()
   {
       progressDialog = Dialog(this)
       progressDialog?.setContentView(R.layout.progress_dialog)
       progressDialog?.setTitle("Wait")
       progressDialog?.setCancelable(false)
       progressDialog?.show()
   }
   private fun endProgreeDialog(){
       if(progressDialog != null){
           progressDialog?.dismiss()
           progressDialog = null
       }

   }
    private fun isLocationEnabled():Boolean{
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main , menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            R.id.action_refresh -> {
                requestLocation()
                true
            }else -> super.onOptionsItemSelected(item)
        }

    }

    private fun setUpUi(weatherList :WhetherResponse){
        for( i in weatherList.weather.indices){
            binding.tvMain.text = weatherList.weather[i].main
            binding.tvMainDescription.text = weatherList.weather[i].description
            binding.tvTemp.text = weatherList.main.temp.toString() + getUnit(application.resources.configuration.locales.toString())
            binding.tvMin.text = weatherList.main.temp_min.toString() + " Min"
            binding.tvMax.text   = weatherList.main.temp_max.toString() + " Max"
            binding.tvSpeed.text  = weatherList.wind.speed.toString()
            binding.tvCountry.text  = weatherList.sys.country
            val sunrise = unixTime(weatherList.sys.sunrise)
            val sunset = unixTime(weatherList.sys.sunset)
            binding.tvName.text  = weatherList.name
            binding.tvSunsetTime.text  = sunset
            binding.tvSunriseTime.text = sunrise
            binding.tvHumidity.text  = weatherList.main.humidity.toString() +" per cent"

            when(weatherList.weather[i].icon){
                "01d" ->{
                    ivMain?.setImageResource(R.drawable.sunny)
                }
                "02d" -> ivMain?.setImageResource(R.drawable.cloud)
                "03d" -> ivMain?.setImageResource(R.drawable.cloud)
                "04d" -> ivMain?.setImageResource(R.drawable.cloud)
                "04n" -> ivMain?.setImageResource(R.drawable.cloud)
                "10d" -> ivMain?.setImageResource(R.drawable.rain)
                "11d" -> ivMain?.setImageResource(R.drawable.storm)
                "13d" -> ivMain?.setImageResource(R.drawable.snowflake)
                "01n" -> ivMain?.setImageResource(R.drawable.cloud)
                "02n" -> ivMain?.setImageResource(R.drawable.cloud)
                "03n" -> ivMain?.setImageResource(R.drawable.cloud)
                "10n" -> ivMain?.setImageResource(R.drawable.cloud)
                "11n" -> ivMain?.setImageResource(R.drawable.rain)
                "13n" -> ivMain?.setImageResource(R.drawable.snowflake)
            }

        }
    }

    private fun getUnit(value: String): String? {
        var value = "^C"
        if("US" == value ||"LR" == value || "MM" == value){
            value = "^F"
        }
        return value
    }
    private fun unixTime(timex :Long):String?{  // function will convert long to time format
        val date = Date(timex * 1000L)
        val sdf = SimpleDateFormat("HH:mm" , Locale.CHINESE)
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(date)
    }
}