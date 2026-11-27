package com.ramirez.yourpeak.data

import com.ramirez.yourpeak.data.model.MensajeAsistente
import com.ramirez.yourpeak.data.network.RetrofitClient
import com.ramirez.yourpeak.data.repository.AsistenteRepository
import com.ramirez.yourpeak.data.repository.ResultadoAsistente
import com.ramirez.yourpeak.ui.home.AsistenteViewModel
import com.ramirez.yourpeak.ui.home.MensajeChat
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** El chat envía la pregunta con su contexto y muestra los límites que impone Spring. */
class AsistenteRepositoryTest {

    private lateinit var servidor: MockWebServer
    private lateinit var repository: AsistenteRepository

    @Before
    fun preparar() {
        servidor = MockWebServer().apply { start() }
        repository = AsistenteRepository(RetrofitClient.crearApiService(servidor.url("/").toString()), RetrofitClient.gson, "clave-prueba")
    }

    @After
    fun cerrar() = servidor.shutdown()

    private fun json(codigo: Int, cuerpo: String) =
        MockResponse().setResponseCode(codigo).setHeader("Content-Type", "application/json").setBody(cuerpo)

    @Test
    fun enviaLaPreguntaYLeeLaRespuesta() = runBlocking {
        servidor.enqueue(json(200, """{"respuesta":"Toma agua.","enTema":true,"consultasRestantesHoy":29,"limiteDiario":30}"""))

        val resultado = repository.preguntar(7, "  ¿Cuánta agua?  ", listOf(MensajeAsistente("usuario", "Hola"), MensajeAsistente("asistente", "¡Hola!")))

        val peticion = servidor.takeRequest()
        assertEquals("/api/cliente/asistente", peticion.path)
        assertEquals("7", peticion.getHeader("X-Client-User-Id"))
        val cuerpo = peticion.body.readUtf8()
        assertTrue(cuerpo, cuerpo.contains("\"pregunta\":\"¿Cuánta agua?\"") && cuerpo.contains("\"rol\":\"asistente\""))
        assertEquals(29, (resultado as ResultadoAsistente.Exito).respuesta.consultasRestantesHoy)
    }

    @Test
    fun elLimiteDiarioMuestraElMensajeDeSpring() = runBlocking {
        servidor.enqueue(json(429, """{"detail":"Alcanzaste las 30 consultas de hoy al asistente. Vuelve mañana.","status":429}"""))

        val resultado = repository.preguntar(7, "hola", emptyList())

        assertTrue((resultado as ResultadoAsistente.Error).mensaje.startsWith("Alcanzaste las 30 consultas"))
    }

    @Test
    fun elContextoExcluyeErroresYRespuestasFueraDeTema() {
        val historial = AsistenteViewModel.historialValido(listOf(
            MensajeChat(true, "¿Qué ceno?"), MensajeChat(false, "Pollo con arroz."),
            MensajeChat(true, "¿Quién ganó?"), MensajeChat(false, "Solo alimentación y ejercicio.", enTema = false),
            MensajeChat(true, "¿Y el desayuno?"), MensajeChat(false, "Sin conexión", error = true)
        ))

        assertEquals(listOf("¿Qué ceno?", "Pollo con arroz."), historial.map { it.texto })
        assertFalse(historial.any { it.texto.contains("ganó") })
    }
}
