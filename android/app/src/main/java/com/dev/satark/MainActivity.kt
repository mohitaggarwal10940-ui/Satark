package com.dev.satark

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.dev.satark.ui.home.HomeViewModel
import com.dev.satark.ui.navigation.AppNavigation
import com.dev.satark.ui.theme.SatarkTheme
import java.util.Locale

import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import android.content.ContextWrapper
import android.content.res.Resources

class LocalizedActivityContext(
    private val activity: ComponentActivity,
    private val localizedRes: Resources
) : ContextWrapper(activity), ActivityResultRegistryOwner {
    override fun getResources(): Resources = localizedRes
    override val activityResultRegistry: ActivityResultRegistry
        get() = activity.activityResultRegistry
}

class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val selectedLanguage by homeViewModel.selectedLanguage.collectAsState()
            val baseContext = LocalContext.current
            val baseConfiguration = LocalConfiguration.current

            val localizedConfiguration = remember(selectedLanguage, baseConfiguration) {
                Configuration(baseConfiguration).apply {
                    setLocale(Locale.forLanguageTag(selectedLanguage))
                }
            }

            val localizedContext = remember(selectedLanguage, baseContext) {
                val configContext = baseContext.createConfigurationContext(localizedConfiguration)
                LocalizedActivityContext(
                    activity = this@MainActivity,
                    localizedRes = configContext.resources
                )
            }

            CompositionLocalProvider(
                LocalConfiguration provides localizedConfiguration,
                LocalContext provides localizedContext,
                LocalActivityResultRegistryOwner provides this@MainActivity
            ) {
                SatarkTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AppNavigation(viewModel = homeViewModel)
                    }
                }
            }
        }
    }
}