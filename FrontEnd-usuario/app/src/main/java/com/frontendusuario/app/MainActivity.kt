package com.frontendusuario.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.frontendusuario.app.ui.registro.RegistroScreen
import com.frontendusuario.app.ui.theme.FrontEndUsuarioTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FrontEndUsuarioTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    RegistroScreen(
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }
}
