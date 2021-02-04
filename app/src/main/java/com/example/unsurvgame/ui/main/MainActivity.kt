package com.example.unsurvgame.ui.main

import android.Manifest.permission.*
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.os.Looper
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProviders
import com.example.unsurvgame.R
import com.example.unsurvgame.models.*
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.Property.NONE
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.*
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.Logger
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

  private lateinit var mapboxMap: MapboxMap
  private var geoSource: GeoJsonSource? = null
  private lateinit var styledMap: Style
  private var lastLatLng = LatLng(0.0, 0.0)
  private lateinit var geoObj: GeoJSON

  companion object {
    private const val DROPPED_MARKER_LAYER_ID = "DROPPED_MARKER_LAYER_ID"
    private const val RC_SIGN_IN = 420
  }

  private val vm: MainActivityVM by lazy {
    ViewModelProviders.of(this).get(MainActivityVM::class.java)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    if (Firebase.auth.currentUser == null) {
      val providers = arrayListOf(
        AuthUI.IdpConfig.EmailBuilder().build(),
        AuthUI.IdpConfig.GoogleBuilder().build())

      val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
          val data: Intent? = result.data

        }
        else{
          Toast.makeText(this, "You must be logged in to contribute and play.", Toast.LENGTH_LONG).show()
        }
      }

      val loginActivity = AuthUI.getInstance()
        .createSignInIntentBuilder()
        .setAvailableProviders(providers)
        .build()

      resultLauncher.launch(loginActivity)
    }

    Mapbox.getInstance(this, getString(R.string.mapbox_access_token))
    setContentView(R.layout.activity_main)
    Logger.addLogAdapter(AndroidLogAdapter())
    runOncePermissionGranted(savedInstanceState)

  }

  private fun runOncePermissionGranted(savedInstanceState: Bundle?) {
    map.onCreate(savedInstanceState)
    map.getMapAsync(this)
  }

  override fun onMapReady(mapboxMap: MapboxMap) {
    this.mapboxMap = mapboxMap

    mapboxMap.setStyle(
      Style.Builder().fromUri("mapbox://styles/paxblueribbon/ckk1k54ln0x0u17o1l8zjblz8")
    ) { styledMap ->
      vm.geoJSON.observe(this, {
        if (!this::geoObj.isInitialized) {
          geoObj = it
          val asJson = Gson().toJson(it)

          geoSource = GeoJsonSource("fbgeo", asJson)
          val symLayer = SymbolLayer("circleLayer", "fbgeo")
          styledMap.addSource(geoSource!!)
          styledMap.addImage(
            "dronecam",
            BitmapFactory.decodeResource(resources, R.drawable.dronecam)
          )
          styledMap.addLayer(
            symLayer.withProperties(
              iconImage("dronecam")
            )
          )
        } else {
          val asJson = Gson().toJson(it)
          geoSource!!.setGeoJson(asJson)
        }
      })

      vm.getGeoJson()

      this.styledMap = styledMap
      when (PackageManager.PERMISSION_GRANTED) {
        ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) -> {
          enableLocationComponent()
        }
        else -> {
          requestPermissions(arrayOf(ACCESS_FINE_LOCATION), 1312)
        }
      }

      val hoveringMarker = ImageView(this@MainActivity)
      hoveringMarker.setImageResource(R.drawable.ic_iconfinder_sed_21_2231967)
      val params = FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
        Gravity.CENTER
      )
      hoveringMarker.layoutParams = params
      map.addView(hoveringMarker)

      selectLocButton.setOnClickListener {
        val mapTargetLatLng: LatLng = mapboxMap.cameraPosition.target
        vm.addPointToDB(
          Point.fromLngLat(
            mapTargetLatLng.longitude,
            mapTargetLatLng.latitude
          )
        )
      }

    }
  }

  @SuppressLint("MissingPermission")
  private fun enableLocationComponent() {
    if (PermissionsManager.areLocationPermissionsGranted(this)) {

      val customLocationComponentOptions = LocationComponentOptions.builder(this)
        .trackingGesturesManagement(true)
        .foregroundDrawable(R.drawable.gpspin)
        .backgroundTintColor(Color.TRANSPARENT)
        .accuracyColor(ContextCompat.getColor(this, R.color.colorPrimaryDark))
        .build()

      val locationComponentActivationOptions =
        LocationComponentActivationOptions.builder(this, styledMap)
          .locationComponentOptions(customLocationComponentOptions)
          .build()

      mapboxMap.locationComponent.apply {
        activateLocationComponent(locationComponentActivationOptions)
        isLocationComponentEnabled = true
        cameraMode = CameraMode.TRACKING
        renderMode = RenderMode.COMPASS
      }

      if (mapboxMap.locationComponent.lastKnownLocation != null) {
        val latLng = LatLng(
          mapboxMap.locationComponent.lastKnownLocation!!.latitude,
          mapboxMap.locationComponent.lastKnownLocation!!.longitude
        )
        val position = CameraPosition.Builder()
          .target(latLng)
          .zoom(18.0)
          .build()
        mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(position))
      }
      initLocationEngine(mapboxMap)
    }
  }

  @SuppressLint("MissingPermission")
  private fun initLocationEngine(mapboxMap: MapboxMap) {
    val locationEngine = LocationEngineProvider.getBestLocationEngine(this)
    val request = LocationEngineRequest.Builder(1000L)
      .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
      .setMaxWaitTime(5000L).build()
    val callback = object : LocationEngineCallback<LocationEngineResult> {
      override fun onSuccess(result: LocationEngineResult?) {
        if (result != null && result.lastLocation != null) {
          val location = result.lastLocation
          val latLng = LatLng(location!!.latitude, location.longitude)
          if (lastLatLng != latLng) {
            lastLatLng = latLng
            val position = CameraPosition.Builder()
              .target(latLng)
              .zoom(18.0)
              .build()
            mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(position))
          }
        }
      }

      override fun onFailure(exception: Exception) {
        throw exception
      }
    }
    locationEngine.requestLocationUpdates(request, callback, Looper.getMainLooper())
  }

  override fun onResume() {
    super.onResume()
    map.onResume()
  }

  override fun onPause() {
    super.onPause()
    map.onPause()
  }

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    when (requestCode) {
      1312 -> {
        if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
          enableLocationComponent()
        } else {
          Toast.makeText(
            this,
            "You've rejected location permissions.  We won't be able to get your location unless you enable them.",
            Toast.LENGTH_LONG
          ).show()
        }
        return
      }
      else -> {
        //ignore all other requests
      }
    }
  }
}
