package com.ramirez.yourpeak.data.network

import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    val gson: Gson = Gson()

    fun crearApiService(baseUrl: String, mostrarLogs: Boolean = false): ApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = if (mostrarLogs) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
            // Nunca registrar cuerpos: contienen contraseñas.
        }

        val cliente = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(cliente)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
    }
}
