package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*

@Composable
fun MotorcycleDrawing(style: String, modifier: Modifier = Modifier) {
    val styleColor = when (style.lowercase()) {
        "sport" -> MotoDanger
        "scooter" -> MotoPrimary
        "adventure" -> MotoAccent
        "cruiser" -> Color(0xFF9C27B0)
        else -> MotoPrimary
    }

    Canvas(modifier = modifier.size(80.dp)) {
        // Draw Wheels
        // Left wheel
        drawCircle(
            color = Color(0xFF2D3748),
            radius = 24f,
            center = Offset(50f, 120f),
            style = Stroke(width = 10f)
        )
        drawCircle(
            color = MotoPrimary,
            radius = 18f,
            center = Offset(50f, 120f),
            style = Stroke(width = 3f)
        )

        // Right wheel
        drawCircle(
            color = Color(0xFF2D3748),
            radius = 24f,
            center = Offset(190f, 120f),
            style = Stroke(width = 10f)
        )
        drawCircle(
            color = MotoPrimary,
            radius = 18f,
            center = Offset(190f, 120f),
            style = Stroke(width = 3f)
        )

        // Draw Frame tubes / forks
        // Front Fork
        drawLine(
            color = Color(0xFFA0AEC0),
            start = Offset(190f, 120f),
            end = Offset(160f, 50f),
            strokeWidth = 8f,
            cap = StrokeCap.Round
        )

        // Frame structure
        drawLine(
            color = Color(0xFF2D3748),
            start = Offset(50f, 120f),
            end = Offset(105f, 120f),
            strokeWidth = 10f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFF2D3748),
            start = Offset(105f, 120f),
            end = Offset(130f, 75f),
            strokeWidth = 10f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFF2D3748),
            start = Offset(130f, 75f),
            end = Offset(165f, 75f),
            strokeWidth = 10f
        )
        drawLine(
            color = Color(0xFF2D3748),
            start = Offset(130f, 75f),
            end = Offset(100f, 120f),
            strokeWidth = 10f
        )

        // Specialized shapes per style type
        when (style.lowercase()) {
            "sport" -> {
                // Tank and seat (Sport)
                drawLine(
                    color = styleColor,
                    start = Offset(90f, 70f),
                    end = Offset(140f, 65f),
                    strokeWidth = 16f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color(0xFF1A202C),
                    start = Offset(130f, 85f),
                    end = Offset(165f, 80f),
                    strokeWidth = 12f,
                    cap = StrokeCap.Round
                )
            }
            "scooter" -> {
                // Frame low footrest
                drawLine(
                    color = Color(0xFF2D3748),
                    start = Offset(80f, 115f),
                    end = Offset(155f, 115f),
                    strokeWidth = 12f
                )
                // Seat (Scooter)
                drawLine(
                    color = styleColor,
                    start = Offset(110f, 85f),
                    end = Offset(165f, 85f),
                    strokeWidth = 18f,
                    cap = StrokeCap.Round
                )
            }
            "adventure" -> {
                // High clearance tank and rear rack box
                drawLine(
                    color = styleColor,
                    start = Offset(90f, 60f),
                    end = Offset(145f, 65f),
                    strokeWidth = 18f,
                    cap = StrokeCap.Round
                )
                // Adventure Box (Top Case)
                drawRect(
                    color = Color(0xFF4A5568),
                    topLeft = Offset(50f, 45f),
                    size = androidx.compose.ui.geometry.Size(32f, 32f)
                )
                drawLine(
                    color = Color(0xFF2D3748),
                    start = Offset(50f, 77f),
                    end = Offset(82f, 77f),
                    strokeWidth = 4f
                )
            }
            "cruiser" -> {
                // Lower frame and very long front rake fork
                drawLine(
                    color = Color(0xFFA0AEC0),
                    start = Offset(190f, 120f),
                    end = Offset(140f, 30f),
                    strokeWidth = 7f,
                    cap = StrokeCap.Round
                )
                // Teardrop tank & low-slung seat
                drawLine(
                    color = styleColor,
                    start = Offset(95f, 85f),
                    end = Offset(145f, 75f),
                    strokeWidth = 20f,
                    cap = StrokeCap.Round
                )
            }
        }

        // Handlebars
        drawLine(
            color = Color(0xFFA0AEC0),
            start = Offset(155f, 50f),
            end = Offset(145f, 40f),
            strokeWidth = 6f,
            cap = StrokeCap.Round
        )
    }
}
