package com.example.unsurvgame.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.example.unsurvgame.interactors.FBInteractorImpl
import com.example.unsurvgame.models.FBGeoJSON
import com.example.unsurvgame.models.GeoJSON
import com.mapbox.geojson.Point
import com.orhanobut.logger.Logger
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableObserver
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers

class MainActivityVM(app : Application) : AndroidViewModel(app)  {
    private val disposer = CompositeDisposable()

    private val fbInteractor = FBInteractorImpl()

    val geoJSON = MutableLiveData<GeoJSON>()

    fun getGeoJson() {
        disposer.add(
            fbInteractor.getGeoJson().subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableObserver<FBGeoJSON>(){
                    override fun onNext(t: FBGeoJSON) {
                        val converted = GeoJSON(t.type, t.features.values.toMutableList())
                        geoJSON.value = converted
                    }

                    override fun onError(e: Throwable) {
                        throw e
                    }

                    override fun onComplete() {
                        TODO("Not yet implemented")
                    }
                })
        )
    }
    fun addPointToDB(point: Point) {
        disposer.add(
            fbInteractor.addFeatureToDB(point).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableSingleObserver<Point>(){
                    override fun onSuccess(t: Point) {
                        Logger.d("Added feature: %s", t)
                    }

                    override fun onError(e: Throwable) {
                        throw e
                    }

                })
        )
    }
}