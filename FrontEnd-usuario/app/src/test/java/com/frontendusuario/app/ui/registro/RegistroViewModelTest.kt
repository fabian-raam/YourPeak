package com.frontendusuario.app.ui.registro

import com.frontendusuario.app.data.remote.RegistroRequest
import com.frontendusuario.app.data.remote.RegistroResponse
import com.frontendusuario.app.data.repository.RegistroRepository
import com.frontendusuario.app.data.repository.RegistroResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeRegistroRepository : RegistroRepository() {
    var resultToReturn: RegistroResult? = null
    var callCount = 0

    override suspend fun registrar(request: RegistroRequest): RegistroResult {
        callCount++
        return resultToReturn ?: RegistroResult.UnknownError("No result configured")
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class RegistroViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeRegistroRepository
    private lateinit var viewModel: RegistroViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeRegistroRepository()
        viewModel = RegistroViewModel(repository = fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `registrar con contrasena corta menor a 15 caracteres establece error de campo sin llamar al repository`() = runTest {
        viewModel.registrar("Ana", "Torres", "ana@example.com", "Corta123!")

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.fieldErrors["contrasena"])
        assertEquals(0, fakeRepository.callCount)
    }

    @Test
    fun `registrar con campos validos llama al repository y establece estado de exito`() = runTest {
        fakeRepository.resultToReturn = RegistroResult.Success(
            RegistroResponse(
                idUsuario = 1L,
                idCliente = 1L,
                estado = "pendiente",
                mensaje = "Registro exitoso",
            )
        )

        viewModel.registrar("Ana", "Torres", "ana@example.com", "PasswordSegura123!")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.isSuccess)
        assertEquals("Registro exitoso", state.successMessage)
        assertEquals(1, fakeRepository.callCount)
    }

    @Test
    fun `prevencion de envios simultaneos ignora llamadas cuando ya esta cargando o completado`() = runTest {
        fakeRepository.resultToReturn = RegistroResult.Success(
            RegistroResponse(
                idUsuario = 1L,
                idCliente = 1L,
                estado = "pendiente",
            )
        )

        viewModel.registrar("Ana", "Torres", "ana@example.com", "PasswordSegura123!")
        viewModel.registrar("Ana", "Torres", "ana@example.com", "PasswordSegura123!")

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, fakeRepository.callCount)
    }
}
