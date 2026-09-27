// The single activity that draws edge to edge and shows the app.
package com.example.purincar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.purincar.core.ui.theme.PurinCarTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PurinCarTheme {
                PurinCarRoot()
            }
        }
    }
}
