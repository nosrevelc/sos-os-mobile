package br.com.sos.osmobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import br.com.sos.osmobile.ui.OSMobileApp
import br.com.sos.osmobile.ui.theme.OSMobileTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        val appContainer = (application as OSMobileApplication).appContainer

        setContent {
            OSMobileTheme {
                OSMobileApp(appContainer = appContainer)
            }
        }
    }
}