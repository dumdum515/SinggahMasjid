package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.MosqueRepository
import com.example.data.OverpassRepository
import com.example.ui.SinggahMasjidApp
import com.example.ui.SinggahMasjidViewModel
import com.example.ui.theme.SinggahMasjidTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SinggahMasjidTheme {
                AppContent()
            }
        }
    }
}

@Composable
fun AppContent() {
    val context = LocalContext.current
    val prefs = remember(context) {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    val initialName = remember(prefs) {
        prefs.getString(KEY_USER_NAME, "Musafir") ?: "Musafir"
    }

    val viewModel: SinggahMasjidViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                SinggahMasjidViewModel(
                    mosqueRepository = MosqueRepository(app),
                    overpassRepository = OverpassRepository(),
                    currentUserId = MosqueRepository.LOCAL_USER_ID,
                    initialUserName = initialName,
                    onPersistUserName = { newName ->
                        prefs.edit().putString(KEY_USER_NAME, newName).apply()
                    }
                )
            }
        }
    )

    SinggahMasjidApp(viewModel = viewModel)
}

private const val PREFS_NAME = "singgah_masjid_prefs"
private const val KEY_USER_NAME = "local_user_name"
