package com.example.unsurvgame.models

import com.google.gson.annotations.SerializedName

data class FBGeoJSON(
    @SerializedName("type") val type : String = "FeatureCollection",
    @SerializedName("features") val features : HashMap<String, Feature> = hashMapOf()
)