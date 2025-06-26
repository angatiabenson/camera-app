package ke.co.banit.cameraapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import ke.co.banit.cameraapp.databinding.ActivityCameraxBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraXActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCameraxBinding
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraxBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()

        setupClickListeners()
        startCamera()
    }

    private fun setupClickListeners() {
        binding.btnCapture.setOnClickListener { takePhoto() }
        binding.btnBack.setOnClickListener { finish() }
        binding.btnSwitchCamera.setOnClickListener { switchCamera() }

        binding.toggleFlash.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnFlashOff -> setFlashMode(ImageCapture.FLASH_MODE_OFF)
                    R.id.btnFlashOn -> setFlashMode(ImageCapture.FLASH_MODE_ON)
                    R.id.btnFlashAuto -> setFlashMode(ImageCapture.FLASH_MODE_AUTO)
                }
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = binding.previewView.surfaceProvider
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            try {
                cameraProvider?.unbindAll()
                camera = cameraProvider?.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )

                // Set initial flash mode
                binding.toggleFlash.check(R.id.btnFlashOff)
                setFlashMode(ImageCapture.FLASH_MODE_OFF)

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        binding.progressBar.visibility = View.VISIBLE
        binding.btnCapture.isEnabled = false

        try {
            // Create the photo file with proper directory creation
            val photoFile = createImageFile()
            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onError(exception: ImageCaptureException) {
                        Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
                        binding.progressBar.visibility = View.GONE
                        binding.btnCapture.isEnabled = true
                        Toast.makeText(
                            baseContext,
                            "Photo capture failed: ${exception.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        val savedUri = Uri.fromFile(photoFile)
                        val msg = "Photo capture succeeded: $savedUri"
                        Toast.makeText(baseContext, "Photo saved", Toast.LENGTH_SHORT).show()
                        Log.d(TAG, msg)

                        binding.progressBar.visibility = View.GONE
                        binding.btnCapture.isEnabled = true

                        // Return result
                        val resultIntent = Intent().apply {
                            putExtra("image_uri", savedUri.toString())
                        }
                        setResult(RESULT_OK, resultIntent)
                        finish()
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up photo capture", e)
            binding.progressBar.visibility = View.GONE
            binding.btnCapture.isEnabled = true
            Toast.makeText(this, "Error setting up photo capture: ${e.message}", Toast.LENGTH_LONG)
                .show()
        }
    }

    /**
     * Creates an image file with proper directory structure
     */
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "CAMERAX_${timeStamp}.jpg"

        // Create Pictures directory in external files dir
        val picturesDir = File(getExternalFilesDir(null), "Pictures")

        // Ensure directory exists
        if (!picturesDir.exists()) {
            val created = picturesDir.mkdirs()
            Log.d(TAG, "Pictures directory created: $created at ${picturesDir.absolutePath}")

            if (!created) {
                Log.e(TAG, "Failed to create Pictures directory")
                // Fallback to external files dir root
                return File(getExternalFilesDir(null), fileName)
            }
        }

        val imageFile = File(picturesDir, fileName)
        Log.d(TAG, "Created image file path: ${imageFile.absolutePath}")

        return imageFile
    }

    private fun switchCamera() {
        lensFacing = if (CameraSelector.LENS_FACING_FRONT == lensFacing) {
            CameraSelector.LENS_FACING_BACK
        } else {
            CameraSelector.LENS_FACING_FRONT
        }
        startCamera()
    }

    private fun setFlashMode(flashMode: Int) {
        imageCapture?.flashMode = flashMode
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraXActivity"
    }
}