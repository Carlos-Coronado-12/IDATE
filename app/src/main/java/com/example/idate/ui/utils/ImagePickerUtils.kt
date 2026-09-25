package com.example.idate.ui.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object ImagePickerUtils {

    /**
     * Procesar y comprimir una imagen seleccionada de la galería.
     * Retorna una cadena Base64 Data URI ("data:image/jpeg;base64,...") lista para persistir
     * en Room y Firebase de forma permanente y cross-device.
     */
    fun processAndSaveGalleryImage(context: Context, sourceUri: Uri, maxDimension: Int = 1280, quality: Int = 88): String? {
        return try {
            // 1. Obtener dimensiones originales sin cargar todo el mapa de bits
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(sourceUri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            if (options.outWidth <= 0 || options.outHeight <= 0) {
                // Fallback directo a copia de archivo si no se pudieron leer las dimensiones
                return copyImageToInternalStorage(context, sourceUri)
            }

            // 2. Calcular inSampleSize para evitar problemas de memoria (OOM)
            var sampleSize = 1
            while ((options.outWidth / sampleSize) > maxDimension * 1.5 || (options.outHeight / sampleSize) > maxDimension * 1.5) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888 // Máxima nitidez y fidelidad de color
            }

            val rawBitmap: Bitmap = context.contentResolver.openInputStream(sourceUri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return null

            // 3. Manejar rotación EXIF si está presente
            val orientedBitmap = correctBitmapOrientation(context, sourceUri, rawBitmap)

            // 4. Escalar bitmap al tamaño máximo proporcional
            val width = orientedBitmap.width
            val height = orientedBitmap.height
            val maxEdge = maxOf(width, height)
            val finalBitmap = if (maxEdge > maxDimension) {
                val scale = maxDimension.toFloat() / maxEdge
                Bitmap.createScaledBitmap(orientedBitmap, (width * scale).toInt(), (height * scale).toInt(), true)
            } else {
                orientedBitmap
            }

            // 5. Comprimir a JPEG
            val byteOutputStream = ByteArrayOutputStream()
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteOutputStream)
            val imageBytes = byteOutputStream.toByteArray()

            // 6. Guardar también una copia local en disco como respaldo
            try {
                val directory = File(context.filesDir, "custom_plan_images")
                if (!directory.exists()) {
                    directory.mkdirs()
                }
                val fileName = "plan_img_${System.currentTimeMillis()}.jpg"
                val destinationFile = File(directory, fileName)
                FileOutputStream(destinationFile).use { fos ->
                    fos.write(imageBytes)
                }
            } catch (e: Exception) {
                android.util.Log.w("ImagePickerUtils", "No se pudo guardar copia local en disco: ${e.message}")
            }

            // 7. Retornar Data URI Base64
            val base64String = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
            "data:image/jpeg;base64,$base64String"
        } catch (e: Exception) {
            android.util.Log.e("ImagePickerUtils", "Error procesando imagen: ${e.message}", e)
            // Fallback a ruta de archivo si falla compresión
            copyImageToInternalStorage(context, sourceUri)
        }
    }

    private fun correctBitmapOrientation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val orientation = context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
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
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            bitmap
        }
    }

    fun copyImageToInternalStorage(context: Context, sourceUri: Uri): String? {
        return try {
            val directory = File(context.filesDir, "custom_plan_images")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val fileName = "plan_img_${System.currentTimeMillis()}.jpg"
            val destinationFile = File(directory, fileName)

            val inputStream = context.contentResolver.openInputStream(sourceUri)
            val outputStream = FileOutputStream(destinationFile)

            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            "file://${destinationFile.absolutePath}"
        } catch (e: Exception) {
            android.util.Log.e("ImagePickerUtils", "Error guardando imagen local: ${e.message}")
            null
        }
    }
}
