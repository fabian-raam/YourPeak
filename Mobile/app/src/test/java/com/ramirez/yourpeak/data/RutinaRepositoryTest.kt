package com.ramirez.yourpeak.data

import com.ramirez.yourpeak.data.model.DiaRutinaRequest
import com.ramirez.yourpeak.data.model.EjercicioRutinaRequest
import com.ramirez.yourpeak.data.model.RutinaRequest
import com.ramirez.yourpeak.data.network.RetrofitClient
import com.ramirez.yourpeak.data.repository.ResultadoGeneracion
import com.ramirez.yourpeak.data.repository.ResultadoRutina
import com.ramirez.yourpeak.data.repository.RutinaRepository
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Comprueba que la app conserva el origen IA de los ejercicios al leer y guardar la rutina. */
class RutinaRepositoryTest {

    private lateinit var servidor: MockWebServer
    private lateinit var repository: RutinaRepository

    @Before
    fun preparar() {
        servidor = MockWebServer().apply { start() }
        val api = RetrofitClient.crearApiService(servidor.url("/").toString())
        repository = RutinaRepository(api, RetrofitClient.gson, "clave-prueba")
    }

    @After
    fun cerrar() = servidor.shutdown()

    private fun json(codigo: Int, cuerpo: String) =
        MockResponse().setResponseCode(codigo).setHeader("Content-Type", "application/json").setBody(cuerpo)

    private val rutinaConIa = """{"dias":[{"diaSemana":1,"disponible":true,"horaInicio":"18:00:00","horaFin":"19:15:00",
        "ejercicios":[{"idEjercicio":3,"nombre":"Press de banca","grupoMuscular":"Pecho","tipoMedicion":"REPETICIONES",
        "orden":1,"series":4,"repeticiones":10,"segundos":null,"descansoSegundos":75,"origen":"IA"}]}]}"""

    @Test
    fun consultarLeeElOrigenDeCadaEjercicio() = runBlocking {
        servidor.enqueue(json(200, rutinaConIa))

        val resultado = repository.consultar(7)

        val ejercicio = (resultado as ResultadoRutina.Exito).rutina.dias!!.first().ejercicios!!.first()
        assertEquals("IA", ejercicio.origen)
    }

    @Test
    fun guardarEnviaElOrigenDeCadaEjercicio() = runBlocking {
        servidor.enqueue(json(200, rutinaConIa))
        val request = RutinaRequest(listOf(DiaRutinaRequest(1, listOf(
            EjercicioRutinaRequest(3, 4, 10, null, 75, "IA"),
            EjercicioRutinaRequest(5, 3, 12, null, 60, "USUARIO")
        ))))

        repository.guardar(7, request)

        val peticion = servidor.takeRequest()
        assertEquals("PUT", peticion.method)
        val cuerpo = peticion.body.readUtf8()
        assertTrue(cuerpo, cuerpo.contains("\"idEjercicio\":3") && cuerpo.contains("\"origen\":\"IA\""))
        assertTrue(cuerpo, cuerpo.contains("\"origen\":\"USUARIO\""))
    }

    @Test
    fun unConflictoMuestraElMotivoDeSpring() = runBlocking {
        servidor.enqueue(json(409, """{"detail":"Solo puedes asignar ejercicios a días disponibles.","instance":"/api/cliente/rutina","status":409,"title":"Conflict"}"""))

        val resultado = repository.guardar(7, RutinaRequest(emptyList()))

        assertEquals("Solo puedes asignar ejercicios a días disponibles.", (resultado as ResultadoRutina.Error).mensaje)
    }

    @Test
    fun generarPideLaPropuestaYLeeSuFuente() = runBlocking {
        servidor.enqueue(json(200, """{"fuente":"REGLAS","mensaje":"La IA no está configurada.",""" + rutinaConIa.removePrefix("{")))

        val resultado = repository.generar(7)

        val peticion = servidor.takeRequest()
        assertEquals("POST", peticion.method)
        assertEquals("/api/cliente/rutina/generar", peticion.path)
        assertEquals("7", peticion.getHeader("X-Client-User-Id"))
        val propuesta = (resultado as ResultadoGeneracion.Exito).propuesta
        assertEquals("REGLAS", propuesta.fuente)
        assertEquals("IA", propuesta.dias!!.first().ejercicios!!.first().origen)
    }

    @Test
    fun generarSinDisponibilidadMuestraElMotivoDeSpring() = runBlocking {
        servidor.enqueue(json(409, """{"detail":"Marca al menos un día disponible antes de generar tu rutina.","status":409,"title":"Conflict"}"""))

        val resultado = repository.generar(7)

        assertEquals("Marca al menos un día disponible antes de generar tu rutina.", (resultado as ResultadoGeneracion.Error).mensaje)
    }
}
