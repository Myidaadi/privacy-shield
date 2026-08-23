package com.privacyshield.app.viewmodel

import android.app.Application
import android.content.Intent
import android.os.Build
import android.view.WindowManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.privacyshield.app.model.*
import com.privacyshield.app.service.CameraAnalyzer
import com.privacyshield.app.service.PrivacyForegroundService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@ExperimentalGetImage
class PrivacyViewModel(application: Application) : AndroidViewModel(application) {

    // ── Repository ─────────────────────────────────────────────────────────
    private val repo = AppSettingsRepository(application)

    // ── State ──────────────────────────────────────────────────────────────
    private val _privacyState = MutableStateFlow(PrivacyState.DISABLED)
    val privacyState: StateFlow<PrivacyState> = _privacyState.asStateFlow()

    val settings: StateFlow<AppSettings> = repo.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    // ── Camera & Detection ─────────────────────────────────────────────────
    private var cameraProvider: ProcessCameraProvider? = null
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var analyzer: CameraAnalyzer? = null

    // ── Debounce Counters ──────────────────────────────────────────────────
    private var peekerFrames = 0
    private var awayFrames   = 0
    private var lockJob: Job? = null

    // ══════════════════════════════════════════════════════════════════════
    // Public API
    // ══════════════════════════════════════════════════════════════════════

    /** Enable protection — starts camera + foreground service. */
    fun enable(lifecycleOwner: LifecycleOwner) {
        viewModelScope.launch {
            repo.setEnabled(true)
            startCamera(lifecycleOwner)
            _privacyState.value = PrivacyState.NORMAL
            startForegroundService()
        }
    }

    /** Disable protection — tears down camera + stops service. */
    fun disable() {
        viewModelScope.launch {
            repo.setEnabled(false)
            tearDownCamera()
            lockJob?.cancel()
            peekerFrames = 0
            awayFrames   = 0
            _privacyState.value = PrivacyState.DISABLED
            stopForegroundService()
            setFlagSecure(false)
        }
    }

    /** Dismiss the privacy overlay (called after biometric auth or peeker leaves). */
    fun dismissOverlay() {
        peekerFrames = 0
        awayFrames   = 0
        setFlagSecure(false)
        _privacyState.value = PrivacyState.NORMAL
    }

    // ── Settings Setters ───────────────────────────────────────────────────
    fun setOverlayStyle(v: OverlayStyle)        = viewModelScope.launch { repo.setOverlayStyle(v) }
    fun setSensitivity(v: DetectionSensitivity) = viewModelScope.launch { repo.setSensitivity(v) }
    fun setAutoLockSeconds(v: Int)              = viewModelScope.launch { repo.setAutoLockSeconds(v) }
    fun setRequireBiometric(v: Boolean)         = viewModelScope.launch { repo.setRequireBiometric(v) }

    // ══════════════════════════════════════════════════════════════════════
    // Camera
    // ══════════════════════════════════════════════════════════════════════

    private fun startCamera(lifecycleOwner: LifecycleOwner) {
        val ctx = getApplication<Application>()
        val future = ProcessCameraProvider.getInstance(ctx)

        future.addListener({
            cameraProvider = future.get()

            analyzer = CameraAnalyzer { faceCount -> onFacesDetected(faceCount) }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { it.setAnalyzer(cameraExecutor, analyzer!!) }

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build()

            try {
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(lifecycleOwner, cameraSelector, imageAnalysis)
            } catch (e: Exception) {
                // Camera not available
            }
        }, ContextCompat.getMainExecutor(ctx))
    }

    private fun tearDownCamera() {
        cameraProvider?.unbindAll()
        analyzer?.close()
        analyzer = null
    }

    // ══════════════════════════════════════════════════════════════════════
    // Face Count → State Machine
    // ══════════════════════════════════════════════════════════════════════

    private fun onFacesDetected(count: Int) {
        val state = _privacyState.value
        if (state == PrivacyState.DISABLED) return
        if (state == PrivacyState.LOCKED)   return   // stay locked until user dismisses

        val threshold = settings.value.sensitivity.frameThreshold

        when {
            count >= 2 -> {
                // Potential peeker — require consecutive frames before alerting
                awayFrames = 0
                peekerFrames++
                if (peekerFrames >= threshold) triggerPeekerAlert()
            }
            count == 1 -> {
                // Owner only — all clear
                peekerFrames = 0
                awayFrames   = 0
                lockJob?.cancel()
                if (state != PrivacyState.NORMAL) {
                    setFlagSecure(false)
                    _privacyState.value = PrivacyState.NORMAL
                }
            }
            else -> {
                // No face — owner may have walked away
                peekerFrames = 0
                awayFrames++
                if (awayFrames >= 5 && state == PrivacyState.NORMAL) {
                    _privacyState.value = PrivacyState.OWNER_AWAY
                    startLockTimer()
                }
            }
        }
    }

    private fun triggerPeekerAlert() {
        if (_privacyState.value == PrivacyState.PEEKER_ALERT) return
        lockJob?.cancel()
        setFlagSecure(true)
        _privacyState.value = PrivacyState.PEEKER_ALERT
    }

    private fun startLockTimer() {
        lockJob?.cancel()
        val seconds = settings.value.autoLockSeconds
        if (seconds <= 0) return

        lockJob = viewModelScope.launch {
            delay(seconds * 1000L)
            if (_privacyState.value == PrivacyState.OWNER_AWAY) {
                setFlagSecure(true)
                _privacyState.value = PrivacyState.LOCKED
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // Native Window Flags + Foreground Service
    // ══════════════════════════════════════════════════════════════════════

    // FLAG_SECURE is set on the window directly in MainActivity via the
    // secureWindow StateFlow below — MainActivity observes and applies it.
    private val _flagSecure = MutableStateFlow(false)
    val flagSecure: StateFlow<Boolean> = _flagSecure.asStateFlow()

    private fun setFlagSecure(enabled: Boolean) {
        _flagSecure.value = enabled
    }

    private fun startForegroundService() {
        val ctx = getApplication<Application>()
        val intent = PrivacyForegroundService.startIntent(ctx)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ctx.startForegroundService(intent)
        } else {
            ctx.startService(intent)
        }
    }

    private fun stopForegroundService() {
        val ctx = getApplication<Application>()
        ctx.stopService(PrivacyForegroundService.stopIntent(ctx))
    }

    override fun onCleared() {
        super.onCleared()
        cameraExecutor.shutdown()
        analyzer?.close()
    }
}
