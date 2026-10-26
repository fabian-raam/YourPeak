package com.ramirez.yourpeak

import android.app.Application
import com.ramirez.yourpeak.data.network.RetrofitClient
import com.ramirez.yourpeak.data.repository.AuthRepository
import com.ramirez.yourpeak.data.session.SessionManager

/** Crea una sola vez las dependencias compartidas por las pantallas. */
class YourPeakApp : Application() {

    lateinit var authRepository: AuthRepository
        private set
    lateinit var sessionManager: SessionManager
        private set

    override fun onCreate() {
        super.onCreate()
        val api = RetrofitClient.crearApiService(BuildConfig.API_BASE_URL, BuildConfig.DEBUG)
        authRepository = AuthRepository(api, RetrofitClient.gson)
        sessionManager = SessionManager(this, RetrofitClient.gson)
    }
}
