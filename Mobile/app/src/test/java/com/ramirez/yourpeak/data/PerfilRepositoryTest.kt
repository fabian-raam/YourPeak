package com.ramirez.yourpeak.data

import com.ramirez.yourpeak.data.model.PerfilInicialRequest
import com.ramirez.yourpeak.data.network.RetrofitClient
import com.ramirez.yourpeak.data.repository.PerfilRepository
import com.ramirez.yourpeak.data.repository.ResultadoPerfil
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

/** Reproduce las respuestas de /api/cliente/perfil de Spring para comprobar el mapeo de la app. */
class PerfilRepositoryTest {

    private lateinit var servidor: MockWebServer
    private lateinit var repository: PerfilRepository

    @Before
    fun preparar() {
        servidor = MockWebServer().apply { start() }
        val api = RetrofitClient.crearApiService(servidor.url("/").toString())
        repository = PerfilRepository(api, RetrofitClient.gson, "clave-prueba")
    }

    @After
    fun cerrar() = servidor.shutdown()

    private fun json(codigo: Int, cuerpo: String) =
        MockResponse().setResponseCode(codigo).setHeader("Content-Type", "application/json").setBody(cuerpo)

    private val perfil = """{"completado":true,"idTipoObjetivo":2,"objetivo":"Ganar masa muscular",
        "idNivelExperiencia":2,"nivelExperiencia":"Intermedio","idNivelActividad":3,"nivelActividad":"Moderado",
        "sexo":"MUJER","pesoKg":62.50,"tallaCm":165.00,"fechaRegistroFisico":"2026-11-24T10:00:00",
        "catalogos":{"objetivos":[{"id":1,"nombre":"Perder grasa","descripcion":"Reducir grasa"},{"id":2,"nombre":"Ganar masa muscular","descripcion":null}],
        "nivelesExperiencia":[{"id":2,"nombre":"Intermedio","descripcion":""}],"nivelesActividad":[{"id":3,"nombre":"Moderado","descripcion":""}]}}"""

    @Test
    fun consultarLeePerfilYCatalogos() = runBlocking {
        servidor.enqueue(json(200, perfil))

        val resultado = repository.consultar(7)

        val peticion = servidor.takeRequest()
        assertEquals("GET", peticion.method)
        assertEquals("/api/cliente/perfil", peticion.path)
        assertEquals("7", peticion.getHeader("X-Client-User-Id"))
        val datos = (resultado as ResultadoPerfil.Exito).perfil
        assertEquals("MUJER", datos.sexo)
        assertEquals(0, BigDecimal("62.5").compareTo(datos.pesoKg))
        assertEquals(2, datos.catalogos!!.objetivos!!.size)
    }

    @Test
    fun guardarEnviaLosDatosConDecimales() = runBlocking {
        servidor.enqueue(json(200, perfil))

        repository.guardar(7, PerfilInicialRequest(2, 2, 3, "MUJER", BigDecimal("62.50"), BigDecimal("165.00")))

        val peticion = servidor.takeRequest()
        assertEquals("PUT", peticion.method)
        val cuerpo = peticion.body.readUtf8()
        assertTrue(cuerpo, cuerpo.contains("\"sexo\":\"MUJER\"") && cuerpo.contains("\"pesoKg\":62.50") && cuerpo.contains("\"tallaCm\":165.00"))
        assertTrue(cuerpo, cuerpo.contains("\"idTipoObjetivo\":2") && cuerpo.contains("\"idNivelActividad\":3"))
    }

    @Test
    fun cambiarElSexoMuestraElMotivoDeSpring() = runBlocking {
        servidor.enqueue(json(409, """{"detail":"El sexo solo se puede registrar una vez y no puede modificarse.","status":409}"""))

        val resultado = repository.guardar(7, PerfilInicialRequest(2, 2, 3, "HOMBRE", BigDecimal("70.00"), BigDecimal("170.00")))

        assertEquals("El sexo solo se puede registrar una vez y no puede modificarse.", (resultado as ResultadoPerfil.Error).mensaje)
    }
}
