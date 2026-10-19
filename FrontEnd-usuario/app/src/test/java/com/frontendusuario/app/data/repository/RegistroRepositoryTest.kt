package com.frontendusuario.app.data.repository

import com.frontendusuario.app.data.remote.RegistroApi
import com.frontendusuario.app.data.remote.RegistroRequest
import com.frontendusuario.app.data.remote.RegistroResponse
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class FakeRegistroApi : RegistroApi {
    var responseToReturn: Response<RegistroResponse>? = null
    var exceptionToThrow: Exception? = null

    override suspend fun registrar(request: RegistroRequest): Response<RegistroResponse> {
        exceptionToThrow?.let { throw it }
        return responseToReturn ?: throw IllegalStateException("No response configured")
    }
}

class RegistroRepositoryTest {

    private val fakeApi = FakeRegistroApi()
    private val repository = RegistroRepository(api = fakeApi)

    @Test
    fun `registrar devuelve Success cuando HTTP 201 tiene identificadores validos y estado pendiente`() = runTest {
        val responseBody = RegistroResponse(
            idUsuario = 10L,
            idCliente = 20L,
            nombre = "Ana",
            apellido = "Torres",
            correo = "ana@example.com",
            estado = "pendiente",
            mensaje = "Usuario creado exitosamente",
        )
        fakeApi.responseToReturn = Response.success(201, responseBody)

        val request = RegistroRequest("Ana", "Torres", "ana@example.com", "PasswordSegura123!")
        val result = repository.registrar(request)

        assertTrue(result is RegistroResult.Success)
        val successResult = result as RegistroResult.Success
        assertEquals(10L, successResult.response.idUsuario)
        assertEquals(20L, successResult.response.idCliente)
        assertEquals("pendiente", successResult.response.estado)
    }

    @Test
    fun `registrar devuelve UnknownError cuando HTTP 201 no tiene identificadores validos`() = runTest {
        val responseBody = RegistroResponse(
            idUsuario = null,
            idCliente = null,
            nombre = "Ana",
            estado = "pendiente",
        )
        fakeApi.responseToReturn = Response.success(201, responseBody)

        val request = RegistroRequest("Ana", "Torres", "ana@example.com", "PasswordSegura123!")
        val result = repository.registrar(request)

        assertTrue(result is RegistroResult.UnknownError)
    }

    @Test
    fun `registrar devuelve ValidationError cuando HTTP 400 contiene detail y campos`() = runTest {
        val jsonError = """
            {
                "detail": "Error de validación",
                "campos": {
                    "correo": "El correo no es válido"
                }
            }
        """.trimIndent()
        val errorResponseBody = jsonError.toResponseBody("application/json".toMediaType())
        fakeApi.responseToReturn = Response.error(400, errorResponseBody)

        val request = RegistroRequest("Ana", "Torres", "ana@example.com", "PasswordSegura123!")
        val result = repository.registrar(request)

        assertTrue(result is RegistroResult.ValidationError)
        val validationResult = result as RegistroResult.ValidationError
        assertEquals("Error de validación", validationResult.detail)
        assertEquals("El correo no es válido", validationResult.campos["correo"])
    }

    @Test
    fun `registrar devuelve ConflictError cuando HTTP 409 indica correo duplicado`() = runTest {
        val jsonError = """
            {
                "detail": "El correo ya existe"
            }
        """.trimIndent()
        val errorResponseBody = jsonError.toResponseBody("application/json".toMediaType())
        fakeApi.responseToReturn = Response.error(409, errorResponseBody)

        val request = RegistroRequest("Ana", "Torres", "ana@example.com", "PasswordSegura123!")
        val result = repository.registrar(request)

        assertTrue(result is RegistroResult.ConflictError)
        val conflictResult = result as RegistroResult.ConflictError
        assertEquals("El correo ya existe", conflictResult.detail)
    }

    @Test
    fun `registrar devuelve TimeoutError en SocketTimeoutException`() = runTest {
        fakeApi.exceptionToThrow = SocketTimeoutException("Read timed out")

        val request = RegistroRequest("Ana", "Torres", "ana@example.com", "PasswordSegura123!")
        val result = repository.registrar(request)

        assertTrue(result is RegistroResult.TimeoutError)
    }

    @Test
    fun `registrar devuelve ConnectionError en IOException`() = runTest {
        fakeApi.exceptionToThrow = IOException("Connection refused")

        val request = RegistroRequest("Ana", "Torres", "ana@example.com", "PasswordSegura123!")
        val result = repository.registrar(request)

        assertTrue(result is RegistroResult.ConnectionError)
    }
}
