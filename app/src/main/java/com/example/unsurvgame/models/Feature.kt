package com.example.unsurvgame.models

import com.google.gson.annotations.SerializedName

data class Feature (
    @SerializedName("type") val type : String = "Feature",
    @SerializedName("properties") val properties : Properties = Properties(),
    @SerializedName("geometry") val geometry : Geometry = Geometry()
)
