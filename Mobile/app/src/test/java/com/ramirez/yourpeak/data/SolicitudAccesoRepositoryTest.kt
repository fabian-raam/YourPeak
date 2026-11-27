package com.ramirez.yourpeak.data

import com.ramirez.yourpeak.data.network.RetrofitClient
import com.ramirez.yourpeak.data.repository.ResultadoSolicitud
import com.ramirez.yourpeak.data.repository.SolicitudAccesoRepository
import com.ramirez.yourpeak.domain.DestinoUsuario
import com.ramirez.yourpeak.domain.determinarDestinoUsuario
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** La app muestra el motivo del rechazo y respeta el tope diario que aplica Spring. */
class SolicitudAccesoRepositoryTest {

    private lateinit var servidor: MockWebServer
    private lateinit var repository: SolicitudAccesoRepository

    @Before
    fun preparar() {
        servidor = MockWebServer().apply { start() }
        repository = SolicitudAccesoRepository(RetrofitClient.crearApiService(servidor.url("/").toString()), RetrofitClient.gson, "clave-prueba")
    }

    @After
    fun cerrar() = servidor.shutdown()

    private fun json(codigo: Int, cuerpo: String) =
        MockResponse().setResponseCode(codigo).setHeader("Content-Type", "application/json").setBody(cuerpo)

    @Test
    fun consultarLeeElMotivoYLasSolicitudesRestantes() = runBlocking {
        servidor.enqueue(json(200, """{"estado":"RECHAZADO","puedeSolicitar":true,"solicitudesRestantesHoy":2,"limiteDiario":3,
            "historial":[{"numero":1,"estado":"RECHAZADA","mensajeCliente":null,"motivoRechazo":"Falta el pago",
            "fechaSolicitud":"2026-11-25T14:36:51","fechaResolucion":"2026-11-26T09:10:00"}]}"""))

        val solicitud = (repository.consultar(7) as ResultadoSolicitud.Exito).datos

        val peticion = servidor.takeRequest()
        assertEquals("/api/cliente/solicitud", peticion.path)
        assertEquals("7", peticion.getHeader("X-Client-User-Id"))
        assertEquals("Falta el pago", solicitud.historial!!.first().motivoRechazo)
        assertEquals(2, solicitud.solicitudesRestantesHoy)
        assertTrue(solicitud.puedeSolicitar)
    }

    @Test
    fun volverASolicitarEnviaElMensajeRecortado() = runBlocking {
        servidor.enqueue(json(200, """{"estado":"PENDIENTE","puedeSolicitar":false,"solicitudesRestantesHoy":1,"limiteDiario":3,"historial":[]}"""))

        repository.volverASolicitar(7, "  Ya pagué  ")

        val peticion = servidor.takeRequest()
        assertEquals("POST", peticion.method)
        assertEquals("""{"mensaje":"Ya pagué"}""", peticion.body.readUtf8())
    }

    @Test
    fun elTopeDiarioMuestraElMensajeDeSpring() = runBlocking {
        servidor.enqueue(json(429, """{"detail":"Alcanzaste el máximo de 3 solicitudes por día. Inténtalo de nuevo mañana.","status":429}"""))

        val error = repository.volverASolicitar(7, null) as ResultadoSolicitud.Error

        assertEquals(429, error.codigo)
        assertTrue(error.mensaje.startsWith("Alcanzaste el máximo"))
    }

    @Test
    fun unaCuentaRechazadaVaASuPantalla() {
        assertEquals(DestinoUsuario.CUENTA_RECHAZADA, determinarDestinoUsuario("CLIENTE", "rechazado"))
    }
}
