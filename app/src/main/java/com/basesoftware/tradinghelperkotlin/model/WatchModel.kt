package com.basesoftware.tradinghelperkotlin.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Watch")
data class WatchModel(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "shareCode")
    val shareCode : String
)
