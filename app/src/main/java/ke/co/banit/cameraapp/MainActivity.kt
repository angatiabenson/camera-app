package ke.co.banit.cameraapp

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import ke.co.banit.cameraapp.databinding.ActivityMainBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var cameraXImageUri: Uri? = null
    private var cameraIntentImageUri: Uri? = null
    private var currentPhotoPath: String? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.CAMERA] == true) {
            Log.d(TAG, "Camera permission granted")
        } else {
            Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    private val cameraXLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringExtra("image_uri")?.let { uriString ->
                cameraXImageUri = Uri.parse(uriString)
                binding.tvCameraXStatus.text = "Captured ✓"
                updateCompareButton()
                Log.d(TAG, "CameraX image captured: $uriString")
            }
        }
    }

    private val cameraIntentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            currentPhotoPath?.let { path ->
                cameraIntentImageUri = Uri.fromFile(File(path))
                binding.tvIntentStatus.text = "Captured ✓"
                updateCompareButton()
                Log.d(TAG, "Camera Intent image captured: $path")
            }
        } else {
            Log.w(TAG, "Camera Intent cancelled or failed. Result code: ${result.resultCode}")
            Toast.makeText(this, "Camera operation cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        checkPermissions()

        // Ensure directories exist on app start
        createAppDirectories()

        // Debug: Check camera availability
        checkCameraAvailability()
    }

    private fun setupClickListeners() {
        binding.btnCameraX.setOnClickListener {
            if (allPermissionsGranted()) {
                openCameraX()
            } else {
                requestPermissions()
            }
        }

        binding.btnCameraIntent.setOnClickListener {
            if (allPermissionsGranted()) {
                openCameraIntent()
            } else {
                requestPermissions()
            }
        }

        binding.btnCompare.setOnClickListener {
            openComparison()
        }
    }

    /**
     * Debug method to check camera availability
     */
    private fun checkCameraAvailability() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val canResolve = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ with package visibility
            cameraIntent.resolveActivity(packageManager) != null
        } else {
            // Pre-Android 11
            packageManager.queryIntentActivities(cameraIntent, PackageManager.MATCH_DEFAULT_ONLY)
                .isNotEmpty()
        }

        Log.d(TAG, "Camera intent can be resolved: $canResolve")
        Log.d(TAG, "Android version: ${Build.VERSION.SDK_INT}")

        if (!canResolve) {
            Log.w(
                TAG,
                "No camera app found. Check <queries> in AndroidManifest.xml for Android 11+"
            )
        }
    }

    /**
     * Create necessary app directories
     */
    private fun createAppDirectories() {
        try {
            val picturesDir = File(getExternalFilesDir(null), "Pictures")
            if (!picturesDir.exists()) {
                val created = picturesDir.mkdirs()
                Log.d(TAG, "App Pictures directory created: $created")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating app directories", e)
        }
    }

    private fun openCameraX() {
        val intent = Intent(this, CameraXActivity::class.java)
        cameraXLauncher.launch(intent)
    }

    private fun openCameraIntent() {
        try {
            val photoFile = createImageFileForIntent()
            currentPhotoPath = photoFile.absolutePath

            val photoURI = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                photoFile
            )

            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                // Grant URI permissions for the camera app
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // Enhanced resolution check with multiple fallbacks
            when {
                canResolveCameraIntent(cameraIntent) -> {
                    Log.d(TAG, "Launching camera intent with URI: $photoURI")
                    cameraIntentLauncher.launch(cameraIntent)
                }

                else -> {
                    Log.e(TAG, "No camera app available")
                    showCameraNotAvailableDialog()
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error opening camera intent", e)
            Toast.makeText(this, "Error opening camera: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Enhanced camera intent resolution check
     */
    private fun canResolveCameraIntent(intent: Intent): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+ with package visibility restrictions
                intent.resolveActivity(packageManager) != null
            } else {
                // Pre-Android 11
                packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
                    .isNotEmpty()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking camera intent resolution", e)
            false
        }
    }

    /**
     * Show dialog when camera is not available
     */
    private fun showCameraNotAvailableDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Camera Not Available")
            .setMessage("No camera app found on this device. Please install a camera app or use CameraX instead.")
            .setPositiveButton("Use CameraX") { _, _ ->
                openCameraX()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Creates an image file for Camera Intent with proper directory structure
     */
    private fun createImageFileForIntent(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "INTENT_${timeStamp}.jpg"

        // Create Pictures directory in external files dir
        val picturesDir = File(getExternalFilesDir(null), "Pictures")

        // Ensure directory exists
        if (!picturesDir.exists()) {
            val created = picturesDir.mkdirs()
            Log.d(TAG, "Pictures directory created for intent: $created")

            if (!created) {
                Log.e(TAG, "Failed to create Pictures directory for intent")
                throw RuntimeException("Cannot create Pictures directory")
            }
        }

        val imageFile = File(picturesDir, fileName)
        Log.d(TAG, "Created intent image file path: ${imageFile.absolutePath}")

        return imageFile
    }

    private fun openComparison() {
        val intent = Intent(this, ComparisonActivity::class.java).apply {
            putExtra("camerax_uri", cameraXImageUri.toString())
            putExtra("intent_uri", cameraIntentImageUri.toString())
        }
        startActivity(intent)
    }

    private fun updateCompareButton() {
        binding.btnCompare.isEnabled = cameraXImageUri != null && cameraIntentImageUri != null
    }

    private fun checkPermissions() {
        if (!allPermissionsGranted()) {
            requestPermissions()
        }
    }

    private fun requestPermissions() {
        requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val TAG = "MainActivity"
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}