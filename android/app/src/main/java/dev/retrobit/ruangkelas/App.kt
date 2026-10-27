package dev.retrobit.ruangkelas

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // landscape saja (kedua arah), dikunci juga di manifest
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        setContent {
            RuangKelasTheme {
                val state = remember { AppState(applicationContext) }
                Root(state)
            }
        }
    }
}

/**
 * Akar aplikasi: launcher + layar aplikasi bawaan.
 * Saat ini baru Cuaca (kerangka); aplikasi lain menyusul satu per satu.
 */
@Composable
fun Root(state: AppState) {
    when (state.screen) {
        Scr.CUACA -> CuacaScreen(onBack = { state.screen = Scr.HOME })
        else -> LauncherScreen(state, onOpen = { tile ->
            if (tile.target == Scr.CUACA) state.screen = Scr.CUACA
        })
    }
}

/**
 * Layar Cuaca — kerangka sementara. Aplikasi Cuaca sendiri dibuat menyusul;
 * untuk sekarang cukup menyatakan bahwa jendela memang membuka aplikasi ini.
 */
@Composable
private fun CuacaScreen(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Night),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("CUACA", style = MaterialTheme.typography.headlineLarge)
            Text(
                "Aplikasi cuaca sedang disiapkan",
                style = MaterialTheme.typography.bodyMedium,
                color = Muted
            )
            Spacer(Modifier.height(20.dp))
            PixelButton("KEMBALI KE RUANGAN", onClick = onBack)
        }
    }
}
