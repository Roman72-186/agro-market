package ru.agromarket.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream

object FileUtils {
    private const val STALE_AGE_MS = 60 * 60 * 1000L // 1 hour
    private const val MAX_DIMENSION = 1600
    private const val JPEG_QUALITY = 85

    /**
     * Copies the picked image into cacheDir for multipart upload, fixing EXIF rotation
     * and downscaling/recompressing to a reasonable max size as JPEG.
     */
    fun uriToFile(context: Context, uri: Uri): File? {
        return try {
            cleanStaleUploads(context.cacheDir)
            val sampled = decodeSampledBitmap(context, uri) ?: return null
            val rotated = applyExifRotation(context, uri, sampled)
            val scaled = scaleDownIfNeeded(rotated)

            val file = File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out -> scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out) }

            if (scaled !== rotated) rotated.recycle()
            if (rotated !== sampled) sampled.recycle()
            scaled.recycle()
            file
        } catch (e: Exception) {
            null
        }
    }

    /** Decodes the image downsampled to roughly [MAX_DIMENSION] to avoid loading huge bitmaps into memory. */
    private fun decodeSampledBitmap(context: Context, uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            ?: return null

        val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, MAX_DIMENSION)
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOptions) }
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var sampleSize = 1
        val largestSide = maxOf(width, height)
        while (largestSide / (sampleSize * 2) >= maxDimension) {
            sampleSize *= 2
        }
        return sampleSize
    }

    /** Rotates/flips the bitmap according to the original image's EXIF orientation tag. */
    private fun applyExifRotation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        val orientation = context.contentResolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /** Downscales further if the sampled bitmap is still larger than [MAX_DIMENSION] on its longest side. */
    private fun scaleDownIfNeeded(bitmap: Bitmap): Bitmap {
        val largestSide = maxOf(bitmap.width, bitmap.height)
        if (largestSide <= MAX_DIMENSION) return bitmap
        val scale = MAX_DIMENSION.toFloat() / largestSide
        val newWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val newHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /** Removes our own leftover temp uploads older than an hour so cacheDir doesn't grow unbounded. */
    private fun cleanStaleUploads(cacheDir: File) {
        val cutoff = System.currentTimeMillis() - STALE_AGE_MS
        cacheDir.listFiles { f -> f.name.startsWith("upload_") && f.lastModified() < cutoff }
            ?.forEach { it.delete() }
    }
}
