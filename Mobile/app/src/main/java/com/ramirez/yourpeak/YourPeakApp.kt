package com.ramirez.yourpeak

import android.app.Application
import com.ramirez.yourpeak.data.network.RetrofitClient
import com.ramirez.yourpeak.data.repository.AuthRepository
import com.ramirez.yourpeak.data.repository.DisponibilidadRepository
import com.ramirez.yourpeak.data.repository.EjercicioRepository
import com.ramirez.yourpeak.data.repository.RutinaRepository
import com.ramirez.yourpeak.data.repository.NutricionRepository
import com.ramirez.yourpeak.data.session.SessionManager

/** Crea una sola vez las dependencias compartidas por las pantallas. */
class YourPeakApp : Application() {

    lateinit var authRepository: AuthRepository
        private set
    lateinit var ejercicioRepository: EjercicioRepository
        private set
    lateinit var disponibilidadRepository: DisponibilidadRepository
        private set
    lateinit var rutinaRepository: RutinaRepository
        private set
    lateinit var nutricionRepository: NutricionRepository
        private set
    lateinit var sessionManager: SessionManager
        private set

    override fun onCreate() {
        super.onCreate()
        val api = RetrofitClient.crearApiService(BuildConfig.API_BASE_URL, BuildConfig.DEBUG)
        authRepository = AuthRepository(api, RetrofitClient.gson)
        ejercicioRepository = EjercicioRepository(api, RetrofitClient.gson, BuildConfig.INTERNAL_API_KEY)
        disponibilidadRepository = DisponibilidadRepository(api, RetrofitClient.gson, BuildConfig.INTERNAL_API_KEY)
        rutinaRepository = RutinaRepository(api, RetrofitClient.gson, BuildConfig.INTERNAL_API_KEY)
        nutricionRepository = NutricionRepository(api, RetrofitClient.gson, BuildConfig.INTERNAL_API_KEY)
        sessionManager = SessionManager(this, RetrofitClient.gson)
    }
}
