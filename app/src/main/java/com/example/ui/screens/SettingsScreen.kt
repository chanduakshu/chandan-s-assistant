package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.AssistantViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AssistantViewModel,
    onBack: () -> Unit
) {
    val deliveryEn by viewModel.deliveryInstructionEn.collectAsState()
    val deliveryKn by viewModel.deliveryInstructionKn.collectAsState()
    val autoBlockSpam by viewModel.autoBlockSpam.collectAsState()
    val alertEmergency by viewModel.alertEmergency.collectAsState()
    val saveTranscripts by viewModel.saveTranscripts.collectAsState()
    val saveAudio by viewModel.saveAudio.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Secretary Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Identity Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Chandan AI Assistant",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Personal Secretary for Chandan",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Status: Active & Screening",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Language & Voice
            item {
                SettingsSection(
                    title = "Bilingual Voice Engine",
                    icon = Icons.Default.Translate
                ) {
                    Text(
                        text = "Automatic Language Switching",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "The AI Assistant automatically detects whether the caller speaks English, Kannada, or Kannada-English mixed, and responds concisely (< 15 words) in the matching language.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Delivery Configuration Section
            item {
                SettingsSection(
                    title = "Delivery & Courier Drop-off Rules",
                    icon = Icons.Default.DeliveryDining
                ) {
                    Text(
                        text = "Custom instructions provided to delivery partners (Zomato, Swiggy, Amazon, Flipkart, BlueDart, DTDC):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = deliveryEn,
                        onValueChange = { viewModel.deliveryInstructionEn.value = it },
                        label = { Text("English Delivery Instruction") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("delivery_en_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = deliveryKn,
                        onValueChange = { viewModel.deliveryInstructionKn.value = it },
                        label = { Text("Kannada Delivery Instruction (ಕನ್ನಡ)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("delivery_kn_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Spam & Security Section
            item {
                SettingsSection(
                    title = "Spam & Robocall Filtering",
                    icon = Icons.Default.Block
                ) {
                    SettingToggleRow(
                        title = "Auto-terminate Sales & Telemarketing",
                        subtitle = "Politely decline loan offers, credit cards, real estate, and promotional calls immediately.",
                        checked = autoBlockSpam,
                        onCheckedChange = { viewModel.autoBlockSpam.value = it }
                    )

                    SettingToggleRow(
                        title = "Emergency & Hospital Priority Bypass",
                        subtitle = "Never mark medical or family emergencies as spam; flag for instant alert.",
                        checked = alertEmergency,
                        onCheckedChange = { viewModel.alertEmergency.value = it }
                    )
                }
            }

            // Privacy & Data Retention Section
            item {
                SettingsSection(
                    title = "Privacy & Data Retention",
                    icon = Icons.Default.Lock
                ) {
                    SettingToggleRow(
                        title = "Save Call Transcripts",
                        subtitle = "Store text transcript after screening is completed.",
                        checked = saveTranscripts,
                        onCheckedChange = { viewModel.saveTranscripts.value = it }
                    )

                    SettingToggleRow(
                        title = "Save Audio Recordings",
                        subtitle = "Allow audio playback of screened calls.",
                        checked = saveAudio,
                        onCheckedChange = { viewModel.saveAudio.value = it }
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Privacy Guarantee: Chandan's private addresses, OTPs, financial details, and passwords are never shared with incoming callers under any circumstances.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            content()
        }
    }
}

@Composable
fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
