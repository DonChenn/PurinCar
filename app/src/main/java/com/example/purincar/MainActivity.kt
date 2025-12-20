package com.example.purincar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.purincar.ui.theme.PurinCarTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PurinCarTheme {
                var cars by remember {
                    mutableStateOf(listOf<String>())
                }

                if(cars.isNotEmpty()) {
                    for(car in cars) {
                        Text(text = car)
                    }
                }
                else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(48.dp),
                    ) {
                        Text(
                            text = "Please enter a car",
                            textAlign = TextAlign.Center,
                            fontSize = 24.sp,
                            modifier = Modifier.align(Alignment.Center)
                        )

                        FloatingActionButton(
                            onClick = ({ cars = cars + listOf("Toyota") }),
                            modifier = Modifier.align(Alignment.BottomEnd),
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add a car",
                            )
                        }
                    }
                }
            }
        }
    }
}
