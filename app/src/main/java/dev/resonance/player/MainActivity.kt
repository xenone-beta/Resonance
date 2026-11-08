package dev.resonance.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import dev.resonance.player.ui.common.PermissionGate
import dev.resonance.player.ui.theme.ResonanceTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ResonanceTheme((application as ResonanceApplication).settings) {
                Surface(color = MaterialTheme.colorScheme.surface) {
                    PermissionGate(onGranted = viewModel::startLibrary) {
                        ResonanceApp(viewModel)
                    }
                }
            }
        }
    }
}
