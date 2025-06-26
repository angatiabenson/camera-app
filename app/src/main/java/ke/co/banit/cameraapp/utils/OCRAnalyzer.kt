package ke.co.banit.cameraapp.utils

import com.google.mlkit.vision.text.Text
import kotlin.math.min

/**
 * ------------------------------------------------------------------------
 * Project: CameraApp
 * File Created by: Angatia Benson on Thu, Jun 26, 2025
 * ------------------------------------------------------------------------
 * © 2025 CoreTec Solution Africa. All rights reserved.
 * ------------------------------------------------------------------------
 * This file is part of the CoreTec Solution Africa project and is intended
 * for internal use within the company. Unauthorized copying, distribution,
 * or use of this file, via any medium, is strictly prohibited.
 * ------------------------------------------------------------------------
 **/
class OCRAnalyzer {

    data class OCRResult(
     val source: String,
     val textConfidence: Float,
     val textBlockCount: Int,
     val totalCharacters: Int,
     val readabilityScore: Float,
     val qualityMetrics: ImageQualityAnalyzer.QualityMetrics,
     val extractedText: String,
     val processingTimeMs: Long,
    )

    fun analyzeOCRResult(
     visionText: Text,
     qualityMetrics: ImageQualityAnalyzer.QualityMetrics,
     source: String,
    ): OCRResult {
        val startTime = System.currentTimeMillis()

        val textBlocks = visionText.textBlocks
        val totalCharacters = visionText.text.length
        val extractedText = visionText.text

        // Calculate average confidence
        var totalConfidence = 0f
        var elementCount = 0

        for (block in textBlocks) {
            for (line in block.lines) {
                for (element in line.elements) {
                    element.confidence?.let { confidence ->
                        totalConfidence += confidence
                        elementCount++
                    }
                }
            }
        }

        val averageConfidence = if (elementCount > 0) totalConfidence / elementCount else 0f

        // Calculate readability score based on various factors
        val readabilityScore = calculateReadabilityScore(
            visionText,
            qualityMetrics,
            averageConfidence
        )

        val processingTime = System.currentTimeMillis() - startTime

        return OCRResult(
            source = source,
            textConfidence = averageConfidence,
            textBlockCount = textBlocks.size,
            totalCharacters = totalCharacters,
            readabilityScore = readabilityScore,
            qualityMetrics = qualityMetrics,
            extractedText = extractedText,
            processingTimeMs = processingTime
        )
    }

    private fun calculateReadabilityScore(
     visionText: Text,
     qualityMetrics: ImageQualityAnalyzer.QualityMetrics,
     confidence: Float,
    ): Float {
        // Weighted factors for readability
        val confidenceWeight = 0.4f
        val sharpnessWeight = 0.3f
        val contrastWeight = 0.2f
        val textDensityWeight = 0.1f

        // Calculate text density (characters per unit area)
        val textDensity = if (qualityMetrics.resolution > 0) {
            min(visionText.text.length.toFloat() / qualityMetrics.resolution * 1000000, 1f)
        } else 0f

        // Normalize quality metrics to 0-1 range
        val normalizedSharpness = min(qualityMetrics.sharpness, 1f)
        val normalizedContrast = min(qualityMetrics.contrast, 1f)

        return (confidence * confidenceWeight) +
                (normalizedSharpness * sharpnessWeight) +
                (normalizedContrast * contrastWeight) +
                (textDensity * textDensityWeight)
    }
}