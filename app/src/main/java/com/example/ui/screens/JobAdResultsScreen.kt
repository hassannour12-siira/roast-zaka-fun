package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.JobAdAnalysisResult
import com.example.model.JobAdIssue
import com.example.ui.components.CategoryScoreBar
import com.example.ui.components.ScoreRadialMeter
import com.example.ui.components.SeverityBadge
import com.example.ui.theme.*

/**
 * The recruiter side of the results: their own job advert, roasted then rewritten.
 *
 * Single scroll rather than the two tabs the CV screen uses. A recruiter is checking one
 * document before they publish it, so splitting nine cards across two tabs would only
 * hide the fixes behind an extra tap.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JobAdResultsScreen(
    result: JobAdAnalysisResult,
    onStartOver: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepCharcoal)
    ) {
        Surface(color = SurfaceDark, shadowElevation = 4.dp) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "📋", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = result.job.title.ifBlank { "Your job advert" },
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = listOf(result.job.company, result.job.seniority)
                                .filter { it.isNotBlank() && it != "Not stated" }
                                .joinToString(" · ")
                                .ifBlank { "Roasted as a recruiter would read it" },
                            fontSize = 12.sp,
                            color = SlateText
                        )
                    }
                    Text(text = result.intensity.peppers, fontSize = 14.sp)
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
                .testTag(JOB_AD_LIST_TAG),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Opening line
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1428)),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(listOf(FlameOrange, FireAmber))
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🔥", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Your job advert, roasted",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black,
                                color = FlameOrange
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "\"${result.roast.openingLine}\"",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            // What a candidate thinks
            if (result.roast.candidateFirstImpression.isNotBlank()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = RescueCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "What a good candidate thinks 👀",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = "Written as if they were skim-reading it on their phone.",
                                fontSize = 12.sp,
                                color = SlateText
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF0F172A))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = result.roast.candidateFirstImpression,
                                    fontSize = 13.sp,
                                    color = Color(0xFFE2E8F0),
                                    lineHeight = 19.sp
                                )
                            }
                        }
                    }
                }
            }

            // Biggest red flag
            if (result.roast.biggestRedFlag.isNotBlank()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = SpicyRed.copy(alpha = 0.12f)
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = Brush.horizontalGradient(
                                listOf(SpicyRed.copy(alpha = 0.5f), SpicyRed.copy(alpha = 0.2f))
                            )
                        )
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = SpicyRed,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "What stops people applying 🚩",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = SpicyRed
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = result.roast.biggestRedFlag,
                                    fontSize = 13.sp,
                                    color = Color.White,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }

            // Score
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(
                            listOf(RescueCyan.copy(alpha = 0.6f), RescueTeal.copy(alpha = 0.3f))
                        )
                    )
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Your advert's score",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Out of 100. We work it out from the five things on the right.",
                            fontSize = 12.sp,
                            color = SlateText
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ScoreRadialMeter(
                                score = result.scores.overall,
                                label = result.scores.tierLabel,
                                modifier = Modifier.weight(1.1f)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1.3f)) {
                                CategoryScoreBar(title = "Is it clear?", score = result.scores.clarity)
                                CategoryScoreBar(title = "Is it honest?", score = result.scores.honesty)
                                CategoryScoreBar(title = "Welcoming?", score = result.scores.inclusivity)
                                CategoryScoreBar(title = "Realistic asks?", score = result.scores.realism)
                                CategoryScoreBar(title = "Would you apply?", score = result.scores.candidateAppeal)
                            }
                        }
                    }
                }
            }

            // Issues
            if (result.roast.issues.isNotEmpty()) {
                item {
                    Column {
                        Text(
                            text = "What we found (${result.roast.issues.size})",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Tap any card to see why it matters.",
                            fontSize = 12.sp,
                            color = SlateText
                        )
                    }
                }
                items(result.roast.issues) { issue -> JobAdIssueCard(issue) }
            }

            // Buzzwords
            if (result.roast.buzzwords.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Words that mean nothing",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Every advert says these, so they tell a candidate nothing about you.",
                                fontSize = 12.sp,
                                color = SlateText
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                result.roast.buzzwords.forEach { word ->
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text(word, fontSize = 11.sp, color = FireAmber) },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = FireAmber.copy(alpha = 0.12f)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Unrealistic asks
            if (result.unrealisticAsks.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Asks that nobody can meet ⛔",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = SpicyRed
                            )
                            Text(
                                text = "Good people read these and close the tab.",
                                fontSize = 12.sp,
                                color = SlateText
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            result.unrealisticAsks.forEach { ask ->
                                Text(
                                    text = "• $ask",
                                    fontSize = 13.sp,
                                    color = Color.White,
                                    lineHeight = 18.sp,
                                    modifier = Modifier.padding(vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Missing details
            if (result.missingDetails.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "You forgot to say",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = FireAmber
                            )
                            Text(
                                text = "Things candidates look for and could not find in your advert.",
                                fontSize = 12.sp,
                                color = SlateText
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            result.missingDetails.forEach { detail ->
                                Row(
                                    modifier = Modifier.padding(vertical = 5.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                        contentDescription = null,
                                        tint = FireAmber,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = detail.item,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = detail.whyItMatters,
                                            fontSize = 12.sp,
                                            color = SlateText,
                                            lineHeight = 17.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Top fixes
            if (result.rescue.topFixes.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = Brush.horizontalGradient(listOf(RescueCyan, RescueGreen))
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "🛟", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Fix these three first",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = "Do these and more of the right people will apply.",
                                fontSize = 12.sp,
                                color = SlateText
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            result.rescue.topFixes.forEachIndexed { index, fix ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(RescueTeal),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${index + 1}",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = fix,
                                        fontSize = 13.sp,
                                        color = Color(0xFFE2E8F0),
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Line rewrites
            if (result.rescue.lineRewrites.isNotEmpty()) {
                item {
                    Column {
                        Text(
                            text = "Your lines, rewritten",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Taken from your advert. We never invent a salary or a benefit you did not offer.",
                            fontSize = 12.sp,
                            color = SlateText
                        )
                    }
                }
                items(result.rescue.lineRewrites) { rewrite ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "❌ WHAT YOU WROTE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = SpicyRed
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = rewrite.original,
                                fontSize = 13.sp,
                                color = SlateText,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = BorderDark, thickness = 0.8.dp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "✅ TRY THIS INSTEAD",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = RescueGreen
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = rewrite.improved,
                                fontSize = 13.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 19.sp
                            )
                            if (rewrite.recommendation.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF0F172A))
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = "💡 ${rewrite.recommendation}",
                                        fontSize = 11.sp,
                                        color = RescueCyan,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Rewritten opening, copyable
            if (result.rescue.improvedOpening.isNotBlank()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "A better opening",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Paste this at the top of your advert.",
                                        fontSize = 12.sp,
                                        color = SlateText
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        val clipboard = context
                                            .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(
                                            ClipData.newPlainText(
                                                "Job advert opening",
                                                result.rescue.improvedOpening
                                            )
                                        )
                                        Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy the rewritten opening",
                                        tint = RescueCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF0F172A))
                                    .border(1.dp, RescueTeal.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = result.rescue.improvedOpening,
                                    fontSize = 13.sp,
                                    color = Color.White,
                                    lineHeight = 19.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        Surface(color = SurfaceDark, shadowElevation = 8.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Button(
                    onClick = onStartOver,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FlameOrange)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Roast another advert", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun JobAdIssueCard(issue: JobAdIssue) {
    var expanded by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Column(
            modifier = Modifier
                .clickable { expanded = !expanded }
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text(text = "🔥", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = issue.title,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
                SeverityBadge(severity = issue.severity)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "\"${issue.roast}\"",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = FireAmber,
                lineHeight = 19.sp
            )

            if (expanded && issue.problem.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0F172A))
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = "Why this costs you candidates",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlateText
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = issue.problem,
                            fontSize = 12.sp,
                            color = Color(0xFFCBD5E1),
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }
    }
}

/** Lets tests scroll the results list to a section that is not composed yet. */
const val JOB_AD_LIST_TAG = "jobAdResultsList"
