package com.example.idate.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.UUID

object FirebaseStorageManager {

    private val storage: FirebaseStorage by lazy {
        try {
            FirebaseStorage.getInstance("gs://idate-8948b.firebasestorage.app")
        } catch (_: Exception) {
            FirebaseStorage.getInstance()
        }
    }

    /**
     * Comprime una imagen de la galería y la sube a Firebase Cloud Storage.
     * Retorna la URL pública HTTPS de descarga permanente accesible desde cualquier dispositivo.
     */
    suspend fun uploadPlanImage(
        context: Context,
        sourceUri: Uri,
        maxDimension: Int = 1280,
        quality: Int = 88
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // 1. Obtener dimensiones para muestreo
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(sourceUri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }

            if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) {
                return@withContext Result.failure(Exception("No se pudo leer la imagen seleccionada"))
            }

            // 2. Calcular sample size para proteger memoria
            var sampleSize = 1
            while ((boundsOptions.outWidth / sampleSize) > maxDimension * 1.5 || (boundsOptions.outHeight / sampleSize) > maxDimension * 1.5) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val rawBitmap: Bitmap = context.contentResolver.openInputStream(sourceUri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return@withContext Result.failure(Exception("Error al decodificar la imagen"))

            // 3. Corregir orientación EXIF
            val orientedBitmap = correctBitmapOrientation(context, sourceUri, rawBitmap)

            // 4. Escalar bitmap si excede el tamaño máximo
            val width = orientedBitmap.width
            val height = orientedBitmap.height
            val maxEdge = maxOf(width, height)
            val finalBitmap = if (maxEdge > maxDimension) {
                val scale = maxDimension.toFloat() / maxEdge
                Bitmap.createScaledBitmap(orientedBitmap, (width * scale).toInt(), (height * scale).toInt(), true)
            } else {
                orientedBitmap
            }

            // 5. Comprimir a formato JPEG
            val byteOutputStream = ByteArrayOutputStream()
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteOutputStream)
            val imageBytes = byteOutputStream.toByteArray()

            // 6. Subir a Firebase Cloud Storage
            val uniqueFileName = "plan_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg"
            val imageRef = storage.reference.child("plans_images/$uniqueFileName")

            val metadata = StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .setCustomMetadata("uploadedAt", System.currentTimeMillis().toString())
                .build()

            val uploadTask = imageRef.putBytes(imageBytes, metadata).await()
            val downloadUrl = uploadTask.storage.downloadUrl.await().toString()

            android.util.Log.d("FirebaseStorage", "Imagen subida exitosamente: $downloadUrl")
            Result.success(downloadUrl)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseStorage", "Error subiendo imagen a Firebase Storage: ${e.message}", e)
            Result.failure(e)
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
        } catch (_: Exception) {
            bitmap
        }
    }
}
