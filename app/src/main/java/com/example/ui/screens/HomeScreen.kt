package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SampleCVs
import com.example.model.AiProvider
import com.example.model.RoastIntensity
import com.example.ui.components.ProviderSettingsDialog
import com.example.ui.theme.*
import com.example.viewmodel.AppMode
import com.example.viewmodel.InputMethod
import com.example.viewmodel.RoastViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: RoastViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val inputMethod by viewModel.inputMethod.collectAsState()
    val cvText by viewModel.cvText.collectAsState()
    val uploadedFileName by viewModel.uploadedFileName.collectAsState()
    val targetJob by viewModel.targetJob.collectAsState()
    val intensity by viewModel.intensity.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var linkedInUrl by remember { mutableStateOf("") }
    var showLinkedInFallback by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.onFileSelected(it) }
    }

    val selectedProvider by viewModel.selectedProvider.collectAsState()
    val appMode by viewModel.appMode.collectAsState()
    val jobAdText by viewModel.jobAdText.collectAsState()
    val isRecruiterMode = appMode == AppMode.ROAST_JOB_AD

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DeepCharcoal)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Who is using the app. The two sides analyse different documents.
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceDark)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    Triple(AppMode.ROAST_CV, "I have a CV", Icons.Default.Person),
                    Triple(AppMode.ROAST_JOB_AD, "I'm hiring", Icons.Default.BusinessCenter)
                ).forEach { (mode, label, icon) ->
                    val isSelected = appMode == mode
                    val tint = if (mode == AppMode.ROAST_CV) FlameOrange else RescueTeal
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) tint else Color.Transparent)
                            .clickable { viewModel.setAppMode(mode) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else SlateText,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else SlateText
                            )
                        }
                    }
                }
            }
        }

        // Hero Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(
                        listOf(FlameOrange.copy(alpha = 0.6f), FireAmber.copy(alpha = 0.3f))
                    )
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(FlameOrange.copy(alpha = 0.15f))
                            .border(1.dp, FlameOrange.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isRecruiterMode) {
                                "Roast the advert. Fix the advert. Hire better."
                            } else {
                                "Get roasted. Get rescued. Get hired."
                            },
                            color = FlameOrange,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = if (isRecruiterMode) {
                            "Your job advert thinks\nit sounds great. 🔥"
                        } else {
                            "Your CV thinks it's ready.\nLet's test that theory. 🔥"
                        },
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        lineHeight = 30.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isRecruiterMode) {
                            "Paste the job advert you are about to post. We will tell you how it reads to the people you want to hire, then rewrite the worst bits."
                        } else {
                            "Upload your CV or drop your LinkedIn profile. We'll roast it first, then actually help you fix it."
                        },
                        fontSize = 14.sp,
                        color = SlateText,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = RescueCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isRecruiterMode) {
                                "We roast adverts, not companies."
                            } else {
                                "We roast résumés, not people."
                            },
                            fontSize = 12.sp,
                            color = RescueCyan,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // AI Model / Provider Selector Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { viewModel.setShowProviderSettingsDialog(true) },
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val providerColor = when (selectedProvider) {
                        AiProvider.OPENAI -> Color(0xFF10A37F)
                        AiProvider.CLAUDE -> Color(0xFFD97706)
                        AiProvider.GEMINI -> RescueCyan
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(providerColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = providerColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "AI Engine: ${selectedProvider.displayName}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
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
                            text = "Tap to switch between OpenAI, Claude & Gemini or add keys",
                            fontSize = 11.sp,
                            color = SlateText
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Configure AI Provider",
                        tint = SlateText,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Quick Demo Samples (candidate side only)
        if (!isRecruiterMode) item {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = FireAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Instant Demo Samples (1-Tap)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(SampleCVs.samples) { sample ->
                        AssistChip(
                            onClick = {
                                viewModel.loadSample(sample)
                            },
                            label = {
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Text(text = sample.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(text = sample.targetRole, fontSize = 11.sp, color = SlateText)
                                }
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = SurfaceDark,
                                labelColor = Color.White
                            ),
                            border = AssistChipDefaults.assistChipBorder(
                                enabled = true,
                                borderColor = BorderDark
                            )
                        )
                    }
                }
            }
        }

        // Input Method Segmented Selector (candidate side only)
        if (!isRecruiterMode) item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceDark)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    Triple(InputMethod.UPLOAD_FILE, "Upload CV", Icons.Default.UploadFile),
                    Triple(InputMethod.LINKEDIN_PROFILE, "LinkedIn", Icons.Default.Share),
                    Triple(InputMethod.PASTE_TEXT, "Paste Text", Icons.Default.EditNote)
                ).forEach { (method, label, icon) ->
                    val isSelected = inputMethod == method
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) FlameOrange else Color.Transparent)
                            .clickable { viewModel.setInputMethod(method) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else SlateText,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else SlateText
                            )
                        }
                    }
                }
            }
        }

        // The recruiter pastes or uploads one document: the advert.
        if (isRecruiterMode) item {
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
                        Text(
                            text = "Paste your job advert",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Text(text = "${jobAdText.length} chars", color = SlateText, fontSize = 12.sp)
                    }
                    Text(
                        text = "The whole thing: title, responsibilities, requirements, benefits.",
                        color = SlateText,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = jobAdText,
                        onValueChange = { viewModel.onJobAdTextChanged(it) },
                        placeholder = {
                            Text(
                                "Senior Backend Engineer\n\nWe are looking for a rockstar developer to join our fast-paced family...",
                                color = SlateText
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RescueTeal,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            filePickerLauncher.launch(
                                arrayOf(
                                    "application/pdf",
                                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                    "text/plain"
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RescueCyan),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = uploadedFileName ?: "Or upload it as a PDF, DOCX or TXT",
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Method Specific Input Containers (candidate side only)
        if (!isRecruiterMode) item {
            when (inputMethod) {
                InputMethod.UPLOAD_FILE -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(BorderDark, BorderDark)))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (uploadedFileName == null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(2.dp, BorderDark, RoundedCornerShape(12.dp))
                                        .clickable {
                                            filePickerLauncher.launch(
                                                arrayOf(
                                                    "application/pdf",
                                                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                                    "text/plain"
                                                )
                                            )
                                        }
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.CloudUpload,
                                            contentDescription = null,
                                            tint = FlameOrange,
                                            modifier = Modifier.size(44.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "Choose PDF, DOCX, or TXT file",
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 15.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Tap to browse documents",
                                            color = SlateText,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF0F172A))
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = RescueCyan,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = uploadedFileName ?: "File",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "Ready to roast (${cvText.length} characters)",
                                            color = RescueGreen,
                                            fontSize = 12.sp
                                        )
                                    }
                                    IconButton(onClick = { viewModel.removeUploadedFile() }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint = SpicyRed
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                InputMethod.LINKEDIN_PROFILE -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "LinkedIn profile",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = linkedInUrl,
                                onValueChange = {
                                    linkedInUrl = it
                                    showLinkedInFallback = it.contains("linkedin.com", ignoreCase = true)
                                },
                                placeholder = { Text("https://www.linkedin.com/in/username", color = SlateText) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                trailingIcon = {
                                    Button(
                                        onClick = { showLinkedInFallback = true },
                                        contentPadding = PaddingValues(horizontal = 8.dp),
                                        modifier = Modifier.padding(end = 4.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = RescueTeal)
                                    ) {
                                        Text("Next", fontSize = 12.sp)
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = RescueCyan,
                                    unfocusedBorderColor = BorderDark,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            if (showLinkedInFallback || linkedInUrl.isNotBlank()) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MutedAmber.copy(alpha = 0.15f))
                                        .border(1.dp, MutedAmberText.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                        .padding(12.dp)
                                    ) {
                                    Column {
                                        Text(
                                            text = "LinkedIn does not let apps read profiles 🔒",
                                            color = FireAmber,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "So paste your profile text below instead: About, Experience and Skills.",
                                            color = SlateText,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = cvText,
                                    onValueChange = { viewModel.onCvTextChanged(it) },
                                    placeholder = { Text("Paste LinkedIn About, Experience, and Skills sections here...", color = SlateText) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = FlameOrange,
                                        unfocusedBorderColor = BorderDark,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }

                InputMethod.PASTE_TEXT -> {
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
                                Text(
                                    text = "Paste CV / Résumé Text",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "${cvText.length} chars",
                                    color = SlateText,
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = cvText,
                                onValueChange = { viewModel.onCvTextChanged(it) },
                                placeholder = {
                                    Text(
                                        "Paste your entire CV text, including work experience and skills...",
                                        color = SlateText
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = FlameOrange,
                                    unfocusedBorderColor = BorderDark,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }

        // Optional Job Target (candidate side only)
        if (!isRecruiterMode) item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.WorkOutline,
                            contentDescription = null,
                            tint = RescueCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Want better advice? (Optional)",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "What job are you targeting? (Title or pasted job description)",
                        color = SlateText,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = targetJob,
                        onValueChange = { viewModel.onTargetJobChanged(it) },
                        placeholder = { Text("e.g. Senior Backend Engineer, or paste job specs", color = SlateText) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RescueCyan,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            }
        }

        // Roast Intensity Selection
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Choose your roast level",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    RoastIntensity.values().forEach { level ->
                        val isSelected = intensity == level
                        val activeColor = when (level) {
                            RoastIntensity.LIGHT -> RescueCyan
                            RoastIntensity.SPICY -> FlameOrange
                            RoastIntensity.EXTRA_SPICY -> SpicyRed
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) activeColor.copy(alpha = 0.15f) else Color.Transparent)
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) activeColor else BorderDark,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { viewModel.onIntensitySelected(level) }
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = level.peppers,
                                    fontSize = 18.sp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = level.label,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else SlateText,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "\"${level.tagline}\"",
                                        color = if (isSelected) activeColor else SlateText.copy(alpha = 0.8f),
                                        fontSize = 12.sp
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = activeColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Error message if any
        if (errorMessage != null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SpicyRed.copy(alpha = 0.15f))
                        .border(1.dp, SpicyRed.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = SpicyRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = errorMessage ?: "",
                                color = SpicyRed,
                                fontSize = 13.sp
                            )
                            if (!isRecruiterMode) {
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(
                                    onClick = { viewModel.runOfflineSample() },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(
                                        text = "Show the offline demo instead",
                                        color = FireAmber,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Primary Action Button
        item {
            Button(
                onClick = {
                    if (isRecruiterMode) viewModel.startJobAdAnalysis() else viewModel.startAnalysis()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRecruiterMode) RescueTeal else FlameOrange
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isRecruiterMode) "🔥 Roast My Job Advert" else "🔥 Roast My CV",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
        }
    }
}
