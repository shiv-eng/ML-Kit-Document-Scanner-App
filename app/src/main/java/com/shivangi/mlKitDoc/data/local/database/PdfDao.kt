package com.shivangi.mlKitDoc.data.local.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
@Dao
interface PdfDao {
    @Insert
    suspend fun insertPdf(pdf: PdfEntity)

    @Delete
    suspend fun deletePdf(pdf: PdfEntity)

    @Update
    suspend fun updatePdf(pdf: PdfEntity)

    @Query("SELECT * FROM pdf_table")
    suspend fun getAllPdfs(): List<PdfEntity>
}