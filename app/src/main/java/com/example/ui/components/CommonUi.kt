package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Severity
import com.example.ui.theme.*

@Composable
fun SeverityBadge(severity: Severity) {
    val (bg, fg, label) = when (severity) {
        Severity.HIGH -> Triple(SpicyRed.copy(alpha = 0.18f), SpicyRed, "High Severity")
        Severity.MEDIUM -> Triple(FireAmber.copy(alpha = 0.18f), FireAmber, "Moderate Issue")
        Severity.LOW -> Triple(Color(0xFF3B82F6).copy(alpha = 0.18f), Color(0xFF60A5FA), "Minor Polish")
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(1.dp, fg.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun FirePill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(FlameOrange.copy(alpha = 0.2f), FireAmber.copy(alpha = 0.2f))
                )
            )
            .border(1.dp, FlameOrange.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = FlameOrange,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun RescuePill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(RescueTeal.copy(alpha = 0.18f))
            .border(1.dp, RescueCyan.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = RescueCyan,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ScoreRadialMeter(score: Int, label: String, modifier: Modifier = Modifier) {
    val animatedScore by animateIntAsState(
        targetValue = score,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "scoreAnim"
    )

    val color = when {
        score >= 85 -> RescueGreen
        score >= 70 -> RescueCyan
        score >= 55 -> FireAmber
        else -> SpicyRed
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(110.dp)
        ) {
            CircularProgressIndicator(
                progress = { animatedScore / 100f },
                modifier = Modifier.fillMaxSize(),
                color = color,
                trackColor = color.copy(alpha = 0.15f),
                strokeWidth = 10.dp,
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$animatedScore",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = "/ 100",
                    fontSize = 12.sp,
                    color = SlateText
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun CategoryScoreBar(title: String, score: Int) {
    val animatedScore by animateFloatAsState(
        targetValue = score / 100f,
        animationSpec = tween(1000),
        label = "catAnim"
    )

    val barColor = when {
        score >= 75 -> RescueGreen
        score >= 60 -> RescueCyan
        score >= 45 -> FireAmber
        else -> SpicyRed
    }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = title, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Medium)
            Text(text = "$score%", fontSize = 13.sp, color = barColor, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { animatedScore },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = barColor,
            trackColor = SurfaceDark
        )
    }
}
