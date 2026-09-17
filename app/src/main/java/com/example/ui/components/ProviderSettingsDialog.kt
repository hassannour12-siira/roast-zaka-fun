package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.AiProvider
import com.example.ui.theme.*
import com.example.viewmodel.RoastViewModel

@Composable
fun ProviderSettingsDialog(
    viewModel: RoastViewModel,
    onDismiss: () -> Unit
) {
    val selectedProvider by viewModel.selectedProvider.collectAsState()
    val openAiKey by viewModel.openAiApiKey.collectAsState()
    val claudeKey by viewModel.claudeApiKey.collectAsState()
    val geminiKey by viewModel.geminiApiKey.collectAsState()

    var showKeyVisibility by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = null,
                        tint = RescueCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI Provider & Model",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Select whether to power roasts with OpenAI (ChatGPT), Anthropic (Claude), or Google Gemini.",
                    fontSize = 12.sp,
                    color = SlateText,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Provider Options
                AiProvider.values().forEach { provider ->
                    val isSelected = selectedProvider == provider
                    val activeColor = when (provider) {
                        AiProvider.OPENAI -> Color(0xFF10A37F) // OpenAI Green
                        AiProvider.CLAUDE -> Color(0xFFD97706) // Claude Warm Amber
                        AiProvider.GEMINI -> RescueCyan
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) activeColor.copy(alpha = 0.15f) else DeepCharcoal)
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) activeColor else BorderDark,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { viewModel.onProviderSelected(provider) }
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = provider.displayName,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else SlateText,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(activeColor.copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = provider.badge,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = activeColor
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = provider.description,
                                    fontSize = 11.sp,
                                    color = SlateText
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

                Spacer(modifier = Modifier.height(16.dp))

                // API Key input for the selected provider
                val currentKey = when (selectedProvider) {
                    AiProvider.OPENAI -> openAiKey
                    AiProvider.CLAUDE -> claudeKey
                    AiProvider.GEMINI -> geminiKey
                }

                val currentKeyName = when (selectedProvider) {
                    AiProvider.OPENAI -> "OpenAI API Key (sk-...)"
                    AiProvider.CLAUDE -> "Claude API Key (sk-ant-...)"
                    AiProvider.GEMINI -> "Gemini API Key (AIza...)"
                }

                OutlinedTextField(
                    value = currentKey,
                    onValueChange = { newKey ->
                        when (selectedProvider) {
                            AiProvider.OPENAI -> viewModel.onOpenAiApiKeyChanged(newKey)
                            AiProvider.CLAUDE -> viewModel.onClaudeApiKeyChanged(newKey)
                            AiProvider.GEMINI -> viewModel.onGeminiApiKeyChanged(newKey)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(currentKeyName, fontSize = 12.sp) },
                    placeholder = { Text("Optional: Enter key or use environment key", fontSize = 11.sp, color = SlateText.copy(alpha = 0.6f)) },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = RescueCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { showKeyVisibility = !showKeyVisibility }) {
                            Icon(
                                imageVector = if (showKeyVisibility) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showKeyVisibility) "Hide key" else "Show key",
                                tint = SlateText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    visualTransformation = if (showKeyVisibility) VisualTransformation.None else PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RescueCyan,
                        unfocusedBorderColor = BorderDark,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = DeepCharcoal,
                        unfocusedContainerColor = DeepCharcoal
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "If left empty, the app uses keys from the build configuration, or falls back to our intelligent CV analysis engine.",
                    fontSize = 11.sp,
                    color = SlateText.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FlameOrange)
                ) {
                    Text(
                        text = "Done",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
