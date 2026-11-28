package com.ramirez.yourpeak.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.ramirez.yourpeak.BuildConfig
import com.ramirez.yourpeak.ui.theme.FondoIlustracion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.Locale

private val rutaSubida = Regex("/api/ejercicios/imagenes/[a-f0-9]{32}\\.(jpg|png)")
private val clienteImagenes by lazy { OkHttpClient() }
private val cacheImagenes by lazy {
    object : LruCache<String, Bitmap>(16 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }
}

internal fun esImagenSubida(ruta: String?): Boolean = ruta != null && rutaSubida.matches(ruta)

@Composable
internal fun pintorIlustracionEjercicio(ruta: String?): Painter? {
    if (!esImagenSubida(ruta)) {
        return recursoIlustracionEjercicio(ruta)?.let { painterResource(it) }
    }

    val url = BuildConfig.API_BASE_URL + ruta.orEmpty().trimStart('/')
    var imagen by remember(url) { mutableStateOf(cacheImagenes.get(url)) }
    LaunchedEffect(url) {
        if (imagen != null) return@LaunchedEffect
        imagen = withContext(Dispatchers.IO) {
            try {
                clienteImagenes.newCall(Request.Builder().url(url).build()).execute().use { respuesta ->
                    if (!respuesta.isSuccessful) return@use null
                    BitmapFactory.decodeStream(respuesta.body?.byteStream())?.also {
                        cacheImagenes.put(url, it)
                    }
                }
            } catch (_: IOException) {
                null
            }
        }
    }
    return imagen?.let { remember(it) { BitmapPainter(it.asImageBitmap()) } }
}

/**
 * Resuelve la clave imagenRuta que entrega la API a un recurso incluido en la app.
 * Convención: "aperturas-con-mancuernas" -> "exercise_aperturas_con_mancuernas".
 */
@Composable
fun recursoIlustracionEjercicio(imagenRuta: String?): Int? {
    val context = LocalContext.current
    return remember(context, imagenRuta) {
        val clave = claveIlustracion(imagenRuta) ?: return@remember null

        context.resources
            .getIdentifier("exercise_$clave", "drawable", context.packageName)
            .takeIf { it != 0 }
    }
}

/** Nombre del recurso (sin el prefijo exercise_) para una imagenRuta, o null si no es válida. */
internal fun claveIlustracion(imagenRuta: String?): String? = imagenRuta
    ?.substringAfterLast('/')
    ?.substringBeforeLast('.')
    ?.trim()
    ?.lowercase(Locale.ROOT)
    ?.replace('-', '_')
    ?.takeIf { it.matches(Regex("[a-z0-9_]+")) }

/** Muestra la ilustración indicada por la ruta que viene asociada al ejercicio en la BD. */
@Composable
fun IlustracionEjercicio(
    imagenRuta: String?,
    grupoMuscular: String?,
    forma: Shape,
    modifier: Modifier = Modifier
) {
    val painter = pintorIlustracionEjercicio(imagenRuta)
    Box(
        modifier = modifier.clip(forma).background(FondoIlustracion),
        contentAlignment = Alignment.Center
    ) {
        if (painter != null) {
            Image(
                painter = painter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize(0.3f)
                    .clip(CircleShape)
                    .background(colorGrupoMuscular(grupoMuscular.orEmpty()))
            )
        }
    }
}
