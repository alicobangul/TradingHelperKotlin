package com.basesoftware.tradinghelperkotlin.model

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "Watch")
data class WatchModel(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "shareCode")
    val shareCode : String
) : Parcelable
