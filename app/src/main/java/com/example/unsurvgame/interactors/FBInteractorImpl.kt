package com.example.unsurvgame.interactors

import com.example.unsurvgame.models.FBGeoJSON
import com.example.unsurvgame.models.Feature
import com.example.unsurvgame.models.Geometry
import com.example.unsurvgame.models.Properties
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.mapbox.geojson.Point
import io.reactivex.Observable
import io.reactivex.Single

class FBInteractorImpl : FBInteractor {
    private val db = Firebase.database

    override fun getGeoJson(): Observable<FBGeoJSON> {
        return Observable.create { emitter ->
            db.getReference("geojson").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    emitter.onNext(snapshot.getValue(FBGeoJSON::class.java) ?: FBGeoJSON())
                }

                override fun onCancelled(error: DatabaseError) {
                    emitter.onError(error.toException())
                }

            })
            }

        }

    override fun addFeatureToDB(point: Point): Single<Point> {
        return Single.create {
            val feature = Feature("Feature", Properties(), Geometry("Point", listOf(point.longitude(), point.latitude())))
            Firebase.database.getReference("geojson").child("features").push().setValue(feature)
            it.onSuccess(point)
        }
    }
}