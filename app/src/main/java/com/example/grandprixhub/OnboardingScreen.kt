package com.example.grandprixhub

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(viewModel: MainViewModel, onComplete: () -> Unit) {
    var selectedDriver by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF15151E))
            .padding(24.dp)
    ) {
        Text(
            text = "PERSONALISE YOUR HUB",
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "CHOOSE YOUR FAVORITE DRIVER",
            color = Color(0xFFE10600),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // This uses your existing drivers list from the ViewModel
        LazyRow {
            items(viewModel.drivers.value) { driver ->
                FilterChip(
                    selected = selectedDriver == driver.Driver.driverId,
                    onClick = { selectedDriver = driver.Driver.driverId },
                    label = { Text(driver.Driver.familyName) },
                    modifier = Modifier.padding(end = 8.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFE10600),
                        selectedLabelColor = Color.White,
                        labelColor = Color.Gray
                    )
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                // Fix: Only passing driverId to match your MainViewModel function
                viewModel.saveUserPrefs(selectedDriver ?: "")
                onComplete()
            },
            enabled = selectedDriver != null,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFE10600),
                disabledContainerColor = Color.DarkGray
            )
        ) {
            Text("FINISH SETUP", fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}