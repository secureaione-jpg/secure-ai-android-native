package one.secureai.app.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Custom CameraX viewfinder — Android equivalent of iOS's AVFoundation-based
 * CustomCameraView: brackets + "point your camera" hint, flash toggle, zoom
 * stepper, shutter, flip camera, and a library button to fall back to an
 * existing photo instead of capturing a new one.
 */
@Composable
fun CustomCameraScreen(
    onCaptured: (Bitmap) -> Unit,
    onPickFromLibrary: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashOn by remember { mutableStateOf(false) }
    var zoomRatio by remember { mutableStateOf(1f) }
    var camera by remember { mutableStateOf<Camera?>(null) }

    val previewView = remember { PreviewView(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }

    LaunchedEffect(lensFacing) {
        val cameraProvider = context.cameraProvider()
        val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
        val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        cameraProvider.unbindAll()
        camera = cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
        zoomRatio = 1f
    }

    LaunchedEffect(flashOn) {
        imageCapture.flashMode = if (flashOn) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
    }

    LaunchedEffect(zoomRatio) {
        val zoomState = camera?.cameraInfo?.zoomState?.value ?: return@LaunchedEffect
        camera?.cameraControl?.setZoomRatio(zoomRatio.coerceIn(zoomState.minZoomRatio, zoomState.maxZoomRatio))
    }

    fun capture() {
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = image.toRotatedBitmap()
                    image.close()
                    onCaptured(bitmap)
                }
                override fun onError(exception: ImageCaptureException) {}
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        // Viewfinder brackets + hint text
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(90.dp))
            Text(
                "Point your camera at something to ask",
                color = Color.White,
                fontSize = 15.sp,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .border(2.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
            )
            Spacer(Modifier.weight(1f))
        }

        // Top bar: close + flash
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
            IconButton(onClick = { flashOn = !flashOn }) {
                Icon(
                    if (flashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = "Toggle flash",
                    tint = Color.White
                )
            }
        }

        // Bottom controls: zoom stepper, shutter, flip, library
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 28.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                listOf(0.5f, 1f, 2f).forEach { step ->
                    val selected = zoomRatio == step
                    Surface(
                        onClick = { zoomRatio = step },
                        shape = CircleShape,
                        color = if (selected) Color.White else Color.White.copy(alpha = 0.25f),
                        modifier = Modifier.padding(4.dp).size(if (selected) 40.dp else 34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(
                                if (step == 1f) "1x" else "${step}x",
                                fontSize = 12.sp,
                                color = if (selected) Color.Black else Color.White
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPickFromLibrary) {
                    Icon(Icons.Default.Photo, contentDescription = "Choose from library", tint = Color.White, modifier = Modifier.size(28.dp))
                }

                Surface(
                    onClick = { capture() },
                    shape = CircleShape,
                    color = Color.White,
                    modifier = Modifier.size(72.dp).border(4.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                ) {}

                IconButton(onClick = {
                    lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK)
                        CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
                }) {
                    Icon(Icons.Default.Cameraswitch, contentDescription = "Flip camera", tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}

private suspend fun Context.cameraProvider(): ProcessCameraProvider = suspendCancellableCoroutine { cont ->
    val future = ProcessCameraProvider.getInstance(this)
    future.addListener({ cont.resumeWith(Result.success(future.get())) }, ContextCompat.getMainExecutor(this))
}

private fun ImageProxy.toRotatedBitmap(): Bitmap {
    val buffer = planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    val rotation = imageInfo.rotationDegrees
    if (rotation == 0) return bitmap
    val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}
