package com.shivangi.mlKitDoc.data.local.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pdf_table")
data class PdfEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val uri: String,
    val name: String = "",
    val creationTime: Long
)