package com.basesoftware.tradinghelperkotlin.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.basesoftware.tradinghelperkotlin.model.WatchModel

@Database(entities = [WatchModel::class], version = 1)
abstract class WatchDatabase : RoomDatabase() {
    abstract fun watchDao() : WatchDao
}