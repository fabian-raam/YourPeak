package com.ramirez.yourpeak.data

import com.ramirez.yourpeak.data.network.RetrofitClient
import com.ramirez.yourpeak.data.repository.ApiResult
import com.ramirez.yourpeak.data.repository.AuthRepository
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Reproduce las respuestas reales de Spring Boot para comprobar el mapeo en la app. */
class AuthRepositoryTest {

    private lateinit var servidor: MockWebServer
    private lateinit var repository: AuthRepository

    @Before
    fun preparar() {
        servidor = MockWebServer().apply { start() }
        val api = RetrofitClient.crearApiService(servidor.url("/").toString())
        repository = AuthRepository(api, RetrofitClient.gson)
    }

    @After
    fun cerrar() = servidor.shutdown()

    private fun json(codigo: Int, cuerpo: String) =
        MockResponse().setResponseCode(codigo).setHeader("Content-Type", "application/json").setBody(cuerpo)

    @Test
    fun loginExitosoEnviaCorreoNormalizado() = runBlocking {
        servidor.enqueue(
            json(200, """{"idUsuario":7,"nombre":"Ana","apellido":"Torres","correo":"ana@tecsup.edu.pe","rol":"CLIENTE","estado":"PENDIENTE"}""")
        )

        val resultado = repository.login(" Ana@Tecsup.edu.pe ", "secreto123")

        val peticion = servidor.takeRequest()
        assertEquals("/api/auth/login", peticion.path)
        assertEquals("POST", peticion.method)
        assertEquals("""{"correo":"ana@tecsup.edu.pe","contrasena":"secreto123"}""", peticion.body.readUtf8())
        val usuario = (resultado as ApiResult.Exito).datos
        assertEquals(7L, usuario.idUsuario)
        assertEquals("PENDIENTE", usuario.estado)
    }

    @Test
    fun loginRechazadoMuestraMensajeDeSpring() = runBlocking {
        servidor.enqueue(json(401, """{"detail":"Correo o contraseña incorrectos."}"""))

        val resultado = repository.login("ana@tecsup.edu.pe", "mala")

        assertEquals("Correo o contraseña incorrectos.", (resultado as ApiResult.Error).mensaje)
    }

    @Test
    fun registroConCamposInvalidosDevuelveErroresPorCampo() = runBlocking {
        servidor.enqueue(
            json(400, """{"type":"about:blank","title":"Datos inválidos","status":400,"detail":"Revisa los campos del registro.","campos":{"correo":"Usa tu correo institucional @tecsup.edu.pe"}}""")
        )

        val resultado = repository.registrar("Ana", "Torres", "2000-01-01", "ana@gmail.com", "12345678")

        val error = resultado as ApiResult.Error
        assertEquals("Revisa los campos del registro.", error.mensaje)
        assertEquals("Usa tu correo institucional @tecsup.edu.pe", error.campos["correo"])
    }

    @Test
    fun registroEnviaFechaIso() = runBlocking {
        servidor.enqueue(
            json(201, """{"idUsuario":1,"idCliente":1,"nombre":"Ana","apellido":"Torres","correo":"ana@tecsup.edu.pe","estado":"PENDIENTE","mensaje":"Registro exitoso. Tu cuenta está pendiente de aprobación."}""")
        )

        val resultado = repository.registrar(" Ana ", "Torres", "2000-01-31", "ANA@tecsup.edu.pe", "12345678")

        val cuerpo = servidor.takeRequest().body.readUtf8()
        assertTrue(cuerpo.contains("\"fechaNacimiento\":\"2000-01-31\""))
        assertTrue(cuerpo.contains("\"nombre\":\"Ana\""))
        assertTrue(resultado is ApiResult.Exito)
    }

    @Test
    fun sinServidorDevuelveErrorDeConexion() = runBlocking {
        servidor.shutdown()

        val resultado = repository.login("ana@tecsup.edu.pe", "secreto123")

        assertTrue((resultado as ApiResult.Error).mensaje.startsWith("No se pudo conectar"))
    }
}
