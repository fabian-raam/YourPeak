package com.frontendusuario.app.data.remote

import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    /**
     * URL base centralizada para el backend Spring Boot.
     *
     * - Para celular físico conectado por USB con `adb reverse tcp:8080 tcp:8080`:
     *   "http://127.0.0.1:8080/"
     * - Para emulador estándar de Android:
     *   "http://10.0.2.2:8080/"
     */
    private const val BASE_URL = "http://10.0.2.2:8080/"

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val registroApi: RegistroApi by lazy {
        retrofit.create(RegistroApi::class.java)
    }
}
