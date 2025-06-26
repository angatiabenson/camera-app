package ke.co.banit.cameraapp.utils

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.max
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

class ImageQualityAnalyzer {

    data class QualityMetrics(
     val sharpness: Float,
     val contrast: Float,
     val brightness: Float,
     val noiseLevel: Float,
     val resolution: Int,
    )

    fun analyzeImageQuality(bitmap: Bitmap): QualityMetrics {
        val sharpness = calculateSharpness(bitmap)
        val contrast = calculateContrast(bitmap)
        val brightness = calculateBrightness(bitmap)
        val noiseLevel = calculateNoiseLevel(bitmap)
        val resolution = bitmap.width * bitmap.height

        return QualityMetrics(
            sharpness = sharpness,
            contrast = contrast,
            brightness = brightness,
            noiseLevel = noiseLevel,
            resolution = resolution
        )
    }

    private fun calculateSharpness(bitmap: Bitmap): Float {
        // Laplacian edge detection for sharpness measurement
        val width = bitmap.width
        val height = bitmap.height
        val grayscale = convertToGrayscale(bitmap)

        var variance = 0.0
        val laplacianKernel = arrayOf(
            intArrayOf(0, -1, 0),
            intArrayOf(-1, 4, -1),
            intArrayOf(0, -1, 0)
        )

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                var sum = 0
                for (ky in -1..1) {
                    for (kx in -1..1) {
                        sum += grayscale[y + ky][x + kx] * laplacianKernel[ky + 1][kx + 1]
                    }
                }
                variance += sum * sum
            }
        }

        variance /= ((width - 2) * (height - 2))
        return (variance / 10000.0).toFloat().coerceIn(0f, 1f) // Normalize
    }

    private fun calculateContrast(bitmap: Bitmap): Float {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        var minLuma = 255f
        var maxLuma = 0f

        for (pixel in pixels) {
            val luma =
                0.299f * Color.red(pixel) + 0.587f * Color.green(pixel) + 0.114f * Color.blue(pixel)
            minLuma = min(minLuma, luma)
            maxLuma = max(maxLuma, luma)
        }

        return if (maxLuma > 0) (maxLuma - minLuma) / maxLuma else 0f
    }

    private fun calculateBrightness(bitmap: Bitmap): Float {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        var totalLuma = 0f
        for (pixel in pixels) {
            totalLuma += 0.299f * Color.red(pixel) + 0.587f * Color.green(pixel) + 0.114f * Color.blue(
                pixel
            )
        }

        return (totalLuma / pixels.size) / 255f
    }

    private fun calculateNoiseLevel(bitmap: Bitmap): Float {
        // Simple noise estimation using local variance
        val grayscale = convertToGrayscale(bitmap)
        val width = bitmap.width
        val height = bitmap.height

        var totalVariance = 0.0
        val windowSize = 5

        for (y in windowSize until height - windowSize) {
            for (x in windowSize until width - windowSize) {
                var sum = 0.0
                var sumSquared = 0.0
                val count = windowSize * windowSize

                for (wy in -windowSize / 2..windowSize / 2) {
                    for (wx in -windowSize / 2..windowSize / 2) {
                        val value = grayscale[y + wy][x + wx]
                        sum += value
                        sumSquared += value * value
                    }
                }

                val mean = sum / count
                val variance = (sumSquared / count) - (mean * mean)
                totalVariance += variance
            }
        }

        val avgVariance = totalVariance / ((width - 2 * windowSize) * (height - 2 * windowSize))
        return (avgVariance / 10000.0).toFloat().coerceIn(0f, 1f)
    }

    private fun convertToGrayscale(bitmap: Bitmap): Array<IntArray> {
        val width = bitmap.width
        val height = bitmap.height
        val grayscale = Array(height) { IntArray(width) }

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = bitmap.getPixel(x, y)
                val gray =
                    (0.299 * Color.red(pixel) + 0.587 * Color.green(pixel) + 0.114 * Color.blue(
                        pixel
                    )).toInt()
                grayscale[y][x] = gray
            }
        }

        return grayscale
    }
}