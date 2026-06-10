package ru.agromarket.utils

import android.content.Context
import android.net.Uri
import java.io.File

object FileUtils {
    private const val STALE_AGE_MS = 60 * 60 * 1000L // 1 hour

    /**
     * Copies the picked image into cacheDir for multipart upload.
     *
     * NOTE: no re-encoding/downscaling here — that would strip EXIF orientation and rotate
     * photos. Compression is deferred to a device-tested pass (must rotate via ExifInterface).
     * This version only fixes the cache-bloat and wrong-extension issues.
     */
    fun uriToFile(context: Context, uri: Uri): File? {
        return try {
            cleanStaleUploads(context.cacheDir)
            val extension = when (context.contentResolver.getType(uri)) {
                "image/png" -> "png"
                "image/webp" -> "webp"
                else -> "jpg"
            }
            val input = context.contentResolver.openInputStream(uri) ?: return null
            val file = File(context.cacheDir, "upload_${System.currentTimeMillis()}.$extension")
            input.use { source -> file.outputStream().use { source.copyTo(it) } }
            file
        } catch (e: Exception) {
            null
        }
    }

    /** Removes our own leftover temp uploads older than an hour so cacheDir doesn't grow unbounded. */
    private fun cleanStaleUploads(cacheDir: File) {
        val cutoff = System.currentTimeMillis() - STALE_AGE_MS
        cacheDir.listFiles { f -> f.name.startsWith("upload_") && f.lastModified() < cutoff }
            ?.forEach { it.delete() }
    }
}
