package com.shivangi.mlKitDoc.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [PdfEntity::class], version = 1)
abstract class PdfDatabase : RoomDatabase() {
    abstract fun pdfDao(): PdfDao
}