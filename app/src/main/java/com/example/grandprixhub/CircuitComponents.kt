package com.example.grandprixhub

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.pow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke

data class CircuitHotspot(
    val name: String,
    val description: String,
    val xOffset: Float,
    val yOffset: Float
)

@Composable
fun CircuitMapWithHotspots(circuitImage: Int, hotspots: List<CircuitHotspot>) {
    var selectedHotspot by remember { mutableStateOf<CircuitHotspot?>(null) }

    // Pulse Animation for the hotspots
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val radiusMultiplier by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 2f,
        animationSpec = infiniteRepeatable(animation = tween(1200), repeatMode = RepeatMode.Restart),
        label = "radius"
    )
    val alphaValue by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 0f,
        animationSpec = infiniteRepeatable(animation = tween(1200), repeatMode = RepeatMode.Restart),
        label = "alpha"
    )

    Box(modifier = Modifier.fillMaxWidth().aspectRatio(1.2f)) {
        Image(
            painter = painterResource(id = circuitImage),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        Canvas(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
            detectTapGestures { offset ->
                selectedHotspot = hotspots.find {
                    val centerX = it.xOffset * size.width
                    val centerY = it.yOffset * size.height
                    (offset.x - centerX).pow(2) + (offset.y - centerY).pow(2) < 2500 // 50px radius
                }
            }
        }) {
            hotspots.forEach { hotspot ->
                val centerX = hotspot.xOffset * size.width
                val centerY = hotspot.yOffset * size.height

                // Pulsing Halo
                drawCircle(color = Color.Red.copy(alpha = alphaValue), radius = 15f * radiusMultiplier, center = Offset(centerX, centerY))
                // Solid Core
                drawCircle(color = Color.Red, radius = 12f, center = Offset(centerX, centerY))
            }
        }

        selectedHotspot?.let { hotspot ->
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                color = Color(0xFF1F1F27),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f))
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(text = hotspot.name.uppercase(), color = Color.Red, fontWeight = FontWeight.Black)
                    Text(text = hotspot.description, color = Color.White, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}