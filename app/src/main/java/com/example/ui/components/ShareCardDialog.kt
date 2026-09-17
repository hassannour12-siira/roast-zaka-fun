package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.FullAnalysisResult
import com.example.ui.theme.*

@Composable
fun ShareCardDialog(
    result: FullAnalysisResult,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val roastPunchline = result.roast.observations.firstOrNull()?.roast
        ?: "Apparently 'responsible for' is not a personality trait."

    val shareText = """
🔥 MY CV GOT ROASTED by Roastumé!

Score: ${result.scores.overall}/100 (${result.scores.tierLabel})
Biggest Problem: "${result.roast.biggestRedFlag}"

AI Verdict: "$roastPunchline"

Get roasted. Get rescued. Get hired.
    """.trimIndent()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.linearGradient(
                    listOf(FlameOrange, RescueCyan)
                )
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Social Share Card",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = SlateText)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // The Social Card Preview (Designed for Screenshot / LinkedIn / Twitter)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF1E1428),
                                    Color(0xFF0F172A),
                                    Color(0xFF0C2434)
                                )
                            )
                        )
                        .border(1.5.dp, FlameOrange.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                        .padding(20.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🔥", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "MY CV GOT ROASTED",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = FlameOrange,
                                letterSpacing = 1.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Score Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White.copy(alpha = 0.1f))
                                .border(1.dp, RescueCyan, RoundedCornerShape(20.dp))
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "CV Score: ${result.scores.overall}/100 • ${result.scores.tierLabel}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "AI Recruiter Verdict:",
                            fontSize = 12.sp,
                            color = SlateText,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "\"$roastPunchline\"",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Biggest Flag: ${result.roast.biggestRedFlag}",
                            fontSize = 11.sp,
                            color = FireAmber,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        HorizontalDivider(color = BorderDark, thickness = 0.8.dp)

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Roastumé 🛟",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = RescueCyan
                            )
                            Text(
                                text = "#CVRoast",
                                fontSize = 11.sp,
                                color = SlateText
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Roast Card", shareText)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Roast summary copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Copy Text", fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share your CV Roast"))
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FlameOrange)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Share", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
