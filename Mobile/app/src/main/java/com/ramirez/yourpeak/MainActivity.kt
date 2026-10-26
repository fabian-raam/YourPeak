package com.ramirez.yourpeak

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.ramirez.yourpeak.ui.navigation.AppNavigation
import com.ramirez.yourpeak.ui.theme.YourPeakTheme

class MainActivity : ComponentActivity() {

    private val permisoRedLocal =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pedirPermisoRedLocal()
        setContent {
            YourPeakTheme {
                AppNavigation()
            }
        }
    }

    /**
     * En desarrollo Spring Boot corre en la PC, que para Android 17 (API 37) es "red local".
     * Sin este permiso el sistema bloquea la conexión y la app solo ve un timeout.
     */
    private fun pedirPermisoRedLocal() {
        if (!BuildConfig.DEBUG || (Build.VERSION.SDK_INT < 37)) return
        if (checkSelfPermission(PERMISO_RED_LOCAL) != PackageManager.PERMISSION_GRANTED) {
            permisoRedLocal.launch(PERMISO_RED_LOCAL)
        }
    }

    private companion object {
        const val PERMISO_RED_LOCAL = "android.permission.ACCESS_LOCAL_NETWORK"
    }
}
