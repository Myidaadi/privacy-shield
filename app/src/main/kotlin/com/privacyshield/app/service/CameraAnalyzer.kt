package com.privacyshield.app.service

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

/**
 * CameraX [ImageAnalysis.Analyzer] that detects faces per frame.
 *
 * - Uses ML Kit in FAST mode (optimised for real-time)
 * - Throttles to max ~8 fps to save battery
 * - Drops frames automatically when still processing
 * - Reports face count via [onFacesDetected] callback
 */
class CameraAnalyzer(
    private val onFacesDetected: (Int) -> Unit,
) : ImageAnalysis.Analyzer {

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .enableTracking()
            .setMinFaceSize(0.15f)   // Only detect faces ≥ 15% of frame width
            .build()
    )

    private var lastProcessedMs = 0L
    private val throttleMs = 125L    // ~8 fps effective detection rate

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val now = System.currentTimeMillis()
        if (now - lastProcessedMs < throttleMs) {
            imageProxy.close()
            return
        }
        lastProcessedMs = now

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val inputImage = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        detector.process(inputImage)
            .addOnSuccessListener { faces ->
                onFacesDetected(faces.size)
            }
            .addOnFailureListener {
                // Silently ignore failures (lighting, blur, etc.)
            }
            .addOnCompleteListener {
                imageProxy.close()   // MUST always close
            }
    }

    fun close() {
        detector.close()
    }
}
