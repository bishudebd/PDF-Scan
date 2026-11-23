package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOff
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShutterSpeed
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.ScanMode
import com.example.engine.AutoCaptureDetectionState
import com.example.engine.DocumentScannerEngine
import com.example.engine.DocumentStabilityAnalyzer
import com.example.ui.components.ProcessingOverlay
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.CobaltBlue
import com.example.ui.theme.DeepNavy
import com.example.ui.viewmodel.ScannerEditorViewModel
import java.io.File
import java.util.concurrent.Executors

@Composable
fun CameraScannerScreen(
    viewModel: ScannerEditorViewModel,
    onClose: () -> Unit,
    onProceedToCrop: () -> Unit,
    onProceedToEnhance: () -> Unit,
    onOcrImageCaptured: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionRequestedOnce by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        permissionRequestedOnce = true
    }

    // Photo picker to import directly from scanner screen
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(20)
    ) { uris ->
        if (uris.isNotEmpty()) {
            if (state.scanMode == ScanMode.OCR) {
                viewModel.importImageUris(listOf(uris.first()), appendToExisting = false) {
                    val path = viewModel.uiState.value.pages.lastOrNull()?.rawImagePath
                    if (path != null) onOcrImageCaptured(path)
                }
            } else {
                viewModel.importImageUris(uris, appendToExisting = state.pages.isNotEmpty()) {
                    onProceedToCrop()
                }
            }
        }
    }

    // Google ML Kit Document Scanner ActivityResult launcher with cancellation & error handling
    val mlKitLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val outcome = viewModel.scannerEngine.parseMlKitActivityResult(result.resultCode, result.data)
        viewModel.handleMlKitOutcome(outcome) {
            onProceedToEnhance()
        }
    }

    fun launchMlKitSmartScanner() {
        val activity = context as? Activity ?: return
        val limit = if (state.isRescanningPage) 1 else DocumentScannerEngine.DEFAULT_MAX_PAGES
        viewModel.scannerEngine.prepareMlKitScanIntent(
            activity = activity,
            pageLimit = limit,
            allowGalleryImport = true,
            onIntentReady = { intentSender ->
                try {
                    mlKitLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                } catch (e: Exception) {
                    viewModel.reportScannerError("Could not start ML Kit scanner: ${e.localizedMessage ?: "Unknown error"}")
                }
            },
            onPreflightFailed = { preflight ->
                viewModel.reportMlKitPreflightFailure(preflight)
            },
            onError = { err ->
                viewModel.reportScannerError("$err — you can continue with the built-in CameraX scanner.")
            }
        )
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Show structured info, error, and Play Services fallback notices
    LaunchedEffect(state.infoMessage, state.errorMessage, state.playServicesNotice) {
        val msg = state.errorMessage ?: state.playServicesNotice ?: state.infoMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearNotices()
        }
    }

    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_AUTO) }
    var showGrid by remember { mutableStateOf(true) }
    var isCapturing by remember { mutableStateOf(false) }
    var autoCaptureState by remember { mutableStateOf(AutoCaptureDetectionState()) }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build()
    }

    LaunchedEffect(flashMode) {
        imageCapture.flashMode = flashMode
    }

    val capturePhotoNow: () -> Unit = {
        if (hasCameraPermission && !isCapturing && viewModel.ensureStorageAvailable()) {
            isCapturing = true
            val outDir = File(context.cacheDir, "scans").apply { mkdirs() }
            val photoFile = File(outDir, "capture_${System.currentTimeMillis()}.jpg")
            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        isCapturing = false
                        if (state.scanMode == ScanMode.OCR) {
                            onOcrImageCaptured(photoFile.absolutePath)
                        } else {
                            viewModel.addCapturedFile(
                                file = photoFile,
                                onIdCardComplete = { onProceedToEnhance() },
                                onRescanComplete = { onProceedToCrop() }
                            )
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        isCapturing = false
                        viewModel.reportScannerError("Capture failed: ${exception.localizedMessage ?: "Camera error"}")
                    }
                }
            )
        }
    }

    val latestAutoCaptureEnabled by rememberUpdatedState(state.autoCaptureEnabled && state.scanMode != ScanMode.OCR)
    val latestIsCapturing by rememberUpdatedState(isCapturing)
    val latestCaptureAction by rememberUpdatedState(capturePhotoNow)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("camera_scanner_screen")
    ) {
        if (hasCameraPermission) {
            val previewView = remember {
                PreviewView(context).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
            }
            val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

            DisposableEffect(lifecycleOwner) {
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                val mainExecutor = ContextCompat.getMainExecutor(context)
                val analyzer = DocumentStabilityAnalyzer(
                    isAutoCaptureEnabled = { latestAutoCaptureEnabled },
                    isCaptureBusy = { latestIsCapturing },
                    onStateChanged = { detected ->
                        mainExecutor.execute { autoCaptureState = detected }
                    },
                    onAutoTriggerCapture = {
                        mainExecutor.execute { latestCaptureAction() }
                    }
                )
                val listener = Runnable {
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }
                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build().also {
                                it.setAnalyzer(analysisExecutor, analyzer)
                            }
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageCapture,
                            analysis
                        )
                    } catch (e: Exception) {
                        viewModel.reportScannerError("Camera unavailable: ${e.localizedMessage ?: "Check hardware"}")
                    }
                }
                cameraProviderFuture.addListener(listener, mainExecutor)
                onDispose {
                    try {
                        cameraProviderFuture.get().unbindAll()
                    } catch (_: Exception) {
                    }
                    analysisExecutor.shutdown()
                }
            }

            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )

            // Crisp Document / ID Card Viewfinder Guide Overlay reacting live to auto-detection
            ScannerViewfinderOverlay(
                scanMode = state.scanMode,
                showGrid = showGrid,
                documentLocked = state.autoCaptureEnabled && autoCaptureState.documentDetected,
                idCardBackStep = state.scanMode == ScanMode.ID_CARD && state.idCardFrontPath != null
            )
        } else {
            // Permission Denied / Prompt UI with App Settings, ML Kit Smart Auto-Scan & Gallery alternatives
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DeepNavy)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(CobaltBlue.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = stringResource(R.string.camera_permission_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.camera_permission_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.78f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("grant_camera_permission_button")
                ) {
                    Text(stringResource(R.string.camera_grant_permission))
                }
                if (permissionRequestedOnce) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.Filled.Settings, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open App Settings", color = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { launchMlKitSmartScanner() },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, CobaltBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Filled.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Use ML Kit Smart Scanner", color = Color.White)
                }
                Spacer(modifier = Modifier.height(10.dp))
                TextButton(
                    onClick = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                ) {
                    Text("Import Photos From Gallery", color = Color.White.copy(alpha = 0.85f))
                }
            }
        }

        // Top Controls Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 12.dp)
                .align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = {
                    viewModel.clearRescanMode()
                    onClose()
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.52f))
                    .testTag("scanner_close_button")
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Close scanner", tint = Color.White)
            }

            // Google ML Kit Document Scanner Pill
            Surface(
                onClick = { launchMlKitSmartScanner() },
                shape = RoundedCornerShape(20.dp),
                color = CobaltBlue.copy(alpha = 0.94f),
                modifier = Modifier.testTag("scanner_mlkit_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (state.isRescanningPage) "ML Kit Rescan" else "ML Kit Scanner",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Auto-Capture Toggle
                IconButton(
                    onClick = { viewModel.toggleAutoCapture() },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if (state.autoCaptureEnabled) CobaltBlue.copy(alpha = 0.85f)
                            else Color.Black.copy(alpha = 0.52f)
                        )
                        .testTag("scanner_auto_capture_toggle")
                ) {
                    Icon(
                        imageVector = Icons.Filled.ShutterSpeed,
                        contentDescription = if (state.autoCaptureEnabled) "Auto capture ON" else "Auto capture OFF",
                        tint = Color.White
                    )
                }
                IconButton(
                    onClick = { showGrid = !showGrid },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.52f))
                ) {
                    Icon(
                        imageVector = if (showGrid) Icons.Filled.GridOn else Icons.Filled.GridOff,
                        contentDescription = "Toggle grid",
                        tint = Color.White
                    )
                }
                IconButton(
                    onClick = {
                        flashMode = when (flashMode) {
                            ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
                            ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_OFF
                            else -> ImageCapture.FLASH_MODE_AUTO
                        }
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.52f))
                ) {
                    Icon(
                        imageVector = when (flashMode) {
                            ImageCapture.FLASH_MODE_ON -> Icons.Filled.FlashOn
                            ImageCapture.FLASH_MODE_OFF -> Icons.Filled.FlashOff
                            else -> Icons.Filled.FlashAuto
                        },
                        contentDescription = "Toggle flash",
                        tint = Color.White
                    )
                }
            }
        }

        // Live Guidance Banner (Rescan / ID Card / Auto-Capture Progress / OCR)
        val bannerText = when {
            state.isRescanningPage ->
                "Rescanning Page ${(state.rescanPageIndex ?: 0) + 1} · Capture replacement"
            state.scanMode == ScanMode.ID_CARD && state.idCardFrontPath == null ->
                "Step 1 of 2 · Align FRONT of ID Card inside frame"
            state.scanMode == ScanMode.ID_CARD && state.idCardFrontPath != null ->
                "Step 2 of 2 · Flip card and capture BACK side"
            state.scanMode == ScanMode.OCR ->
                "Align printed text clearly inside frame for OCR"
            state.autoCaptureEnabled ->
                "Auto-Capture ON · ${autoCaptureState.guidanceText}"
            else ->
                "Manual Capture · Tap shutter to capture page"
        }
        if (hasCameraPermission) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (state.isRescanningPage) AccentAmber.copy(alpha = 0.92f)
                else DeepNavy.copy(alpha = 0.86f),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 72.dp, start = 16.dp, end = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (state.autoCaptureEnabled && autoCaptureState.holdProgress > 0.05f && !state.isRescanningPage) {
                        CircularProgressIndicator(
                            progress = { autoCaptureState.holdProgress },
                            color = AccentEmerald,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = bannerText,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                }
            }
        }

        // Bottom Camera Control Dock
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.76f))
                .navigationBarsPadding()
                .padding(top = 12.dp, bottom = 18.dp, start = 20.dp, end = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Mode Selector Pills (Document | ID Card | OCR)
            if (!state.isRescanningPage) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ScanMode.entries.forEach { mode ->
                        val selected = state.scanMode == mode
                        Surface(
                            onClick = { viewModel.setScanMode(mode) },
                            shape = RoundedCornerShape(16.dp),
                            color = if (selected) CobaltBlue else Color.White.copy(alpha = 0.14f),
                            modifier = Modifier.testTag("scan_mode_${mode.name.lowercase()}")
                        ) {
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Gallery | Shutter | Multi-Page Counter & Proceed
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                        .padding(8.dp)
                        .testTag("scanner_gallery_button")
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = "Import from gallery", tint = Color.White, modifier = Modifier.size(26.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Gallery", style = MaterialTheme.typography.labelSmall, color = Color.White)
                }

                // Shutter Capture Button with Live Auto-Capture Progress Ring
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(82.dp)
                ) {
                    if (state.autoCaptureEnabled && autoCaptureState.holdProgress > 0.05f) {
                        CircularProgressIndicator(
                            progress = { autoCaptureState.holdProgress },
                            color = AccentEmerald,
                            strokeWidth = 4.dp,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .border(4.dp, Color.White, CircleShape)
                            .padding(6.dp)
                            .clip(CircleShape)
                            .background(if (isCapturing) CobaltBlue else Color.White)
                            .testTag("camera_shutter_button")
                            .clickable(enabled = hasCameraPermission && !isCapturing) {
                                capturePhotoNow()
                            }
                    )
                }

                if (state.pages.isNotEmpty() && !state.isRescanningPage) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(CobaltBlue)
                            .clickable(onClick = onProceedToCrop)
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .testTag("scanner_proceed_button")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .border(1.5.dp, Color.White, RoundedCornerShape(6.dp))
                        ) {
                            AsyncImage(
                                model = File(state.pages.last().rawImagePath),
                                contentDescription = "Last scanned page",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${state.pages.size}",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Edit scanned pages",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(64.dp))
                }
            }
        }

        // Snackbar Host floating cleanly above dock
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 148.dp)
        )

        ProcessingOverlay(visible = state.isProcessing, label = state.processingLabel)
    }

    // Low-Storage Pre-flight Dialog
    state.lowStorageStatus?.let { low ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissLowStorageDialog() },
            icon = { Icon(Icons.Filled.WarningAmber, null, tint = AccentAmber) },
            title = { Text("Low Device Storage") },
            text = {
                Text(
                    "Only ${low.availableMb} MB is free on your device (${low.requiredMb} MB recommended for high-resolution scanning). Clear temporary scan cache or free device space before continuing."
                )
            },
            confirmButton = {
                Button(onClick = { viewModel.clearScanCacheAndRecheckStorage() }) {
                    Text("Clear Scan Cache")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissLowStorageDialog() }) {
                    Text("Dismiss")
                }
            }
        )
    }
}

@Composable
private fun ScannerViewfinderOverlay(
    scanMode: ScanMode,
    showGrid: Boolean,
    documentLocked: Boolean,
    idCardBackStep: Boolean
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        val frameW = if (scanMode == ScanMode.ID_CARD) w * 0.86f else w * 0.84f
        val frameH = if (scanMode == ScanMode.ID_CARD) frameW / 1.586f else h * 0.56f
        val left = (w - frameW) / 2f
        val top = (h - frameH) / 2f - h * 0.04f

        if (showGrid) {
            val gridColor = Color.White.copy(alpha = 0.22f)
            drawLine(gridColor, Offset(left + frameW / 3f, top), Offset(left + frameW / 3f, top + frameH), 1.5f)
            drawLine(gridColor, Offset(left + 2 * frameW / 3f, top), Offset(left + 2 * frameW / 3f, top + frameH), 1.5f)
            drawLine(gridColor, Offset(left, top + frameH / 3f), Offset(left + frameW, top + frameH / 3f), 1.5f)
            drawLine(gridColor, Offset(left, top + 2 * frameH / 3f), Offset(left + frameW, top + 2 * frameH / 3f), 1.5f)
        }

        val accent = when {
            idCardBackStep || documentLocked -> Color(0xFF10B981)
            else -> CobaltBlue
        }

        drawRoundRect(
            color = accent.copy(alpha = if (documentLocked) 0.72f else 0.45f),
            topLeft = Offset(left, top),
            size = Size(frameW, frameH),
            cornerRadius = CornerRadius(20f, 20f),
            style = Stroke(width = if (documentLocked) 4f else 2.5f)
        )

        val cornerLen = 46f
        val sw = 8f
        drawLine(accent, Offset(left, top + cornerLen), Offset(left, top), sw, StrokeCap.Round)
        drawLine(accent, Offset(left, top), Offset(left + cornerLen, top), sw, StrokeCap.Round)
        drawLine(accent, Offset(left + frameW - cornerLen, top), Offset(left + frameW, top), sw, StrokeCap.Round)
        drawLine(accent, Offset(left + frameW, top), Offset(left + frameW, top + cornerLen), sw, StrokeCap.Round)
        drawLine(accent, Offset(left, top + frameH - cornerLen), Offset(left, top + frameH), sw, StrokeCap.Round)
        drawLine(accent, Offset(left, top + frameH), Offset(left + cornerLen, top + frameH), sw, StrokeCap.Round)
        drawLine(accent, Offset(left + frameW - cornerLen, top + frameH), Offset(left + frameW, top + frameH), sw, StrokeCap.Round)
        drawLine(accent, Offset(left + frameW, top + frameH - cornerLen), Offset(left + frameW, top + frameH), sw, StrokeCap.Round)
    }
}
