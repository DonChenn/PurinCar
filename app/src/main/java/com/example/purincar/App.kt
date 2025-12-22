package com.example.purincar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.purincar.data.CarDao
import com.example.purincar.navigation.NavigationRoot
import com.example.purincar.ui.theme.PurinBrown
import com.example.purincar.ui.theme.PurinCarTheme
import com.example.purincar.ui.theme.PurinYellow

@Composable
fun App(dao: CarDao) {
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
                // Pass dao to NavigationRoot
                NavigationRoot(dao = dao)
            }
        }
    }
}
