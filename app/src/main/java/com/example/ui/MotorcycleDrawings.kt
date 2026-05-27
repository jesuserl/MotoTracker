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
                    color = Color(0xFF1A305C),
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
            "scrambler" -> {
                // Scrambler retro bench seat
                drawLine(
                    color = styleColor,
                    start = Offset(90f, 80f),
                    end = Offset(145f, 80f),
                    strokeWidth = 14f,
                    cap = StrokeCap.Round
                )
                // Retro classic tank
                drawLine(
                    color = Color(0xFFE2E8F0),
                    start = Offset(130f, 80f),
                    end = Offset(165f, 75f),
                    strokeWidth = 18f,
                    cap = StrokeCap.Round
                )
                // High scrambler exhaust
                drawLine(
                    color = Color(0xFF718096),
                    start = Offset(80f, 95f),
                    end = Offset(170f, 95f),
                    strokeWidth = 6f,
                    cap = StrokeCap.Round
                )
            }
            "cafe_racer" -> {
                // Drop cafe racer bars
                drawLine(
                    color = Color(0xFFA0AEC0),
                    start = Offset(155f, 55f),
                    end = Offset(145f, 65f),
                    strokeWidth = 6f,
                    cap = StrokeCap.Round
                )
                // Single seat cowl hump back
                drawLine(
                    color = styleColor,
                    start = Offset(90f, 82f),
                    end = Offset(120f, 82f),
                    strokeWidth = 14f,
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = styleColor,
                    radius = 8f,
                    center = Offset(83f, 76f)
                )
                // Classic flat tank
                drawLine(
                    color = Color(0xFFCBD5E0),
                    start = Offset(115f, 80f),
                    end = Offset(155f, 78f),
                    strokeWidth = 16f,
                    cap = StrokeCap.Round
                )
            }
            "tracker" -> {
                // Tracker style plate
                drawLine(
                    color = styleColor,
                    start = Offset(100f, 75f),
                    end = Offset(155f, 75f),
                    strokeWidth = 12f,
                    cap = StrokeCap.Round
                )
                // High exhaust pipe
                drawLine(
                    color = Color(0xFFA0AEC0),
                    start = Offset(70f, 90f),
                    end = Offset(150f, 90f),
                    strokeWidth = 5f
                )
                // Tracker number plate outline
                drawRect(
                    color = Color.White,
                    topLeft = Offset(150f, 40f),
                    size = androidx.compose.ui.geometry.Size(20f, 20f)
                )
            }
            else -> {
                // Custom / Bobber or Fallback style
                drawLine(
                    color = styleColor,
                    start = Offset(90f, 85f),
                    end = Offset(145f, 80f),
                    strokeWidth = 16f,
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
