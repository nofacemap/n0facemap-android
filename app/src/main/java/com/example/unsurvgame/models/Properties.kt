package com.example.unsurvgame.models

import com.google.gson.annotations.SerializedName

data class Properties(	  @SerializedName("scalerank") val scalerank : Int = 0,
                          @SerializedName("name") val name : String = "",
                          @SerializedName("website") val website : String = "",
                          @SerializedName("natlscale") val natlscale : Int = 0,
                          @SerializedName("featureclass") val featureclass : String = "" )