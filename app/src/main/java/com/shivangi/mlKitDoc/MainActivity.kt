package com.shivangi.mlKitDoc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.room.Room
import com.shivangi.mlKitDoc.data.local.database.PdfDatabase
import com.shivangi.mlKitDoc.ui.screen.HomeScreen
import com.shivangi.mlKitDoc.ui.theme.DocumentScannerTheme

class MainActivity : ComponentActivity() {
    lateinit var pdfDatabase: PdfDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pdfDatabase = Room.databaseBuilder(applicationContext, PdfDatabase::class.java, "pdf_database").build()

        setContent {
            DocumentScannerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HomeScreen(pdfDatabase)
                }
            }
        }
    }
}

