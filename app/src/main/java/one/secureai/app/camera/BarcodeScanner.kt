package one.secureai.app.camera

import android.graphics.Bitmap
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.tasks.await

/**
 * Pure decode function — Android equivalent of iOS's Vision-based
 * BarcodeScanner.swift, extracted the same way for testability (no Bitmap
 * capture/CameraX wiring in here, just decode-in/value-out).
 */
object BarcodeScanner {
    suspend fun decode(bitmap: Bitmap): String? {
        val image = InputImage.fromBitmap(bitmap, 0)
        val scanner = BarcodeScanning.getClient()
        return try {
            val barcodes = scanner.process(image).await()
            barcodes.firstOrNull()?.rawValue
        } catch (_: Exception) {
            null
        }
    }
}
