package com.shivangi.mlKitDoc.ui.screen

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.shivangi.mlKitDoc.R
import com.shivangi.mlKitDoc.data.local.database.PdfDatabase
import com.shivangi.mlKitDoc.data.local.database.PdfEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(pdfDatabase: PdfDatabase) {
    val activity = LocalContext.current as Activity
    val scannerOptions = GmsDocumentScannerOptions.Builder()
        .setGalleryImportAllowed(true)
        .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_PDF)
        .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
        .build()

    // Initialize the docs list with existing PDFs from the database
    var docs by remember { mutableStateOf<List<PdfEntity>>(emptyList()) }

    val scanner = GmsDocumentScanning.getClient(scannerOptions)
    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanningResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            scanningResult?.pdf?.let { pdf ->
                val pdfEntity = PdfEntity(uri = pdf.uri.toString(), creationTime = System.currentTimeMillis())
                GlobalScope.launch(Dispatchers.IO) {
                    pdfDatabase.pdfDao().insertPdf(pdfEntity)
                    docs = pdfDatabase.pdfDao().getAllPdfs() // Update docs list from the database
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        // Fetch PDFs asynchronously when the composable is initially launched
        val pdfs = withContext(Dispatchers.IO) {
            pdfDatabase.pdfDao().getAllPdfs()
        }
        docs = pdfs
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                colors = TopAppBarDefaults.mediumTopAppBarColors()
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    scanner.getStartScanIntent(activity).addOnSuccessListener { intentSender ->
                        scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                    }.addOnFailureListener {
                        Log.d("TAG", "HomeScreen: ${it.message}")
                    }
                },
                text = { Text(text = stringResource(R.string.scan)) },
                icon = { Icon(painterResource(id = R.drawable.ic_camera), contentDescription = null) }
            )
        },
        content = { paddingValues ->
            Surface(modifier = Modifier.padding(paddingValues)) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(10.dp)
                ) {
                    items(items = docs, key = { it.id }) { pdf ->
                        var showRenameDialog by remember { mutableStateOf(false) }
                        if (showRenameDialog) {
                            RenameDialog(
                                initialName = pdf.name,
                                onConfirm = { newName ->
                                    val updatedPdf = pdf.copy(name = newName)
                                    GlobalScope.launch(Dispatchers.IO) {
                                        pdfDatabase.pdfDao().updatePdf(updatedPdf)
                                        // Update in-memory list
                                        docs = pdfDatabase.pdfDao().getAllPdfs()
                                    }
                                    showRenameDialog = false
                                },
                                onDismiss = { showRenameDialog = false }
                            )
                        }

                        SwipeToDeleteContainer(
                            item = pdf,
                            onDelete = {
                                GlobalScope.launch(Dispatchers.IO) {
                                    pdfDatabase.pdfDao().deletePdf(pdf)
                                    // Update in-memory list
                                    docs = pdfDatabase.pdfDao().getAllPdfs()
                                }
                            }
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .padding(10.dp),
                                onClick = {
                                    val pdfUri = Uri.parse(pdf.uri)
                                    val pdfFileUri = FileProvider.getUriForFile(
                                        activity,
                                        activity.packageName + ".provider",
                                        pdfUri.toFile()
                                    )
                                    val browserIntent = Intent(Intent.ACTION_VIEW, pdfFileUri)
                                    browserIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    activity.startActivity(browserIntent)
                                }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Image(
                                        modifier = Modifier.size(80.dp),
                                        painter = painterResource(id = R.drawable.ic_pdf),
                                        contentDescription = null,
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = pdf.name.ifEmpty { Uri.parse(pdf.uri).lastPathSegment ?: "Document" },
                                            style = MaterialTheme.typography.bodyLarge,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Size: ${getFileSize(Uri.parse(pdf.uri))}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.Gray
                                        )
                                    }
                                    IconButton(onClick = { showRenameDialog = true }) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun RenameDialog(initialName: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initialName) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.rename_pdf), style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { newText -> text = newText },

                    label = { Text(stringResource(R.string.pdf_name)) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    Button(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onConfirm(text) }) { Text(stringResource(R.string.update)) }
                }
            }
        }
    }
}

fun getFileSize(uri: Uri): String {
    val file = File(uri.path ?: "")
    val fileSizeInBytes = file.length()
    val fileSizeInKB = fileSizeInBytes / 1024
    return if (fileSizeInKB > 1024) {
        val fileSizeInMB = fileSizeInKB / 1024
        "$fileSizeInMB MB"
    } else {
        "$fileSizeInKB KB"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SwipeToDeleteContainer(
    item: T,
    onDelete: (T) -> Unit,
    animationDuration: Int = 500,
    content: @Composable (T) -> Unit
) {
    var isRemoved by remember {
        mutableStateOf(false)
    }
    val state = rememberDismissState(
        confirmValueChange = { value ->
            if (value == DismissValue.DismissedToStart) {
                isRemoved = true
                true
            } else {
                false
            }
        }
    )

    LaunchedEffect(key1 = isRemoved) {
        if (isRemoved) {
            delay(animationDuration.toLong())
            onDelete(item)
        }
    }

    AnimatedVisibility(
        visible = !isRemoved,
        exit = shrinkVertically(
            animationSpec = tween(durationMillis = animationDuration),
            shrinkTowards = Alignment.Top
        ) + fadeOut()
    ) {
        SwipeToDismiss(
            state = state,
            background = {
                DeleteBackground(swipeDismissState = state)
            },
            dismissContent = { content(item) },
            directions = setOf(DismissDirection.EndToStart)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteBackground(
    swipeDismissState: DismissState
) {
    val color = if (swipeDismissState.dismissDirection == DismissDirection.EndToStart) {
        Color.Red
    } else Color.Transparent

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color)
            .padding(16.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = null,
            tint = Color.White
        )
    }
}