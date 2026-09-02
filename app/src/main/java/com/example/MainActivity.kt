package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ui.navigation.NavGraph
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    companion object {
        const val EXTRA_OPEN_PROCESSOR = "extra_open_processor"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                NavGraph(
                    startDestination = if (intent.getBooleanExtra(EXTRA_OPEN_PROCESSOR, false)) {
                        "processor"
                    } else {
                        "home"
                    }
                )
            }
        }
    }
}
