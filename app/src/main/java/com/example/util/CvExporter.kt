package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import android.provider.MediaStore
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Writes the rewritten CV somewhere the user can actually find it.
 *
 * On Android 10 and later this goes straight into the shared Downloads folder through
 * MediaStore, which needs no permission. Below that, scoped storage does not exist and
 * writing to Downloads would mean asking for WRITE_EXTERNAL_STORAGE, so the file is
 * written inside the app instead and handed to the system share sheet, letting the user
 * put it wherever they want. Either way nothing is requested at runtime.
 */
object CvExporter {

    private const val DOCX_MIME =
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"

    sealed interface Destination {
        /** Landed in the device's Downloads folder. */
        data class Downloads(val fileName: String) : Destination

        /** Held inside the app; the caller should offer [shareIntent]. */
        data class NeedsSharing(val file: File, val fileName: String) : Destination
    }

    fun fileNameFor(candidateName: String): String {
        val safeName = candidateName
            .trim()
            .replace(Regex("[^A-Za-z0-9 _-]"), "")
            .replace(Regex("\\s+"), "_")
            .take(40)
            .ifBlank { "CV" }
        val stamp = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        return "${safeName}_rescued_$stamp.docx"
    }

    fun save(context: Context, fileName: String, bytes: ByteArray): Result<Destination> = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Result.success(saveToDownloads(context, fileName, bytes))
        } else {
            Result.success(saveForSharing(context, fileName, bytes))
        }
    } catch (e: Exception) {
        Result.failure(Exception(e.message ?: "Could not save the file."))
    }

    private fun saveToDownloads(context: Context, fileName: String, bytes: ByteArray): Destination {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, DOCX_MIME)
            // Marked pending until the bytes are written, so nothing else sees a half file.
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("Downloads folder is not available on this device.")

        resolver.openOutputStream(uri)?.use { it.write(bytes) }
            ?: throw IllegalStateException("Could not open the file for writing.")

        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, values, null, null)

        return Destination.Downloads(fileName)
    }

    private fun saveForSharing(context: Context, fileName: String, bytes: ByteArray): Destination {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, fileName)
        file.writeBytes(bytes)
        return Destination.NeedsSharing(file, fileName)
    }

    /** Matches the authority declared for the provider in AndroidManifest.xml. */
    fun shareIntent(context: Context, file: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = DOCX_MIME
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
            "Save your rescued CV"
        )
    }

    /** Only used on the pre-Android-10 path. */
    @Suppress("unused")
    fun legacyDownloadsDir(): File? =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
}
