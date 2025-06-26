package ke.co.banit.cameraapp

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.exifinterface.media.ExifInterface
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import ke.co.banit.cameraapp.databinding.ActivityComparisonBinding
import ke.co.banit.cameraapp.utils.ImageQualityAnalyzer
import ke.co.banit.cameraapp.utils.OCRAnalyzer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DecimalFormat

class ComparisonActivity : AppCompatActivity() {
    private lateinit var binding: ActivityComparisonBinding
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val imageQualityAnalyzer = ImageQualityAnalyzer()
    private val ocrAnalyzer = OCRAnalyzer()

    private var cameraXBitmap: Bitmap? = null
    private var intentBitmap: Bitmap? = null
    private var cameraXOcrResult: OCRAnalyzer.OCRResult? = null
    private var intentOcrResult: OCRAnalyzer.OCRResult? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityComparisonBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val cameraXUri = intent.getStringExtra("camerax_uri")?.let { Uri.parse(it) }
        val intentUri = intent.getStringExtra("intent_uri")?.let { Uri.parse(it) }

        setupClickListeners()
        loadAndAnalyzeImages(cameraXUri, intentUri)
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun loadAndAnalyzeImages(cameraXUri: Uri?, intentUri: Uri?) {
        binding.tvComparison.text = "Analyzing images for OCR quality..."

        val analysisJobs = mutableListOf<Job>()

        cameraXUri?.let { uri ->
            Glide.with(this)
                .asBitmap()
                .load(uri)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?,
                    ) {
                        binding.ivCameraX.setImageBitmap(resource)
                        cameraXBitmap = resource

                        val metadata = getImageMetadata(uri)
                        binding.tvCameraXInfo.text = metadata

                        // Start OCR analysis
                        val job = CoroutineScope(Dispatchers.Main).launch {
                            cameraXOcrResult = analyzeImageForOCR(resource, "CameraX")
                            checkAnalysisComplete()
                        }
                        analysisJobs.add(job)
                    }

                    override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {}
                })
        }

        intentUri?.let { uri ->
            Glide.with(this)
                .asBitmap()
                .load(uri)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?,
                    ) {
                        binding.ivCameraIntent.setImageBitmap(resource)
                        intentBitmap = resource

                        val metadata = getImageMetadata(uri)
                        binding.tvCameraIntentInfo.text = metadata

                        // Start OCR analysis
                        val job = CoroutineScope(Dispatchers.Main).launch {
                            intentOcrResult = analyzeImageForOCR(resource, "Camera Intent")
                            checkAnalysisComplete()
                        }
                        analysisJobs.add(job)
                    }

                    override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {}
                })
        }
    }

    private suspend fun analyzeImageForOCR(bitmap: Bitmap, source: String): OCRAnalyzer.OCRResult {
        return withContext(Dispatchers.IO) {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val qualityMetrics = imageQualityAnalyzer.analyzeImageQuality(bitmap)

            // Perform OCR analysis
            val ocrResult = suspendCancellableCoroutine<OCRAnalyzer.OCRResult> { continuation ->
                textRecognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        val result = ocrAnalyzer.analyzeOCRResult(
                            visionText,
                            qualityMetrics,
                            source
                        )
                        continuation.resumeWith(Result.success(result))
                    }
                    .addOnFailureListener { exception ->
                        Log.e(TAG, "OCR failed for $source", exception)
                        val failureResult = OCRAnalyzer.OCRResult(
                            source = source,
                            textConfidence = 0f,
                            textBlockCount = 0,
                            totalCharacters = 0,
                            readabilityScore = 0f,
                            qualityMetrics = qualityMetrics,
                            extractedText = "",
                            processingTimeMs = 0L
                        )
                        continuation.resumeWith(Result.success(failureResult))
                    }
            }

            ocrResult
        }
    }

    private fun checkAnalysisComplete() {
        if (cameraXOcrResult != null && intentOcrResult != null) {
            generateOCRComparison()
        }
    }

    private fun generateOCRComparison() {
        val cameraXResult = cameraXOcrResult ?: return
        val intentResult = intentOcrResult ?: return

        val comparison = buildString {
            appendLine("🔍 OCR QUALITY ANALYSIS & RANKING")
            appendLine("=" * 50)
            appendLine()

            // Determine winner
            val cameraXScore = calculateOCRScore(cameraXResult)
            val intentScore = calculateOCRScore(intentResult)

            val winner = if (cameraXScore > intentScore) "CameraX" else "Camera Intent"
            val scoreDiff = Math.abs(cameraXScore - intentScore)

            appendLine("🏆 OVERALL WINNER FOR OCR: $winner")
            appendLine("Score Difference: ${DecimalFormat("#.##").format(scoreDiff)}")
            appendLine()

            // Detailed comparison
            appendLine("📊 DETAILED COMPARISON:")
            appendLine()

            appendLine("🔤 Text Detection Results:")
            appendLine("• CameraX: ${cameraXResult.textBlockCount} text blocks, ${cameraXResult.totalCharacters} chars")
            appendLine("• Camera Intent: ${intentResult.textBlockCount} text blocks, ${intentResult.totalCharacters} chars")
            appendLine("Winner: ${if (cameraXResult.totalCharacters > intentResult.totalCharacters) "CameraX" else "Camera Intent"} (More text detected)")
            appendLine()

            appendLine("📈 Confidence Scores:")
            appendLine("• CameraX: ${DecimalFormat("#.##").format(cameraXResult.textConfidence * 100)}%")
            appendLine("• Camera Intent: ${DecimalFormat("#.##").format(intentResult.textConfidence * 100)}%")
            appendLine("Winner: ${if (cameraXResult.textConfidence > intentResult.textConfidence) "CameraX" else "Camera Intent"}")
            appendLine()

            appendLine("🎯 Image Quality Metrics:")
            appendLine("• CameraX Sharpness: ${DecimalFormat("#.##").format(cameraXResult.qualityMetrics.sharpness)}")
            appendLine("• Camera Intent Sharpness: ${DecimalFormat("#.##").format(intentResult.qualityMetrics.sharpness)}")
            appendLine("Winner: ${if (cameraXResult.qualityMetrics.sharpness > intentResult.qualityMetrics.sharpness) "CameraX" else "Camera Intent"}")
            appendLine()

            appendLine("• CameraX Contrast: ${DecimalFormat("#.##").format(cameraXResult.qualityMetrics.contrast)}")
            appendLine("• Camera Intent Contrast: ${DecimalFormat("#.##").format(intentResult.qualityMetrics.contrast)}")
            appendLine("Winner: ${if (cameraXResult.qualityMetrics.contrast > intentResult.qualityMetrics.contrast) "CameraX" else "Camera Intent"}")
            appendLine()

            appendLine("📋 Readability Scores:")
            appendLine("• CameraX: ${DecimalFormat("#.##").format(cameraXResult.readabilityScore * 100)}%")
            appendLine("• Camera Intent: ${DecimalFormat("#.##").format(intentResult.readabilityScore * 100)}%")
            appendLine("Winner: ${if (cameraXResult.readabilityScore > intentResult.readabilityScore) "CameraX" else "Camera Intent"}")
            appendLine()

            appendLine("🏁 FINAL RANKING:")
            appendLine("1st Place: $winner (Score: ${DecimalFormat("#.##").format(if (winner == "CameraX") cameraXScore else intentScore)})")
            appendLine(
                "2nd Place: ${if (winner == "CameraX") "Camera Intent" else "CameraX"} (Score: ${
                    DecimalFormat(
                        "#.##"
                    ).format(if (winner == "CameraX") intentScore else cameraXScore)
                })"
            )
            appendLine()

            appendLine("💡 RECOMMENDATIONS FOR OCR:")
            when (winner) {
                "CameraX" -> {
                    appendLine("• Use CameraX for better OCR results")
                    appendLine("• CameraX provides more consistent image quality")
                    appendLine("• Better manual control leads to optimal OCR conditions")
                }

                else -> {
                    appendLine("• Camera Intent produced better OCR results")
                    appendLine("• Device's camera app may have better auto-optimization")
                    appendLine("• Consider using Camera Intent for text capture scenarios")
                }
            }

            if (cameraXResult.extractedText.isNotEmpty()) {
                appendLine()
                appendLine("📝 Sample Extracted Text (CameraX):")
                appendLine("\"${cameraXResult.extractedText.take(100)}${if (cameraXResult.extractedText.length > 100) "..." else ""}\"")
            }

            if (intentResult.extractedText.isNotEmpty()) {
                appendLine()
                appendLine("📝 Sample Extracted Text (Camera Intent):")
                appendLine("\"${intentResult.extractedText.take(100)}${if (intentResult.extractedText.length > 100) "..." else ""}\"")
            }
        }

        binding.tvComparison.text = comparison
    }

    private fun calculateOCRScore(result: OCRAnalyzer.OCRResult): Float {
        // Weighted scoring system for OCR quality
        val confidenceWeight = 0.3f
        val sharpnessWeight = 0.25f
        val contrastWeight = 0.2f
        val readabilityWeight = 0.15f
        val textDetectionWeight = 0.1f

        val normalizedTextDetection =
            Math.min(result.totalCharacters / 1000f, 1f) // Normalize to 0-1

        return (result.textConfidence * confidenceWeight) +
                (result.qualityMetrics.sharpness * sharpnessWeight) +
                (result.qualityMetrics.contrast * contrastWeight) +
                (result.readabilityScore * readabilityWeight) +
                (normalizedTextDetection * textDetectionWeight)
    }

    private fun getImageMetadata(uri: Uri): String {
        return try {
            val file = File(uri.path ?: return "Unable to read file")
            val exif = ExifInterface(file.absolutePath)

            val width = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
            val height = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)
            val make = exif.getAttribute(ExifInterface.TAG_MAKE) ?: "Unknown"
            val model = exif.getAttribute(ExifInterface.TAG_MODEL) ?: "Unknown"
            val datetime = exif.getAttribute(ExifInterface.TAG_DATETIME) ?: "Unknown"
            val flash = exif.getAttribute(ExifInterface.TAG_FLASH) ?: "Unknown"
            val focalLength = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH) ?: "Unknown"
            val aperture = exif.getAttribute(ExifInterface.TAG_F_NUMBER) ?: "Unknown"
            val iso = exif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS) ?: "Unknown"
            val exposureTime = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME) ?: "Unknown"

            val fileSizeMB = DecimalFormat("#.##").format(file.length() / (1024.0 * 1024.0))

            """
            Resolution: ${width}x${height}
            File Size: ${fileSizeMB} MB
            Device: $make $model
            Date/Time: $datetime
            Flash: $flash
            Focal Length: $focalLength
            Aperture: f/$aperture
            ISO: $iso
            Exposure: ${exposureTime}s
            """.trimIndent()

        } catch (e: Exception) {
            "Error reading metadata: ${e.message}"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        textRecognizer.close()
    }

    companion object {
        private const val TAG = "ComparisonActivity"
    }
}

private operator fun String.times(i: Int): String {
    return this.repeat(i)
}