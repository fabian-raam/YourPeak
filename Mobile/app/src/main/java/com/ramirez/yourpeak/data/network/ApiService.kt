package com.ramirez.yourpeak.data.network

import com.ramirez.yourpeak.data.model.LoginRequest
import com.ramirez.yourpeak.data.model.LoginResponse
import com.ramirez.yourpeak.data.model.MembresiaPeriodoResponse
import com.ramirez.yourpeak.data.model.EjercicioResponse
import com.ramirez.yourpeak.data.model.DisponibilidadRequest
import com.ramirez.yourpeak.data.model.DisponibilidadResponse
import com.ramirez.yourpeak.data.model.AlimentoNutricionResponse
import com.ramirez.yourpeak.data.model.IngestaNutricionResponse
import com.ramirez.yourpeak.data.model.RegistrarIngestaRequest
import com.ramirez.yourpeak.data.model.ResumenNutricionalResponse
import com.ramirez.yourpeak.data.model.PerfilInicialRequest
import com.ramirez.yourpeak.data.model.PerfilInicialResponse
import com.ramirez.yourpeak.data.model.RutinaGeneradaResponse
import com.ramirez.yourpeak.data.model.RutinaRequest
import com.ramirez.yourpeak.data.model.RutinaResponse
import com.ramirez.yourpeak.data.model.RegistrarEjercicioRealizadoRequest
import com.ramirez.yourpeak.data.model.EjercicioRealizadoResponse
import com.ramirez.yourpeak.data.model.RegistroProgresoRequest
import com.ramirez.yourpeak.data.model.RegistroProgresoResponse
import com.ramirez.yourpeak.data.model.RegistroRequest
import com.ramirez.yourpeak.data.model.RegistroResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.DELETE
import retrofit2.http.Query
import retrofit2.http.PUT

/** Endpoints públicos de autenticación expuestos por Spring Boot. */
interface ApiService {

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    /** Obtiene el estado actualizado del usuario después de restaurar una sesión guardada. */
    @GET("api/auth/usuarios/{idUsuario}")
    suspend fun consultarUsuario(
        @Header("X-Internal-Api-Key") apiKey: String,
        @Path("idUsuario") idUsuario: Long
    ): Response<LoginResponse>

    @POST("api/auth/registro")
    suspend fun registrar(@Body request: RegistroRequest): Response<RegistroResponse>

    /** Catálogo activo disponible para que un cliente consulte ejercicios. */
    @GET("api/cliente/rutina/ejercicios")
    suspend fun listarEjerciciosDisponibles(
        @Header("X-Internal-Api-Key") apiKey: String,
        @Header("X-Client-User-Id") idUsuario: Long
    ): Response<List<EjercicioResponse>>

    /** Consulta los siete días de disponibilidad del cliente activo. */
    @GET("api/cliente/disponibilidad")
    suspend fun consultarDisponibilidad(
        @Header("X-Internal-Api-Key") apiKey: String,
        @Header("X-Client-User-Id") idUsuario: Long
    ): Response<DisponibilidadResponse>

    /** Reemplaza la disponibilidad semanal completa del cliente. */
    @PUT("api/cliente/disponibilidad")
    suspend fun guardarDisponibilidad(
        @Header("X-Internal-Api-Key") apiKey: String,
        @Header("X-Client-User-Id") idUsuario: Long,
        @Body request: DisponibilidadRequest
    ): Response<DisponibilidadResponse>

    /** Lista los periodos de membresía del cliente con su plan, fechas y estado. */
    @GET("api/cliente/perfil/membresias")
    suspend fun consultarMembresias(
        @Header("X-Internal-Api-Key") apiKey: String,
        @Header("X-Client-User-Id") idUsuario: Long
    ): Response<List<MembresiaPeriodoResponse>>

    /** Consulta el perfil inicial del cliente junto con los catálogos para el formulario. */
    @GET("api/cliente/perfil")
    suspend fun consultarPerfil(
        @Header("X-Internal-Api-Key") apiKey: String,
        @Header("X-Client-User-Id") idUsuario: Long
    ): Response<PerfilInicialResponse>

    /** Guarda objetivo, niveles, sexo y medidas del cliente. */
    @PUT("api/cliente/perfil")
    suspend fun guardarPerfil(
        @Header("X-Internal-Api-Key") apiKey: String,
        @Header("X-Client-User-Id") idUsuario: Long,
        @Body request: PerfilInicialRequest
    ): Response<PerfilInicialResponse>

    /** Consulta los siete días de la rutina semanal del cliente. */
    @GET("api/cliente/rutina")
    suspend fun consultarRutina(
        @Header("X-Internal-Api-Key") apiKey: String,
        @Header("X-Client-User-Id") idUsuario: Long
    ): Response<RutinaResponse>

    /** Pide a Spring una propuesta de rutina con IA (Gemini o reglas); no la guarda. */
    @POST("api/cliente/rutina/generar")
    suspend fun generarRutina(
        @Header("X-Internal-Api-Key") apiKey: String,
        @Header("X-Client-User-Id") idUsuario: Long
    ): Response<RutinaGeneradaResponse>

    /** Reemplaza la planificación semanal completa. */
    @PUT("api/cliente/rutina")
    suspend fun guardarRutina(
        @Header("X-Internal-Api-Key") apiKey: String,
        @Header("X-Client-User-Id") idUsuario: Long,
        @Body request: RutinaRequest
    ): Response<RutinaResponse>

    /** Registra que el cliente completÃ³ hoy todas las series de un ejercicio de su rutina. */
    @POST("api/cliente/rutina/ejercicios/{idPlanEjercicio}/completar")
    suspend fun completarEjercicioRutina(
        @Header("X-Internal-Api-Key") apiKey: String,
        @Header("X-Client-User-Id") idUsuario: Long,
        @Path("idPlanEjercicio") idPlanEjercicio: Long,
        @Body request: RegistrarEjercicioRealizadoRequest
    ): Response<EjercicioRealizadoResponse>

    /** Lista los alimentos del catálogo que puede registrar un cliente activo. */
    @GET("api/cliente/nutricion/alimentos")
    suspend fun listarAlimentosNutricion(
        @Header("X-Internal-Api-Key") apiKey: String,
        @Header("X-Client-User-Id") idUsuario: Long
    ): Response<List<AlimentoNutricionResponse>>

    /** Consulta los consumos y totales nutricionales de una fecha. */
    @GET("api/cliente/nutricion")
    suspend fun consultarNutricionDia(
        @Header("X-Internal-Api-Key") apiKey: String,
        @Header("X-Client-User-Id") idUsuario: Long,
        @Query("fecha") fecha: String?
    ): Response<ResumenNutricionalResponse>

    /** Registra un alimento consumido en la fecha actual. */
    @POST("api/cliente/nutricion/ingestas")
    suspend fun registrarIngesta(
        @Header("X-Internal-Api-Key") apiKey: String,
        @Header("X-Client-User-Id") idUsuario: Long,
        @Body request: RegistrarIngestaRequest
    ): Response<IngestaNutricionResponse>

    /** Actualiza alimento o cantidad de un consumo propio. */
    @PUT("api/cliente/nutricion/ingestas/{idIngesta}")
    suspend fun actualizarIngesta(
        @Header("X-Internal-Api-Key") apiKey: String,
        @Header("X-Client-User-Id") idUsuario: Long,
        @Path("idIngesta") idIngesta: Long,
        @Body request: RegistrarIngestaRequest
    ): Response<IngestaNutricionResponse>

    /** Elimina un consumo propio. */
    @DELETE("api/cliente/nutricion/ingestas/{idIngesta}")
    suspend fun eliminarIngesta(
        @Header("X-Internal-Api-Key") apiKey: String,
        @Header("X-Client-User-Id") idUsuario: Long,
        @Path("idIngesta") idIngesta: Long
    ): Response<Unit>

    /** Consulta el historial de peso del cliente autenticado. */
    @GET("api/cliente/progreso")
    suspend fun consultarProgreso(
        @Header("X-Internal-Api-Key") apiKey: String,
        @Header("X-Client-User-Id") idUsuario: Long
    ): Response<List<RegistroProgresoResponse>>

    /** Registra una medición nueva de peso para el cliente autenticado. */
    @POST("api/cliente/progreso/peso")
    suspend fun registrarPeso(
        @Header("X-Internal-Api-Key") apiKey: String,
        @Header("X-Client-User-Id") idUsuario: Long,
        @Body request: RegistroProgresoRequest
    ): Response<RegistroProgresoResponse>
}
