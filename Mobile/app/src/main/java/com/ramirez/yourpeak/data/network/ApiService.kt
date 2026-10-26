package com.ramirez.yourpeak.data.network

import com.ramirez.yourpeak.data.model.LoginRequest
import com.ramirez.yourpeak.data.model.LoginResponse
import com.ramirez.yourpeak.data.model.RegistroRequest
import com.ramirez.yourpeak.data.model.RegistroResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/** Endpoints públicos de autenticación expuestos por Spring Boot. */
interface ApiService {

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/auth/registro")
    suspend fun registrar(@Body request: RegistroRequest): Response<RegistroResponse>
}
