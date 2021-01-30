package com.example.unsurvgame.models

import com.google.gson.annotations.SerializedName

data class GeoJSON (
    @SerializedName("type") val type : String = "FeatureCollection",
    @SerializedName("features") val features : MutableList<Feature> = mutableListOf()
)