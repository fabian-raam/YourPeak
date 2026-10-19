package com.frontendusuario.app.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface RegistroApi {
    @POST("api/auth/registro")
    suspend fun registrar(@Body request: RegistroRequest): Response<RegistroResponse>
}
