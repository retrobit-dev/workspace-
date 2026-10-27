package dev.retrobit.ruangkelas

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
 * Aplikasi dibuka satu per satu sesuai pesanan.
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
