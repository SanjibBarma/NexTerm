package com.nexterm.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nexterm.app.data.local.dao.CommandHistoryDao
import com.nexterm.app.data.local.dao.SessionDao
import com.nexterm.app.data.local.entity.CommandHistoryEntity
import com.nexterm.app.data.local.entity.SessionEntity
import com.nexterm.app.data.local.converter.Converters

@Database(
    entities = [
        SessionEntity::class,
        CommandHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class NexTermDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun commandHistoryDao(): CommandHistoryDao
}