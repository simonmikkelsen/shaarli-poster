package com.shaarli.poster

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.shaarli.poster.ui.ShaarliPosterApp
import com.shaarli.poster.ui.theme.ShaarliPosterTheme
import com.shaarli.poster.ui.MainViewModel

class MainActivity : ComponentActivity() {

    private val sharedUrlState = mutableStateOf<String?>(null)
    private val mainViewModel: MainViewModel by viewModels {
        MainViewModel.provideFactory((application as ShaarliPosterApplication).container.repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedUrlState.value = extractSharedUrl(intent)

        setContent {
            val state = remember { sharedUrlState }
            ShaarliPosterTheme {
                ShaarliPosterApp(sharedUrlState = state, mainViewModel = mainViewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        sharedUrlState.value = extractSharedUrl(intent)
    }

    private fun extractSharedUrl(intent: Intent?): String? {
        if (intent?.action != Intent.ACTION_SEND) return null
        if (intent.type != "text/plain") return null

        val rawText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return null
        val matcher = Patterns.WEB_URL.matcher(rawText)
        return if (matcher.find()) matcher.group() else rawText
    }
}
