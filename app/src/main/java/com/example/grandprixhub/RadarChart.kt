package com.example.grandprixhub

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.text.font.FontWeight

@Composable
fun ComparisonRadar(
    driver1Name: String,
    driver2Name: String,
    driver1Scores: Map<String, Float>,
    driver2Scores: Map<String, Float>
) {
    val labels = listOf("Qualy Pace", "Race Craft", "Peak Performance")

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.size(320.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(220.dp)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.width / 2f

                // 1. Draw the "Web" (Background Rings)
                for (level in 1..5) {
                    val ringRadius = radius * (level / 5f)
                    val ringPath = Path()
                    for (i in 0..2) {
                        val angle = i * (2 * PI / 3) - PI / 2
                        val x = center.x + ringRadius * cos(angle).toFloat()
                        val y = center.y + ringRadius * sin(angle).toFloat()
                        if (i == 0) ringPath.moveTo(x, y) else ringPath.lineTo(x, y)
                    }
                    ringPath.close()
                    drawPath(ringPath, Color.White.copy(alpha = 0.15f), style = Stroke(width = 1.dp.toPx()))
                }

                // 2. Draw the Labels (Qualy, Race Craft, Peak)
                labels.forEachIndexed { i, label ->
                    val angle = i * (2 * PI / 3) - PI / 2
                    val labelRadius = radius + 35.dp.toPx()
                    val x = center.x + labelRadius * cos(angle).toFloat()
                    val y = center.y + labelRadius * sin(angle).toFloat()

                    drawContext.canvas.nativeCanvas.drawText(
                        label.uppercase(),
                        x,
                        y,
                        Paint().apply {
                            color = Color.Gray.toArgb()
                            textSize = 32f
                            textAlign = Paint.Align.CENTER
                            typeface = Typeface.DEFAULT_BOLD
                        }
                    )
                }

                // 3. Draw Driver Shapes
                drawDriverShape(driver1Scores, labels, center, radius, Color(0xFFE10600))
                drawDriverShape(driver2Scores, labels, center, radius, Color(0xFF64C4FF))
            }
        }

        // 4. The Legend (Color Key)
        Row(
            modifier = Modifier.padding(top = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem(driver1Name, Color(0xFFE10600))
            Spacer(modifier = Modifier.width(24.dp))
            LegendItem(driver2Name, Color(0xFF64C4FF))
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDriverShape(
    scores: Map<String, Float>,
    labels: List<String>,
    center: Offset,
    radius: Float,
    color: Color
) {
    if (scores.isEmpty()) return
    val path = Path()
    labels.forEachIndexed { i, label ->
        val score = scores[label] ?: 5f // Default to middle if no data
        val normalizedRadius = (score / 10f) * radius
        val angle = i * (2 * PI / 3) - PI / 2
        val point = Offset(
            center.x + normalizedRadius * cos(angle).toFloat(),
            center.y + normalizedRadius * sin(angle).toFloat()
        )
        if (i == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
    }
    path.close()
    drawPath(path, color.copy(alpha = 0.35f), style = Fill)
    drawPath(path, color, style = Stroke(width = 2.dp.toPx()))
}

@Composable
fun LegendItem(name: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).background(color, shape = CircleShape))
        Spacer(modifier = Modifier.width(6.dp))
        Text(name.uppercase(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}