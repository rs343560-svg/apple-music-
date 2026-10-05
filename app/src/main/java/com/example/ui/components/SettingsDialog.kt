package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.AppleMusicRed
import com.example.util.PerformanceMode

@Composable
fun SettingsDialog(
    currentApiKey: String,
    geminiStatus: String,
    isAiActive: Boolean,
    performanceMode: PerformanceMode,
    downloadedCount: Int,
    onSaveApiKey: (String, (Boolean, String) -> Unit) -> Unit,
    onClearApiKey: () -> Unit,
    onSetPerformanceMode: (PerformanceMode) -> Unit,
    onDismiss: () -> Unit
) {
    var apiKeyInput by remember { mutableStateOf(currentApiKey) }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isTestingKey by remember { mutableStateOf(false) }
    var testFeedback by remember { mutableStateOf<String?>(null) }
    var isFeedbackSuccess by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1C1C1E),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Settings",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 1: Gemini AI Integration
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF6366F1).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF818CF8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Gemini AI Search Intelligence",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Enables natural-language intent understanding, Hindi/Hinglish spelling corrections (e.g. \"barsaat banjaare\", \"arijit singh romantic\"), mood discovery, and intelligent ranking of real results. Gemini never invents fake songs or URLs.",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Real-time Connection Status Indicator Badge
                val (statusColor, statusBg, statusText) = when {
                    isTestingKey || geminiStatus == "VALIDATING" -> Triple(
                        Color(0xFFFBBF24),
                        Color(0xFFFBBF24).copy(alpha = 0.15f),
                        "Validating connection..."
                    )
                    isAiActive || geminiStatus == "CONNECTED" -> Triple(
                        Color(0xFF10B981),
                        Color(0xFF10B981).copy(alpha = 0.15f),
                        "Connected • Gemini AI Active (gemini-3.5-flash)"
                    )
                    geminiStatus == "ERROR" -> Triple(
                        Color(0xFFEF4444),
                        Color(0xFFEF4444).copy(alpha = 0.15f),
                        "Connection Failed • Invalid Key or Network Error"
                    )
                    else -> Triple(
                        Color.White.copy(alpha = 0.5f),
                        Color.White.copy(alpha = 0.08f),
                        "Not Configured • Standard Search Active"
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusBg)
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = statusText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = statusColor
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // API Key input field
                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = {
                        apiKeyInput = it
                        testFeedback = null
                    },
                    placeholder = {
                        Text(
                            text = "Enter Gemini API Key (e.g. AIzaSy...)",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.35f)
                        )
                    },
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (isPasswordVisible) "Hide" else "Show",
                                tint = Color.White.copy(alpha = 0.5f)
                            )
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF141416),
                        unfocusedContainerColor = Color(0xFF141416),
                        focusedBorderColor = AppleMusicRed,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gemini_key_input")
                )

                if (testFeedback != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isFeedbackSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (isFeedbackSuccess) Color(0xFF10B981) else Color(0xFFEF4444),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = testFeedback!!,
                            fontSize = 12.sp,
                            color = if (isFeedbackSuccess) Color(0xFF10B981) else Color(0xFFEF4444)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            isTestingKey = true
                            testFeedback = null
                            onSaveApiKey(apiKeyInput) { valid, message ->
                                isTestingKey = false
                                testFeedback = message
                                isFeedbackSuccess = valid
                            }
                        },
                        enabled = !isTestingKey,
                        colors = ButtonDefaults.buttonColors(containerColor = AppleMusicRed),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        if (isTestingKey) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Validating...", fontSize = 13.sp)
                        } else {
                            Text("Validate & Save", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (currentApiKey.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                apiKeyInput = ""
                                onClearApiKey()
                                testFeedback = "Key removed. Normal music search is active."
                                isFeedbackSuccess = true
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(0.8f)
                        ) {
                            Text("Remove", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(16.dp))

                // Section 2: Performance Mode
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = AppleMusicRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Hardware Optimization & FPS",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                PerformanceMode.values().forEach { mode ->
                    val isSelected = mode == performanceMode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) AppleMusicRed.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.04f))
                            .clickable { onSetPerformanceMode(mode) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = mode.displayName,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) AppleMusicRed else Color.White
                            )
                            Text(
                                text = mode.description,
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = AppleMusicRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(14.dp))

                // Section 3: Storage & Offline
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DownloadDone,
                        contentDescription = null,
                        tint = Color(0xFF22C55E),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Offline Library",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "$downloadedCount songs saved for offline listening. Downloaded tracks play seamlessly without internet access.",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
