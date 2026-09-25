package com.example.idate.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Base64
import android.util.LruCache
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Precision
import coil.size.Scale
import com.example.idate.R
import com.example.idate.model.Plan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object PlanImageCache {
    // Cache de hasta 150 Bitmaps decodificados en memoria para rendimiento instantáneo en frame 0
    private val memoryCache = object : LruCache<String, Bitmap>(150) {
        override fun sizeOf(key: String, value: Bitmap): Int = 1
    }

    fun getBitmap(key: String): Bitmap? = memoryCache.get(key)

    fun putBitmap(key: String, bitmap: Bitmap) {
        if (!bitmap.isRecycled) {
            memoryCache.put(key, bitmap)
        }
    }

    suspend fun preloadImage(context: Context, imageUrl: String) {
        val trimmed = imageUrl.trim()
        if (trimmed.isBlank() || getBitmap(trimmed) != null) return

        withContext(Dispatchers.IO) {
            try {
                if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                    val request = ImageRequest.Builder(context)
                        .data(trimmed)
                        .precision(Precision.EXACT)
                        .scale(Scale.FILL)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .allowHardware(false)
                        .build()
                    val result = (context.imageLoader.execute(request) as? SuccessResult)?.drawable
                    val bitmap = drawableToBitmap(result)
                    if (bitmap != null) {
                        putBitmap(trimmed, bitmap)
                    }
                } else if (trimmed.startsWith("data:image") || (trimmed.length > 200 && !trimmed.startsWith("/"))) {
                    val rawBase64 = if (trimmed.contains(",")) trimmed.substringAfter(",") else trimmed
                    val decodedBytes = Base64.decode(rawBase64, Base64.DEFAULT)
                    val opts = BitmapFactory.Options().apply {
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }
                    val decoded = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size, opts)
                    if (decoded != null) {
                        putBitmap(trimmed, decoded)
                    }
                } else if (trimmed.startsWith("file://") || trimmed.startsWith("/")) {
                    val rawPath = if (trimmed.startsWith("file://")) trimmed.removePrefix("file://") else trimmed
                    val file = File(rawPath)
                    if (file.exists() && file.length() > 0) {
                        val opts = BitmapFactory.Options().apply {
                            inPreferredConfig = Bitmap.Config.ARGB_8888
                        }
                        val decoded = BitmapFactory.decodeFile(file.absolutePath, opts)
                        if (decoded != null) {
                            putBitmap(trimmed, decoded)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("PlanImageCache", "Error precargando imagen: ${e.message}")
            }
        }
    }
}

fun drawableToBitmap(drawable: Drawable?): Bitmap? {
    if (drawable == null) return null
    if (drawable is BitmapDrawable && drawable.bitmap != null && !drawable.bitmap.isRecycled) {
        return drawable.bitmap
    }
    return try {
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 1080
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 1920
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        bitmap
    } catch (e: Exception) {
        null
    }
}

/**
 * Componente unificado para renderizado de imágenes de planes con máxima nitidez (ARGB_8888),
 * encuadre perfecto y sin distorsión ni pixelación en el primer renderizado (frame 0).
 */
@Composable
fun PlanImage(
    plan: Plan,
    modifier: Modifier = Modifier,
    contentDescription: String? = plan.title,
    contentScale: ContentScale = ContentScale.Crop,
    @DrawableRes fallbackResId: Int = resolveFallbackForPlan(plan)
) {
    PlanImage(
        imageUrl = plan.imageUrl,
        fallbackResId = fallbackResId,
        modifier = modifier,
        contentDescription = contentDescription,
        contentScale = contentScale
    )
}

@Composable
fun PlanImage(
    imageUrl: String,
    @DrawableRes fallbackResId: Int = R.drawable.plan_legos,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    val trimmedUrl = imageUrl.trim()

    // CASO 1: Es un nombre de recurso predeterminado (ej: "plan_legos", "plan_sushi", etc.)
    val presetDrawable = getPresetDrawableId(trimmedUrl)
    if (presetDrawable != null) {
        Image(
            painter = painterResource(id = presetDrawable),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
            alignment = Alignment.Center
        )
        return
    }

    // CASO 2: Cadena Base64 Data URI o raw Base64 (Decodificación inmediata a GPU ImageBitmap)
    if (trimmedUrl.startsWith("data:image") || (trimmedUrl.length > 200 && !trimmedUrl.startsWith("http") && !trimmedUrl.startsWith("file://") && !trimmedUrl.startsWith("content://"))) {
        val cached = PlanImageCache.getBitmap(trimmedUrl)
        var bitmap by remember(trimmedUrl) { mutableStateOf(cached) }

        LaunchedEffect(trimmedUrl) {
            if (bitmap == null || bitmap!!.isRecycled) {
                withContext(Dispatchers.IO) {
                    try {
                        val rawBase64 = if (trimmedUrl.contains(",")) trimmedUrl.substringAfter(",") else trimmedUrl
                        val decodedBytes = Base64.decode(rawBase64, Base64.DEFAULT)
                        val opts = BitmapFactory.Options().apply {
                            inPreferredConfig = Bitmap.Config.ARGB_8888
                        }
                        val decoded = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size, opts)
                        if (decoded != null) {
                            PlanImageCache.putBitmap(trimmedUrl, decoded)
                            withContext(Dispatchers.Main) {
                                bitmap = decoded
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("PlanImage", "Error decodificando Base64: ${e.message}")
                    }
                }
            }
        }

        if (bitmap != null && !bitmap!!.isRecycled) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale,
                alignment = Alignment.Center,
                filterQuality = FilterQuality.High
            )
        } else {
            Image(
                painter = painterResource(id = fallbackResId),
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale,
                alignment = Alignment.Center
            )
        }
        return
    }

    // CASO 3: Archivo local (file://... o ruta absoluta)
    if (trimmedUrl.startsWith("file://") || (trimmedUrl.startsWith("/") && !trimmedUrl.startsWith("http"))) {
        val rawPath = if (trimmedUrl.startsWith("file://")) trimmedUrl.removePrefix("file://") else trimmedUrl
        val cached = PlanImageCache.getBitmap(trimmedUrl)
        var bitmap by remember(trimmedUrl) { mutableStateOf(cached) }

        LaunchedEffect(trimmedUrl) {
            if (bitmap == null || bitmap!!.isRecycled) {
                withContext(Dispatchers.IO) {
                    try {
                        val file = File(rawPath)
                        if (file.exists() && file.length() > 0) {
                            val opts = BitmapFactory.Options().apply {
                                inPreferredConfig = Bitmap.Config.ARGB_8888
                            }
                            val decoded = BitmapFactory.decodeFile(file.absolutePath, opts)
                            if (decoded != null) {
                                PlanImageCache.putBitmap(trimmedUrl, decoded)
                                withContext(Dispatchers.Main) {
                                    bitmap = decoded
                                }
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("PlanImage", "Error decodificando archivo: ${e.message}")
                    }
                }
            }
        }

        if (bitmap != null && !bitmap!!.isRecycled) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale,
                alignment = Alignment.Center,
                filterQuality = FilterQuality.High
            )
        } else {
            Image(
                painter = painterResource(id = fallbackResId),
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale,
                alignment = Alignment.Center
            )
        }
        return
    }

    // CASO 4: URL Web remota (Firebase Cloud Storage HTTPS, etc.) con decodificación directa a Bitmap GPU y cache
    if (trimmedUrl.startsWith("http://") || trimmedUrl.startsWith("https://")) {
        val cached = PlanImageCache.getBitmap(trimmedUrl)
        var bitmap by remember(trimmedUrl) { mutableStateOf(cached) }

        LaunchedEffect(trimmedUrl) {
            if (bitmap == null || bitmap!!.isRecycled) {
                withContext(Dispatchers.IO) {
                    try {
                        val request = ImageRequest.Builder(context)
                            .data(trimmedUrl)
                            .precision(Precision.EXACT)
                            .scale(Scale.FILL)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .allowHardware(false)
                            .build()
                        val result = (context.imageLoader.execute(request) as? SuccessResult)?.drawable
                        val decoded = drawableToBitmap(result)
                        if (decoded != null) {
                            PlanImageCache.putBitmap(trimmedUrl, decoded)
                            withContext(Dispatchers.Main) {
                                bitmap = decoded
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("PlanImage", "Error cargando URL remota: ${e.message}")
                    }
                }
            }
        }

        if (bitmap != null && !bitmap!!.isRecycled) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale,
                alignment = Alignment.Center,
                filterQuality = FilterQuality.High
            )
        } else {
            Image(
                painter = painterResource(id = fallbackResId),
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale,
                alignment = Alignment.Center
            )
        }
        return
    }

    // CASO 5: Fallback general nativo (Renderizado instantáneo al 100% de nitidez)
    Image(
        painter = painterResource(id = fallbackResId),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        alignment = Alignment.Center
    )
}

fun getPresetDrawableId(name: String): Int? {
    return when (name.trim().lowercase()) {
        "plan_legos" -> R.drawable.plan_legos
        "plan_sushi" -> R.drawable.plan_sushi
        "plan_pizza" -> R.drawable.plan_pizza
        "plan_tacos" -> R.drawable.plan_tacos
        "plan_cine" -> R.drawable.plan_cine
        "plan_drinks" -> R.drawable.plan_drinks
        "plan_escape" -> R.drawable.plan_escape
        "plan_boliche" -> R.drawable.plan_boliche
        "plan_picnic" -> R.drawable.plan_picnic
        "plan_senderismo" -> R.drawable.plan_senderismo
        "plan_jazz" -> R.drawable.plan_jazz
        "plan_ceramica" -> R.drawable.plan_ceramica
        else -> null
    }
}

/**
 * Obtener el Drawable ID adecuado para el plan según sus recursos o categoría
 */
fun resolveFallbackForPlan(plan: Plan): Int {
    if (plan.imageResId != 0) return plan.imageResId
    return getFallbackDrawableForCategory(plan.category)
}

fun getFallbackDrawableForCategory(category: String): Int {
    return when (category.lowercase().trim()) {
        "comida", "restaurante", "gastronomía" -> R.drawable.plan_sushi
        "película", "cine" -> R.drawable.plan_cine
        "fiesta", "drinks", "nocturno" -> R.drawable.plan_drinks
        "juegos", "escape room" -> R.drawable.plan_escape
        "boliche" -> R.drawable.plan_boliche
        "aire libre", "parque", "picnic" -> R.drawable.plan_picnic
        "tacos" -> R.drawable.plan_tacos
        "pizza" -> R.drawable.plan_pizza
        "senderismo", "aventura", "naturaleza" -> R.drawable.plan_senderismo
        "música", "jazz", "concierto" -> R.drawable.plan_jazz
        "arte", "cerámica", "manualidades", "cultura" -> R.drawable.plan_ceramica
        "romántico" -> R.drawable.plan_picnic
        else -> R.drawable.plan_legos
    }
}

