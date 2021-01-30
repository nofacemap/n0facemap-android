package com.example.unsurvgame.interactors

import com.example.unsurvgame.models.FBGeoJSON
import com.mapbox.geojson.Point
import io.reactivex.Observable
import io.reactivex.Single

interface FBInteractor {
    fun getGeoJson(): Observable<FBGeoJSON>
    fun addFeatureToDB(point: Point): Single<Point>
}