package com.example.purincar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.purincar.ui.theme.PurinBrown
import kotlin.collections.plus

@Composable
fun Home() {
    var cars by remember {
        mutableStateOf(listOf<String>())
    }

    if(cars.isNotEmpty()) {
        LazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(16.dp, 64.dp, 16.dp, 64.dp),
        ) {
            items(cars.size) { currentCar ->
                FloatingActionButton(
                    onClick = ({ cars = cars - cars[currentCar] }),
                    containerColor = PurinBrown,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .size(128.dp),
                ) {
                    Text(
                        text = cars[currentCar],
                        fontSize = 24.sp,
                        color = Color.White
                    )
                }
            }

        }
    }
    else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(48.dp)
        ) {
            Text(
                text = "Please enter a car",
                fontSize = 24.sp,
                modifier = Modifier.align(Alignment.Center)
            )

            FloatingActionButton(
                onClick = ({ cars = cars + listOf("Camry", "Civic") }),
                modifier = Modifier.align(Alignment.BottomEnd),
                shape = CircleShape,
                containerColor = PurinBrown
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add a car",
                    tint = Color.White
                )
            }
        }
    }
}
