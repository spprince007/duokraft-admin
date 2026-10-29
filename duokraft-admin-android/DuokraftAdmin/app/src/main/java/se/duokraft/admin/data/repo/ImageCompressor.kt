package se.duokraft.admin.data.repo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.OpenableColumns
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max

/** A file ready to upload. */
class UploadFile(val bytes: ByteArray, val fileName: String, val mimeType: String)

/**
 * Shrinks photos before upload so huge camera images don't slow the website down:
 * longest side at most [MAX_EDGE] px, EXIF rotation applied, JPEG quality [QUALITY].
 * PNGs with transparency stay PNG. GIF, SVG and other formats are uploaded unchanged.
 */
class ImageCompressor(private val context: Context) {

    suspend fun prepare(uri: Uri): UploadFile = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri) ?: "application/octet-stream"
        val name = displayName(uri) ?: "upload-${System.currentTimeMillis()}"
        val original = resolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Could not read the selected file.")

        if (mime !in RESIZABLE) return@withContext UploadFile(original, name, mime)

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(original, 0, original.size, bounds)
        val longest = max(bounds.outWidth, bounds.outHeight)
        if (longest <= 0) return@withContext UploadFile(original, name, mime)

        var sample = 1
        while (longest / (sample * 2) >= MAX_EDGE) sample *= 2
        val decoded = BitmapFactory.decodeByteArray(original, 0, original.size, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: return@withContext UploadFile(original, name, mime)

        val scaled = scaleDown(rotate(decoded, original))
        val keepPng = mime == "image/png" && scaled.hasAlpha()
        val out = ByteArrayOutputStream()
        scaled.compress(if (keepPng) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG, QUALITY, out)
        val bytes = out.toByteArray()

        // Only use the compressed version if it is actually smaller.
        if (bytes.size >= original.size && longest <= MAX_EDGE) return@withContext UploadFile(original, name, mime)
        val outMime = if (keepPng) "image/png" else "image/jpeg"
        val outName = name.substringBeforeLast('.') + if (keepPng) ".png" else ".jpg"
        UploadFile(bytes, outName, outMime)
    }

    private fun scaleDown(bitmap: Bitmap): Bitmap {
        val longest = max(bitmap.width, bitmap.height)
        if (longest <= MAX_EDGE) return bitmap
        val ratio = MAX_EDGE.toFloat() / longest
        return Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
    }

    private fun rotate(bitmap: Bitmap, bytes: ByteArray): Bitmap {
        val degrees = runCatching {
            when (ExifInterface(bytes.inputStream()).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        }.getOrDefault(0f)
        if (degrees == 0f) return bitmap
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(degrees) }, true)
    }

    private fun displayName(uri: Uri): String? =
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }?.replace(Regex("[^A-Za-z0-9._-]"), "-")

    private companion object {
        const val MAX_EDGE = 2048
        const val QUALITY = 85
        val RESIZABLE = setOf("image/jpeg", "image/png", "image/webp", "image/heic", "image/heif")
    }
}
