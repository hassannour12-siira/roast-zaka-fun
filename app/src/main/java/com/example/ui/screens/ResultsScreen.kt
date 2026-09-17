package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FullAnalysisResult
import com.example.model.RoastObservation
import com.example.util.CvExporter
import com.example.viewmodel.ExportState
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.RoastViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResultsScreen(
    viewModel: RoastViewModel,
    result: FullAnalysisResult,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeTab by viewModel.activeResultTab.collectAsState()
    val previousScore by viewModel.previousScore.collectAsState()
    val exportState by viewModel.exportState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepCharcoal)
    ) {
        // Top Header
        Surface(
            color = SurfaceDark,
            shadowElevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val selectedProvider by viewModel.selectedProvider.collectAsState()

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = result.candidate.name,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = result.intensity.peppers,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val providerColor = when (selectedProvider) {
                                com.example.model.AiProvider.OPENAI -> Color(0xFF10A37F)
                                com.example.model.AiProvider.CLAUDE -> Color(0xFFD97706)
                                com.example.model.AiProvider.GEMINI -> RescueCyan
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(providerColor.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = selectedProvider.badge,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = providerColor
                                )
                            }
                        }
                        Text(
                            text = result.candidate.headline,
                            fontSize = 12.sp,
                            color = SlateText
                        )
                    }

                    FilledTonalIconButton(
                        onClick = { viewModel.setShowShareDialog(true) },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = FlameOrange.copy(alpha = 0.2f),
                            contentColor = FlameOrange
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share Roast Card")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Segmented Tabs: THE ROAST vs THE RESCUE
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F172A))
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (activeTab == 0) FlameOrange else Color.Transparent)
                            .clickable { viewModel.setActiveResultTab(0) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🔥", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "1. THE ROAST",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (activeTab == 0) Color.White else SlateText
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (activeTab == 1) RescueTeal else Color.Transparent)
                            .clickable { viewModel.setActiveResultTab(1) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🛟", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "2. THE FIXES",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (activeTab == 1) Color.White else SlateText
                            )
                        }
                    }
                }
            }
        }

        // Main Tab Content
        Box(modifier = Modifier.weight(1f)) {
            if (activeTab == 0) {
                RoastTabContent(
                    result = result,
                    onSwitchToRescue = { viewModel.setActiveResultTab(1) }
                )
            } else {
                RescueTabContent(
                    result = result,
                    previousScore = previousScore,
                    exportState = exportState,
                    onApplyFixes = { viewModel.applyFixesAndDownload() },
                    onDismissExport = { viewModel.clearExportState() },
                    onImproveAgain = { viewModel.prepareImproveThisOneAgain() }
                )
            }
        }

        // Persistent Bottom Bar
        Surface(
            color = SurfaceDark,
            shadowElevation = 8.dp,
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.horizontalGradient(listOf(BorderDark, BorderDark))
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.resetForNewRoast() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Start over", fontSize = 12.sp)
                }

                Button(
                    onClick = { viewModel.setShowShareDialog(true) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FlameOrange)
                ) {
                    Icon(imageVector = Icons.Default.IosShare, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Share Card", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                FilledTonalButton(
                    onClick = { viewModel.prepareImproveThisOneAgain() },
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = RescueTeal,
                        contentColor = Color.White
                    )
                ) {
                    Icon(imageVector = Icons.Default.Upgrade, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Try a new version", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RoastTabContent(
    result: FullAnalysisResult,
    onSwitchToRescue: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Offline-sample warning. The old build served this content silently whenever a
        // real API call failed, so people could not tell generic copy from their own roast.
        if (result.isOfflineFallback) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = FireAmber.copy(alpha = 0.15f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, FireAmber.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = FireAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "This is the offline demo, not your CV",
                                fontWeight = FontWeight.Bold,
                                color = FireAmber,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "It is example text we wrote in advance. Add an API key in settings for a real analysis.",
                                color = SlateText,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // Opening Roast Card
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
                            text = "Your CV Roast",
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

        // Recruiter's 6-Second Impression
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(listOf(BorderDark, BorderDark))
                )
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
                            text = "What a recruiter thinks in 6 seconds ⏱️",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0F172A))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = result.roast.recruiterFirstImpression,
                            fontSize = 13.sp,
                            color = Color(0xFFE2E8F0),
                            lineHeight = 19.sp
                        )
                    }
                }
            }
        }

        // Biggest Red Flag Alert
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SpicyRed.copy(alpha = 0.12f)),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(listOf(SpicyRed.copy(alpha = 0.5f), SpicyRed.copy(alpha = 0.2f)))
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = SpicyRed,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "The biggest problem 🚩",
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

        // Buzzword Detector
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🚨", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Overused words we found",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "${result.roast.buzzwords.size} found",
                            fontSize = 12.sp,
                            color = FireAmber,
                            fontWeight = FontWeight.Bold
                        )
                    }
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
                                ),
                                border = SuggestionChipDefaults.suggestionChipBorder(
                                    enabled = true,
                                    borderColor = FireAmber.copy(alpha = 0.3f)
                                )
                            )
                        }
                    }
                }
            }
        }

        // Roast Observations Section Header
        item {
            Column {
                Text(
                    text = "What we found (${result.roast.observations.size})",
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

        // 4-6 Roast Observation Cards
        items(result.roast.observations) { obs ->
            RoastObservationItem(obs = obs)
        }

        // Transition CTA to Rescue
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSwitchToRescue() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = RescueTeal.copy(alpha = 0.18f)),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(listOf(RescueCyan, RescueGreen))
                )
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "🛟", fontSize = 32.sp)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Right, that is the funny part over.",
                            fontSize = 13.sp,
                            color = RescueCyan
                        )
                        Text(
                            text = "Now let us fix it ➡️",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = RescueGreen
                    )
                }
            }
        }
    }
}

@Composable
private fun RoastObservationItem(obs: RoastObservation) {
    var expanded by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(listOf(BorderDark, BorderDark))
        )
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
                        text = obs.title,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
                SeverityBadge(severity = obs.severity)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "\"${obs.roast}\"",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = FireAmber,
                lineHeight = 19.sp
            )

            if (expanded) {
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
                            text = "Why this hurts you",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlateText
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = obs.problem,
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RescueTabContent(
    result: FullAnalysisResult,
    previousScore: Int?,
    exportState: ExportState?,
    onApplyFixes: () -> Unit,
    onDismissExport: () -> Unit,
    onImproveAgain: () -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Comparison Banner (If user used "Improve This One Again")
        if (previousScore != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = RescueLightBg.copy(alpha = 0.15f)),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(listOf(RescueGreen, RescueCyan))
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "📈", fontSize = 28.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "YOUR OLD CV vs YOUR NEW ONE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = RescueCyan
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val delta = result.scores.overall - previousScore
                            Text(
                                text = "Before: $previousScore  ➡️  Now: ${result.scores.overall} (${if (delta >= 0) "+$delta pts" else "$delta pts"})",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Overall Profile Strength Score Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(listOf(RescueCyan.copy(alpha = 0.6f), RescueTeal.copy(alpha = 0.3f)))
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Your CV score",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Out of 100. We work it out from the six things on the right.",
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
                            CategoryScoreBar(title = "Is it specific?", score = result.scores.specificity)
                            CategoryScoreBar(title = "Does it show results?", score = result.scores.impact)
                            CategoryScoreBar(title = "Easy to read?", score = result.scores.readability)
                            CategoryScoreBar(title = "Proof for skills", score = result.scores.skillsEvidence)
                            if (result.scores.jobAlignment != null) {
                                CategoryScoreBar(title = "Fits the job?", score = result.scores.jobAlignment)
                            }
                        }
                    }
                }
            }
        }

        // Fix These First (Top 3 highest impact improvements)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.PriorityHigh, contentDescription = null, tint = FireAmber)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Fix these three first 🎯",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "Do these and your CV improves more than anything else on this page.",
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
                                    .background(FlameOrange),
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

        // Apply the fixes to the real document and hand it back.
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
                        Text(text = "📄", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Get your fixed CV",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "We put the rewritten lines back into your own CV and save it as " +
                            "a PDF. Nothing else is changed and nothing is invented.",
                        fontSize = 12.sp,
                        color = SlateText,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    when (exportState) {
                        ExportState.Working -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = RescueCyan
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Rewriting your CV...",
                                    color = SlateText,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        is ExportState.Saved -> {
                            val destination = exportState.destination
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(RescueGreen.copy(alpha = 0.12f))
                                    .border(1.dp, RescueGreen.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text(
                                        text = when (destination) {
                                            is CvExporter.Destination.Downloads ->
                                                "✅ Saved to Downloads"
                                            is CvExporter.Destination.NeedsSharing ->
                                                "✅ Your CV is ready"
                                        },
                                        color = RescueGreen,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = when (destination) {
                                            is CvExporter.Destination.Downloads ->
                                                destination.fileName
                                            is CvExporter.Destination.NeedsSharing ->
                                                destination.fileName
                                        },
                                        color = Color.White,
                                        fontSize = 12.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "${exportState.applied} change" +
                                            (if (exportState.applied == 1) "" else "s") +
                                            " applied to your CV.",
                                        color = SlateText,
                                        fontSize = 12.sp
                                    )
                                    if (exportState.notApplied.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "We could not find ${exportState.notApplied.size} " +
                                                "of the suggested lines in your CV, so those are left " +
                                                "for you to apply by hand.",
                                            color = FireAmber,
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp
                                        )
                                    }

                                    if (destination is CvExporter.Destination.NeedsSharing) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Button(
                                            onClick = {
                                                context.startActivity(
                                                    CvExporter.shareIntent(context, destination.file)
                                                )
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = RescueTeal
                                            )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.IosShare,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Save it", fontSize = 13.sp)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    TextButton(
                                        onClick = onDismissExport,
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("Do it again", color = SlateText, fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        is ExportState.Failed -> {
                            Text(
                                text = exportState.message,
                                color = SpicyRed,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = onApplyFixes,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
                            ) {
                                Text("Try again", fontSize = 13.sp)
                            }
                        }

                        null -> {
                            Button(
                                onClick = onApplyFixes,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = RescueTeal)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Apply fixes & download PDF",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Before ➡️ After Section Header
        item {
            Column {
                Text(
                    text = "Your lines, rewritten",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "We took lines straight from your CV. We never add numbers you did not give us.",
                    fontSize = 12.sp,
                    color = SlateText
                )
            }
        }

        // Bullet Rewrites
        items(result.rescue.bulletRewrites) { rewrite ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(listOf(BorderDark, BorderDark))
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Before
                    Row(verticalAlignment = Alignment.Top) {
                        Text(text = "❌ WHAT YOU WROTE", fontSize = 11.sp, fontWeight = FontWeight.Black, color = SpicyRed)
                    }
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

                    // After
                    Row(verticalAlignment = Alignment.Top) {
                        Text(text = "✅ TRY THIS INSTEAD", fontSize = 11.sp, fontWeight = FontWeight.Black, color = RescueGreen)
                    }
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
                            Row(verticalAlignment = Alignment.Top) {
                                Text(text = "💡 ", fontSize = 12.sp)
                                Text(
                                    text = rewrite.recommendation,
                                    fontSize = 11.sp,
                                    color = RescueCyan
                                )
                            }
                        }
                    }
                }
            }
        }

        // Summary Rescue
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "📝", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Your summary, rewritten",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Rescued Summary", result.rescue.summary.improved))
                                Toast.makeText(context, "Rescued summary copied!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = RescueCyan, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(text = "What you have now", fontSize = 11.sp, color = SlateText, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = result.rescue.summary.original,
                        fontSize = 12.sp,
                        color = SlateText,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(text = "Our version", fontSize = 11.sp, color = RescueGreen, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0F172A))
                            .border(1.dp, RescueTeal.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = result.rescue.summary.improved,
                            fontSize = 13.sp,
                            color = Color.White,
                            lineHeight = 19.sp
                        )
                    }
                }
            }
        }

        // Skills Analysis
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Your skills",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Listing a skill is not the same as showing where you used it.",
                        fontSize = 12.sp,
                        color = SlateText
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(text = "You proved these ✅", fontSize = 12.sp, color = RescueGreen, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        result.skills.demonstrated.forEach {
                            SuggestionChip(
                                onClick = {},
                                label = { Text(it, fontSize = 11.sp, color = RescueGreen) },
                                colors = SuggestionChipDefaults.suggestionChipColors(containerColor = RescueGreen.copy(alpha = 0.12f))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(text = "You listed these but never showed them ⚠️", fontSize = 12.sp, color = FireAmber, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        result.skills.mentionedButNotDemonstrated.forEach {
                            SuggestionChip(
                                onClick = {},
                                label = { Text(it, fontSize = 11.sp, color = FireAmber) },
                                colors = SuggestionChipDefaults.suggestionChipColors(containerColor = FireAmber.copy(alpha = 0.12f))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(text = "Worth saying more about 💡", fontSize = 12.sp, color = RescueCyan, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        result.skills.recommendedEmphasis.forEach {
                            SuggestionChip(
                                onClick = {},
                                label = { Text(it, fontSize = 11.sp, color = RescueCyan) },
                                colors = SuggestionChipDefaults.suggestionChipColors(containerColor = RescueCyan.copy(alpha = 0.12f))
                            )
                        }
                    }
                }
            }
        }

        // Job Match Mode (If target role provided)
        if (result.jobMatch.enabled) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(listOf(RescueCyan, RescueTeal))
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🎯", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "How You Match: ${result.jobTarget.ifBlank { "Target Role" }}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(text = "The job asks for these, and you showed them:", fontSize = 12.sp, color = RescueGreen, fontWeight = FontWeight.Bold)
                        result.jobMatch.strongMatches.forEach { match ->
                            Text(text = "• $match", fontSize = 12.sp, color = Color.White, modifier = Modifier.padding(vertical = 2.dp))
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(text = "The job asks for these, but your CV does not show them:", fontSize = 12.sp, color = SpicyRed, fontWeight = FontWeight.Bold)
                        result.jobMatch.notDemonstrated.forEach { missing ->
                            Text(text = "• $missing", fontSize = 12.sp, color = SlateText, modifier = Modifier.padding(vertical = 2.dp))
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = BorderDark, thickness = 0.8.dp)
                        Spacer(modifier = Modifier.height(10.dp))

                        Text(text = "Keywords", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            text = "Many companies scan CVs with software before a person reads them. " +
                                "It looks for words from the job advert. Only add a word if you have really done it.",
                            fontSize = 12.sp,
                            color = SlateText
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            result.jobMatch.keywordsPresent.forEach { kw ->
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text("✅ $kw", fontSize = 11.sp, color = RescueGreen) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(containerColor = RescueGreen.copy(alpha = 0.1f))
                                )
                            }
                            result.jobMatch.keywordsMissing.forEach { kw ->
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text("⚠️ $kw", fontSize = 11.sp, color = FireAmber) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(containerColor = FireAmber.copy(alpha = 0.1f))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
