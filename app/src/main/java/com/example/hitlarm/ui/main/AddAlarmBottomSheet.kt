/*
 * ============================================================================
 * HitLarm - Android Anti-Oversleep Solution
 * ----------------------------------------------------------------------------
 * OPEN SOURCED
 * 
 * Ideation: Vikash Seelam
 * Place: Ingolstadt
 * Created completely by: Google Antigravity
 * Date: 06.07.2026
 * ============================================================================
 */
package com.example.hitlarm.ui.main

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.hitlarm.data.Alarm
import com.example.hitlarm.data.StopPreference
import com.example.hitlarm.theme.CyberCyan
import com.example.hitlarm.theme.DarkGrey
import com.example.hitlarm.theme.ElectricPurple
import com.example.hitlarm.theme.LightGrey
import com.example.hitlarm.theme.Obsidian
import com.google.mlkit.vision.common.InputImage
import java.util.Calendar
import java.util.UUID
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAlarmBottomSheet(
    alarm: Alarm?,
    onDismiss: () -> Unit,
    onSave: (Alarm) -> Unit
) {
    val initialHour = alarm?.hour ?: Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val initialMinute = alarm?.minute ?: Calendar.getInstance().get(Calendar.MINUTE)
    
    var is24HourFormat by remember { mutableStateOf(false) }
    var selectedHour by remember { mutableIntStateOf(initialHour) }
    var selectedMinute by remember { mutableIntStateOf(initialMinute) }

    val timePickerState = key(is24HourFormat) {
        rememberTimePickerState(
            initialHour = selectedHour,
            initialMinute = selectedMinute,
            is24Hour = is24HourFormat
        )
    }

    var label by remember { mutableStateOf(alarm?.label ?: "Wake Up!") }
    var selectedDays by remember { mutableStateOf(alarm?.days ?: emptySet()) }
    var stopPreference by remember { mutableStateOf(alarm?.stopPreference ?: StopPreference.NORMAL) }
    var targetCount by remember { mutableStateOf((alarm?.targetCount ?: 10).toFloat()) }
    var targetBarcode by remember { mutableStateOf(alarm?.targetBarcode ?: "") }
    var showBarcodeSetupScanner by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Obsidian,
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (alarm == null) "Set New Alarm" else "Edit Alarm",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = CyberCyan,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Format Selector: 12H vs 24H
            Row(
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkGrey)
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(false to "12 Hour", true to "24 Hour").forEach { (is24, labelStr) ->
                    val isSelected = is24HourFormat == is24
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) CyberCyan else Color.Transparent)
                            .clickable {
                                if (is24HourFormat != is24) {
                                    selectedHour = timePickerState.hour
                                    selectedMinute = timePickerState.minute
                                    is24HourFormat = is24
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = labelStr,
                            color = if (isSelected) Obsidian else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Time Picker
            TimePicker(
                state = timePickerState,
                colors = TimePickerDefaults.colors(
                    clockDialColor = DarkGrey,
                    clockDialSelectedContentColor = Obsidian,
                    clockDialUnselectedContentColor = Color.White,
                    selectorColor = CyberCyan,
                    periodSelectorBorderColor = CyberCyan,
                    periodSelectorSelectedContainerColor = CyberCyan,
                    periodSelectorUnselectedContainerColor = DarkGrey,
                    periodSelectorSelectedContentColor = Obsidian,
                    periodSelectorUnselectedContentColor = Color.White,
                    timeSelectorSelectedContainerColor = CyberCyan,
                    timeSelectorUnselectedContainerColor = DarkGrey,
                    timeSelectorSelectedContentColor = Obsidian,
                    timeSelectorUnselectedContentColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Label Input
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Alarm Label") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberCyan,
                    unfocusedBorderColor = DarkGrey,
                    focusedLabelColor = CyberCyan,
                    unfocusedLabelColor = LightGrey,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Weekdays Picker
            Text(
                text = "Repeat on days",
                fontSize = 14.sp,
                color = CyberCyan,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")
                for (i in 1..7) {
                    val dayNum = i
                    val isSelected = selectedDays.contains(dayNum)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) CyberCyan else DarkGrey)
                            .clickable {
                                selectedDays = if (isSelected) {
                                    selectedDays - dayNum
                                } else {
                                    selectedDays + dayNum
                                }
                            }
                    ) {
                        Text(
                            text = daysOfWeek[i - 1],
                            color = if (isSelected) Obsidian else Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stop Preference
            Text(
                text = "Dismiss Challenge",
                fontSize = 14.sp,
                color = CyberCyan,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        StopPreference.NORMAL to "Normal",
                        StopPreference.PUSHUPS to "Pushups",
                        StopPreference.SQUATS to "Squats"
                    ).forEach { (pref, title) ->
                        val isSelected = stopPreference == pref
                        Button(
                            onClick = { stopPreference = pref },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) ElectricPurple else DarkGrey,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        StopPreference.SHAKE to "Shake",
                        StopPreference.MATH to "Math",
                        StopPreference.BARCODE to "Barcode"
                    ).forEach { (pref, title) ->
                        val isSelected = stopPreference == pref
                        Button(
                            onClick = { stopPreference = pref },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) ElectricPurple else DarkGrey,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Target Configurations
            if (stopPreference != StopPreference.NORMAL && stopPreference != StopPreference.BARCODE) {
                var labelText = ""
                var minVal = 5f
                var maxVal = 30f
                var stepCount = 24

                when (stopPreference) {
                    StopPreference.SHAKE -> {
                        labelText = "Target Shakes: ${targetCount.toInt()}"
                        minVal = 10f
                        maxVal = 100f
                        stepCount = 8
                    }
                    StopPreference.MATH -> {
                        labelText = "Math Problems: ${targetCount.toInt()}"
                        minVal = 1f
                        maxVal = 10f
                        stepCount = 8
                    }
                    else -> {
                        labelText = "Target Reps: ${targetCount.toInt()}"
                        minVal = 5f
                        maxVal = 30f
                        stepCount = 24
                    }
                }

                // Adjust targetCount value if it falls out of range
                LaunchedEffect(stopPreference) {
                    if (stopPreference == StopPreference.SHAKE && (targetCount < 10f || targetCount > 100f)) {
                        targetCount = 30f
                    } else if (stopPreference == StopPreference.MATH && (targetCount < 1f || targetCount > 10f)) {
                        targetCount = 3f
                    } else if ((stopPreference == StopPreference.PUSHUPS || stopPreference == StopPreference.SQUATS) && (targetCount < 5f || targetCount > 30f)) {
                        targetCount = 10f
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = labelText,
                        fontSize = 14.sp,
                        color = CyberCyan,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Slider(
                    value = targetCount,
                    onValueChange = { targetCount = it },
                    valueRange = minVal..maxVal,
                    steps = stepCount,
                    colors = SliderDefaults.colors(
                        thumbColor = CyberCyan,
                        activeTrackColor = CyberCyan,
                        inactiveTrackColor = DarkGrey
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Barcode setup inputs
            if (stopPreference == StopPreference.BARCODE) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Target Barcode",
                    fontSize = 14.sp,
                    color = CyberCyan,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = targetBarcode,
                        onValueChange = { targetBarcode = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("e.g. 501234567890", color = LightGrey) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = DarkGrey,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )
                    Button(
                        onClick = { showBarcodeSetupScanner = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricPurple,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Text("Scan", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.horizontalGradient(listOf(CyberCyan, ElectricPurple)))
                    .clickable {
                        val newAlarm = Alarm(
                            id = alarm?.id ?: UUID.randomUUID().toString(),
                            hour = timePickerState.hour,
                            minute = timePickerState.minute,
                            label = label,
                            isActive = alarm?.isActive ?: true,
                            days = selectedDays,
                            stopPreference = stopPreference,
                            targetCount = targetCount.toInt(),
                            targetBarcode = if (stopPreference == StopPreference.BARCODE) targetBarcode else null
                        )
                        onSave(newAlarm)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Save Alarm",
                    color = Obsidian,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showBarcodeSetupScanner) {
        BarcodeScannerDialog(
            onDismiss = { showBarcodeSetupScanner = false },
            onBarcodeScanned = { barcode ->
                targetBarcode = barcode
                showBarcodeSetupScanner = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScannerDialog(
    onDismiss: () -> Unit,
    onBarcodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember { mutableStateOf(
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    ) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> hasCameraPermission = isGranted }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Obsidian)
        ) {
            if (hasCameraPermission) {
                Box(modifier = Modifier.fillMaxSize()) {
                    BarcodeCameraPreview(
                        onBarcodeScanned = { barcode ->
                            onBarcodeScanned(barcode)
                        }
                    )
                    
                    // Header Overlay
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(24.dp)
                            .align(Alignment.TopCenter),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Point camera at a barcode",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Cancel button overlay
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .padding(bottom = 32.dp)
                            .align(Alignment.BottomCenter)
                            .height(50.dp)
                            .width(200.dp)
                    ) {
                        Text("Cancel Scanner", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Camera Access Required", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Grant camera access to scan your target barcode.", color = LightGrey, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Obsidian)
                    ) {
                        Text("Grant Permission", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun BarcodeCameraPreview(
    onBarcodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    val previewView = remember { PreviewView(context) }
    val executor = remember { Executors.newSingleThreadExecutor() }

    val barcodeScanner = remember {
        val options = BarcodeScannerOptions.Builder()
            .build()
        BarcodeScanning.getClient(options)
    }

    DisposableEffect(Unit) {
        onDispose {
            executor.shutdown()
            barcodeScanner.close()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    ) { view ->
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = view.surfaceProvider
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(executor) { imageProxy ->
                val mediaImage = imageProxy.image
                if (mediaImage != null) {
                    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                    barcodeScanner.process(image)
                        .addOnSuccessListener { barcodes ->
                            val barcode = barcodes.firstOrNull()?.rawValue
                            if (barcode != null) {
                                onBarcodeScanned(barcode)
                            }
                        }
                        .addOnFailureListener {
                            // Suppress
                        }
                        .addOnCompleteListener {
                            imageProxy.close()
                        }
                } else {
                    imageProxy.close()
                }
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )
            } catch (exc: Exception) {
                // Suppress
            }
        }, ContextCompat.getMainExecutor(context))
    }
}
