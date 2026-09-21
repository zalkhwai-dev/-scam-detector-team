package com.example.aiscamdetector
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.aiscamdetector.data.ServiceLocator
import com.example.aiscamdetector.navigation.AIScamDetectorNavHost
import com.example.aiscamdetector.ui.theme.AIScamDetectorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ServiceLocator.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            AIScamDetectorTheme {
                AIScamDetectorNavHost()
            }
        }
    }
}
