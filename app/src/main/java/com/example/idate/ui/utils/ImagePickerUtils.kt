package com.example.idate.ui.utils

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object ImagePickerUtils {
    fun copyImageToInternalStorage(context: Context, sourceUri: Uri): String? {
        return try {
            val directory = File(context.filesDir, "custom_plan_images")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val fileName = "plan_img_${System.currentTimeMillis()}.jpg"
            val destinationFile = File(directory, fileName)

            val inputStream: InputStream? = context.contentResolver.openInputStream(sourceUri)
            val outputStream = FileOutputStream(destinationFile)

            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            destinationFile.absolutePath
        } catch (e: Exception) {
            android.util.Log.e("ImagePickerUtils", "Error guardando imagen local: ${e.message}")
            null
        }
    }
}
