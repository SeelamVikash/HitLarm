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
package com.example.hitlarm

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.hitlarm.data.Alarm
import com.example.hitlarm.data.AlarmScheduler
import com.example.hitlarm.data.AlarmService
import com.example.hitlarm.data.DefaultDataRepository
import com.example.hitlarm.data.StopPreference
import com.example.hitlarm.pose.PostureDetector
import com.example.hitlarm.theme.CyberCyan
import com.example.hitlarm.theme.DarkGrey
import com.example.hitlarm.theme.ElectricPurple
import com.example.hitlarm.theme.HitLarmTheme
import com.example.hitlarm.theme.LightGrey
import com.example.hitlarm.theme.Obsidian
import com.example.hitlarm.theme.NeonGreen
import com.example.hitlarm.theme.NeonPink
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class AlarmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Bypass Lock Screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val km = getSystemService(Context.KEYGUARD_SERVICE) as android.app.KeyguardManager
            km.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val alarmId = intent.getStringExtra("extra_alarm_id") ?: ""

        setContent {
            HitLarmTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Obsidian
                ) {
                    AlarmTriggerScreen(
                        alarmId = alarmId,
                        onDismissed = {
                            finish()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AlarmTriggerScreen(
    alarmId: String,
    onDismissed: () -> Unit
) {
    val context = LocalContext.current.applicationContext
    val repository = remember { DefaultDataRepository(context) }
    val scheduler = remember { AlarmScheduler(context) }

    var alarm by remember { mutableStateOf<Alarm?>(null) }
    var currentCount by remember { mutableStateOf(0) }
    var feedbackText by remember { mutableStateOf("Position camera to show full body") }
    var hasCameraPermission by remember { mutableStateOf(false) }
    var isFinishedState by remember { mutableStateOf(false) }
    var isSkipped by remember { mutableStateOf(false) }
    var showSkipDialog by remember { mutableStateOf(false) }
    var skipReason by remember { mutableStateOf("") }

    // Math challenge state
    var mathProblem by remember { mutableStateOf("") }
    var mathAnswer by remember { mutableStateOf(0) }
    var userInputAnswer by remember { mutableStateOf("") }

    // Pulsing Animation for Time
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // Load alarm
    LaunchedEffect(alarmId) {
        val loaded = repository.getAlarmsList().firstOrNull { it.id == alarmId }
        alarm = loaded ?: Alarm(hour = 7, minute = 0) // Default fallback
    }

    // Camera Permission Launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasCameraPermission = isGranted
        }
    )

    LaunchedEffect(alarm) {
        val currentAlarm = alarm ?: return@LaunchedEffect
        if (currentAlarm.stopPreference == StopPreference.PUSHUPS || 
            currentAlarm.stopPreference == StopPreference.SQUATS || 
            currentAlarm.stopPreference == StopPreference.BARCODE) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            } else {
                hasCameraPermission = true
            }
        }
        if (currentAlarm.stopPreference == StopPreference.MATH) {
            val prob = generateMathProblem()
            mathProblem = prob.first
            mathAnswer = prob.second
            feedbackText = "Solve the equation to stop the alarm"
        }
        if (currentAlarm.stopPreference == StopPreference.SHAKE) {
            feedbackText = "Shake phone vigorously!"
        }
    }

    // Shake challenge accelerometer sensor logic
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val accelerometer = remember { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }
    val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_ALARM, 80) }

    if (alarm?.stopPreference == StopPreference.SHAKE && !isFinishedState) {
        val target = alarm?.targetCount ?: 30
        DisposableEffect(alarm) {
            var lastUpdate: Long = 0
            var shakeCount = 0

            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]

                    // Calculate acceleration magnitude minus gravity
                    val gForce = Math.sqrt((x * x + y * y + z * z).toDouble()).toFloat() - SensorManager.GRAVITY_EARTH

                    val now = System.currentTimeMillis()
                    if (gForce > 3.2f) { // Shaken
                        if (now - lastUpdate > 300) { // Debounce shakes by 300ms
                            lastUpdate = now
                            shakeCount++
                            currentCount = minOf(target, shakeCount)
                            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
                            feedbackText = "Keep shaking! ${target - currentCount} remaining."
                            if (shakeCount >= target) {
                                isFinishedState = true
                            }
                        }
                    }
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }

            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)

            onDispose {
                sensorManager.unregisterListener(listener)
            }
        }
    }

    // Dismiss Handler
    val handleDismiss = {
        val currentAlarm = alarm
        if (currentAlarm != null) {
            if (!isSkipped) {
                // Record streak wakeup
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                val timeStr = SimpleDateFormat("hh:mm a", Locale.US).format(Date())
                repository.recordWakeUp(todayStr, timeStr)
            }

            // Reschedule alarm if repeating, else disable it
            if (currentAlarm.days.isEmpty()) {
                repository.updateAlarmActiveState(currentAlarm.id, false)
            } else {
                scheduler.schedule(currentAlarm)
            }
        }

        // Stop Audio Playback Service
        context.stopService(Intent(context, AlarmService::class.java))

        // Cancel Notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(1001)

        onDismissed()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Large Clock Display
        val timeString = remember {
            val cal = Calendar.getInstance()
            val format = SimpleDateFormat("hh:mm", Locale.US)
            val amPm = SimpleDateFormat("a", Locale.US)
            Pair(format.format(cal.time), amPm.format(cal.time))
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 32.dp)
        ) {
            Text(
                text = timeString.first,
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.scale(scale)
            )
            Text(
                text = timeString.second,
                fontSize = 20.sp,
                color = CyberCyan,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = alarm?.label ?: "Wake Up!",
                fontSize = 18.sp,
                color = LightGrey,
                textAlign = TextAlign.Center
            )
        }

        val activeAlarm = alarm
        if (activeAlarm == null) {
            CircularProgressIndicator(color = CyberCyan)
        } else {
            if (showSkipDialog) {
                AlertDialog(
                    onDismissRequest = { showSkipDialog = false },
                    title = { Text("Skip Challenge?", color = Color.White, fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            Text(
                                text = "To skip the challenge and silence the alarm, you must type a reason below. Note that skipping will freeze and protect your active wakeup streak.",
                                color = LightGrey,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = skipReason,
                                onValueChange = { skipReason = it },
                                placeholder = { Text("Enter reason (e.g., injured, tired, running late)", color = LightGrey) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = ElectricPurple,
                                    focusedBorderColor = ElectricPurple,
                                    unfocusedBorderColor = Color.Gray
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showSkipDialog = false
                                isSkipped = true
                                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                                val timeStr = SimpleDateFormat("hh:mm a", Locale.US).format(Date())
                                repository.recordSkip(todayStr, timeStr, skipReason)
                                context.stopService(Intent(context, AlarmService::class.java))
                                isFinishedState = true
                            },
                            enabled = skipReason.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple, contentColor = Color.White)
                        ) {
                            Text("Skip & Silence Alarm", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showSkipDialog = false }) {
                            Text("Cancel", color = Color.White)
                        }
                    },
                    containerColor = Obsidian,
                    textContentColor = Color.White
                )
            }

            when (activeAlarm.stopPreference) {
                StopPreference.NORMAL -> {
                    // Normal Alarm Trigger Layout
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 48.dp)
                    ) {
                        Button(
                            onClick = handleDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Obsidian),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().height(60.dp)
                        ) {
                            Text("Dismiss Alarm", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                // Snooze: Reschedule for 5 mins in future, stop service
                                val snoozeCal = Calendar.getInstance().apply {
                                    add(Calendar.MINUTE, 5)
                                }
                                val snoozedAlarm = activeAlarm.copy(
                                    hour = snoozeCal.get(Calendar.HOUR_OF_DAY),
                                    minute = snoozeCal.get(Calendar.MINUTE),
                                    isActive = true
                                )
                                scheduler.schedule(snoozedAlarm)

                                context.stopService(Intent(context, AlarmService::class.java))
                                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                                notificationManager.cancel(1001)

                                onDismissed()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkGrey, contentColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().height(60.dp)
                        ) {
                            Text("Snooze (5 Mins)", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                StopPreference.PUSHUPS, StopPreference.SQUATS -> {
                    if (isFinishedState) {
                        FinishedExerciseScreen(isSkipped, handleDismiss)
                    } else if (!hasCameraPermission) {
                        CameraPermissionLayout {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    } else {
                        // Live CV Exercise tracking layout
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Camera Preview Box
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(320.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .border(2.dp, Brush.horizontalGradient(listOf(CyberCyan, ElectricPurple)), RoundedCornerShape(24.dp))
                                    .background(Color.Black)
                            ) {
                                CameraTrackingView(
                                    isPushUp = activeAlarm.stopPreference == StopPreference.PUSHUPS,
                                    targetCount = activeAlarm.targetCount,
                                    onUpdate = { count, feedback ->
                                        currentCount = count
                                        feedbackText = feedback
                                    },
                                    onFinished = {
                                        isFinishedState = true
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Counter stats
                            Text(
                                text = "${currentCount} / ${activeAlarm.targetCount}",
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberCyan
                            )
                            Text(
                                text = if (activeAlarm.stopPreference == StopPreference.PUSHUPS) "Pushups Completed" else "Squats Completed",
                                fontSize = 12.sp,
                                color = LightGrey,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Custom feedback text panel
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DarkGrey)
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = feedbackText,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Debug button for testing in emulator
                            Button(
                                onClick = { showSkipDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ElectricPurple,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text("Skip the Challenge", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                StopPreference.SHAKE -> {
                    if (isFinishedState) {
                        FinishedExerciseScreen(isSkipped, handleDismiss)
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Spacer(modifier = Modifier.height(32.dp))
                            Text("📳", fontSize = 72.sp)
                            Spacer(modifier = Modifier.height(32.dp))
                            Text(
                                text = "${currentCount} / ${activeAlarm.targetCount}",
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberCyan
                            )
                            Text(
                                text = "Shakes Completed",
                                fontSize = 12.sp,
                                color = LightGrey,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DarkGrey)
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = feedbackText,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(modifier = Modifier.height(32.dp))
                            Button(
                                onClick = { showSkipDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple, contentColor = Color.White),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text("Skip the Challenge", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                StopPreference.MATH -> {
                    if (isFinishedState) {
                        FinishedExerciseScreen(isSkipped, handleDismiss)
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("🧮", fontSize = 48.sp)
                            Text(
                                text = "Problem ${currentCount + 1} of ${activeAlarm.targetCount}",
                                fontSize = 16.sp,
                                color = LightGrey,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "$mathProblem = ?",
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberCyan
                            )
                            OutlinedTextField(
                                value = userInputAnswer,
                                onValueChange = { userInputAnswer = it },
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                ),
                                placeholder = { Text("Enter answer", color = LightGrey) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyberCyan,
                                    unfocusedBorderColor = DarkGrey,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                            )
                            Button(
                                onClick = {
                                    val ans = userInputAnswer.toIntOrNull()
                                    if (ans == mathAnswer) {
                                        val newC = currentCount + 1
                                        currentCount = newC
                                        userInputAnswer = ""
                                        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
                                        if (newC >= activeAlarm.targetCount) {
                                            isFinishedState = true
                                        } else {
                                            val prob = generateMathProblem()
                                            mathProblem = prob.first
                                            mathAnswer = prob.second
                                            feedbackText = "Correct! Solved ${newC} problems."
                                        }
                                    } else {
                                        feedbackText = "Incorrect answer, try again!"
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Obsidian),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(50.dp).padding(horizontal = 24.dp)
                            ) {
                                Text("Submit Answer", fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = feedbackText,
                                color = if (feedbackText.contains("Incorrect")) NeonPink else NeonGreen,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Button(
                                onClick = { showSkipDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple, contentColor = Color.White),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text("Skip the Challenge", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                StopPreference.BARCODE -> {
                    if (isFinishedState) {
                        FinishedExerciseScreen(isSkipped, handleDismiss)
                    } else if (!hasCameraPermission) {
                        CameraPermissionLayout {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(320.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .border(2.dp, Brush.horizontalGradient(listOf(CyberCyan, ElectricPurple)), RoundedCornerShape(24.dp))
                                    .background(Color.Black)
                            ) {
                                BarcodeTrackingView(
                                    targetBarcode = activeAlarm.targetBarcode ?: "",
                                    onFinished = {
                                        isFinishedState = true
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = "Scan Target Barcode",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberCyan
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Scan: ${activeAlarm.targetBarcode ?: "No barcode configured"}",
                                fontSize = 14.sp,
                                color = LightGrey
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showSkipDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple, contentColor = Color.White),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text("Skip the Challenge", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CameraTrackingView(
    isPushUp: Boolean,
    targetCount: Int,
    onUpdate: (Int, String) -> Unit,
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    val postureDetector = remember { PostureDetector() }
    val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_ALARM, 80) }

    var lastCount by remember { mutableIntStateOf(0) }
    var isCountingStarted by remember { mutableStateOf(false) }
    val executor = remember { Executors.newSingleThreadExecutor() }

    val poseDetector = remember {
        val options = PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
            .build()
        PoseDetection.getClient(options)
    }

    val currentIsPushUp by rememberUpdatedState(isPushUp)
    val currentTargetCount by rememberUpdatedState(targetCount)
    val currentOnUpdate by rememberUpdatedState(onUpdate)
    val currentOnFinished by rememberUpdatedState(onFinished)
    val currentIsCountingStarted by rememberUpdatedState(isCountingStarted)

    LaunchedEffect(isPushUp) {
        postureDetector.resetCount()
        lastCount = 0
        isCountingStarted = false
    }

    DisposableEffect(Unit) {
        onDispose {
            executor.shutdown()
            poseDetector.close()
            toneGenerator.release()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = this.surfaceProvider
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        imageAnalysis.setAnalyzer(executor) { imageProxy ->
                            val mediaImage = imageProxy.image
                            if (mediaImage != null && currentIsCountingStarted) {
                                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                poseDetector.process(image)
                                    .addOnSuccessListener { pose ->
                                        val result = postureDetector.processPose(pose, currentIsPushUp)
                                        val newCount = result.first
                                        val feedback = result.second

                                        if (newCount > lastCount) {
                                            lastCount = newCount
                                            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                                        }

                                        currentOnUpdate(newCount, feedback)

                                        if (newCount >= currentTargetCount) {
                                            currentOnFinished()
                                        }
                                    }
                                    .addOnFailureListener {
                                        // Suppress errors
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
                                CameraSelector.DEFAULT_FRONT_CAMERA,
                                preview,
                                imageAnalysis
                            )
                        } catch (exc: Exception) {
                            try {
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageAnalysis
                                )
                            } catch (e: Exception) {
                                // Suppress
                            }
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (!isCountingStarted) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = { isCountingStarted = true },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Start Challenge", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun FinishedExerciseScreen(isSkipped: Boolean = false, onDismiss: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxHeight().fillMaxWidth()
    ) {
        if (isSkipped) {
            Text("⚠️", fontSize = 72.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Challenge Skipped!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF9800)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Alarm silenced. Streak frozen & protected.",
                fontSize = 14.sp,
                color = LightGrey,
                textAlign = TextAlign.Center
            )
        } else {
            Text("🎉", fontSize = 72.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Workout Completed!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = CyberCyan
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Great job waking up! Streak updated.",
                fontSize = 14.sp,
                color = LightGrey,
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onDismiss,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isSkipped) Color(0xFFFF9800) else CyberCyan,
                contentColor = Obsidian
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Complete Dismiss", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun CameraPermissionLayout(onRequestPermission: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxHeight().fillMaxWidth().padding(horizontal = 24.dp)
    ) {
        Text("⚠️", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Camera Permission Required",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "HitLarm requires camera access to scan barcodes or analyze body posture.",
            fontSize = 14.sp,
            color = LightGrey,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Obsidian),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Grant Camera Access", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun BarcodeTrackingView(
    targetBarcode: String,
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_ALARM, 80) }
    val executor = remember { Executors.newSingleThreadExecutor() }

    val barcodeScanner = remember {
        val options = BarcodeScannerOptions.Builder()
            .build()
        BarcodeScanning.getClient(options)
    }

    val currentTargetBarcode by rememberUpdatedState(targetBarcode)
    val currentOnFinished by rememberUpdatedState(onFinished)

    DisposableEffect(Unit) {
        onDispose {
            executor.shutdown()
            barcodeScanner.close()
            toneGenerator.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = this.surfaceProvider
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
                                    for (barcode in barcodes) {
                                        val rawValue = barcode.rawValue
                                        if (rawValue != null && rawValue.trim() == currentTargetBarcode.trim()) {
                                            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
                                            currentOnFinished()
                                            break
                                        }
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
                }, ContextCompat.getMainExecutor(ctx))
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

fun generateMathProblem(): Pair<String, Int> {
    val operation = (1..3).random()
    return when (operation) {
        1 -> {
            val a = (10..99).random()
            val b = (10..99).random()
            Pair("$a + $b", a + b)
        }
        2 -> {
            val a = (50..99).random()
            val b = (10..49).random()
            Pair("$a - $b", a - b)
        }
        else -> {
            val a = (2..12).random()
            val b = (3..15).random()
            Pair("$a * $b", a * b)
        }
    }
}
