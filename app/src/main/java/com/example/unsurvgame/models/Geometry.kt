package com.example.unsurvgame.models

import com.google.gson.annotations.SerializedName

data class Geometry(    @SerializedName("type") val type : String = "Point",
                        @SerializedName("coordinates") val coordinates : List<Double> = listOf(0.0, 0.0))