package org.elsoft.bkdb

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.russhwolf.settings.SharedPreferencesSettings
import org.elsoft.bkdb.utils.appSettings

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Android-specific settings
        val sharedPrefs = getSharedPreferences("ebook_prefs", MODE_PRIVATE)
        appSettings = SharedPreferencesSettings(sharedPrefs)

        setContent {
            // This calls your shared UI from commonMain
            //App()
        }
    }
}
