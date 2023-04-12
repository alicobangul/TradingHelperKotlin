package com.basesoftware.tradinghelperkotlin.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.basesoftware.tradinghelperkotlin.model.WatchModel
import io.reactivex.Completable
import io.reactivex.Single

@Dao
interface WatchDao {

    @Query("SELECT * FROM Watch")
    fun getWatchListCoroutines() : List<WatchModel>

    @Query("SELECT * FROM Watch WHERE shareCode IN (:shareCode)")
    fun getWatchDataCoroutines(shareCode: String): WatchModel?

    @Insert
    fun setWatchDataCoroutines(watchModel: WatchModel)

    @Delete
    fun deleteWatchDataCoroutines(watchModel: WatchModel)


    @Query("SELECT * FROM Watch")
    fun getWatchListRxjava() : Single<List<WatchModel>>

    @Query("SELECT * FROM Watch WHERE shareCode IN (:shareCode)")
    fun getWatchDataRxjava(shareCode: String): Single<WatchModel?>

    @Insert
    fun setWatchDataRxjava(watchModel: WatchModel) : Completable

    @Delete
    fun deleteWatchDataRxjava(watchModel: WatchModel) : Completable

}