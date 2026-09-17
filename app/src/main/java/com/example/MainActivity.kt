package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.filled.Tune
import com.example.ui.components.CompareScoreDialog
import com.example.ui.components.ProviderSettingsDialog
import com.example.ui.components.ShareCardDialog
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.JobAdResultsScreen
import com.example.ui.screens.LoadingScreen
import com.example.ui.screens.ResultsScreen
import com.example.ui.theme.DeepCharcoal
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SurfaceDark
import com.example.viewmodel.RoastViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true) {
                MainApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(viewModel: RoastViewModel = viewModel()) {
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val loadingMessage by viewModel.loadingMessage.collectAsState()
    val analysisResult by viewModel.analysisResult.collectAsState()
    val showShareDialog by viewModel.showShareDialog.collectAsState()
    val showCompareDialog by viewModel.showCompareDialog.collectAsState()
    val showProviderSettingsDialog by viewModel.showProviderSettingsDialog.collectAsState()
    val previousScore by viewModel.previousScore.collectAsState()
    val cvText by viewModel.cvText.collectAsState()
    val jobAdResult by viewModel.jobAdResult.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DeepCharcoal,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🔥", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CV Roast & Rescue",
                            fontWeight = FontWeight.Black,
                            fontSize = 19.sp,
                            color = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.setShowProviderSettingsDialog(true) }) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "AI Model Settings",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceDark,
                    titleContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                isAnalyzing -> {
                    LoadingScreen(currentMessage = loadingMessage)
                }
                analysisResult != null -> {
                    ResultsScreen(
                        viewModel = viewModel,
                        result = analysisResult!!
                    )
                }
                jobAdResult != null -> {
                    JobAdResultsScreen(
                        result = jobAdResult!!,
                        onStartOver = { viewModel.resetForNewRoast() }
                    )
                }
                else -> {
                    HomeScreen(viewModel = viewModel)
                }
            }

            if (showProviderSettingsDialog) {
                ProviderSettingsDialog(
                    viewModel = viewModel,
                    onDismiss = { viewModel.setShowProviderSettingsDialog(false) }
                )
            }

            if (showShareDialog && analysisResult != null) {
                ShareCardDialog(
                    result = analysisResult!!,
                    onDismiss = { viewModel.setShowShareDialog(false) }
                )
            }

            if (showCompareDialog && previousScore != null) {
                CompareScoreDialog(
                    previousScore = previousScore!!,
                    currentCvText = cvText,
                    onDismiss = { viewModel.setShowCompareDialog(false) },
                    onSubmitRevisedCv = { revisedText ->
                        viewModel.submitRevisedCv(revisedText)
                    }
                )
            }
        }
    }
}
