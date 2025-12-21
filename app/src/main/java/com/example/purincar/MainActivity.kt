package com.example.purincar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.purincar.ui.theme.PurinCarTheme
import com.example.purincar.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PurinCarTheme {
                Column {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .size(60.dp)
                            .background(PurinBrown)
                    )
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(PurinYellow)
                    ) {
                        Home()
                    }
                }
            }
        }
    }
}
