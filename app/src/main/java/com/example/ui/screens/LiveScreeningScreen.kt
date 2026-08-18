package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneForwarded
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CallCategory
import com.example.data.model.DialogueMessage
import com.example.data.model.ScreeningPresetScenario
import com.example.data.model.SpeakerType
import com.example.ui.components.LiveAudioVisualizer
import com.example.ui.components.PlaybackWaveform
import com.example.ui.components.rememberDuration
import com.example.ui.components.rememberFormattedTime
import com.example.ui.viewmodel.AssistantViewModel
import com.example.ui.viewmodel.ScreeningCallState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveScreeningScreen(
    viewModel: AssistantViewModel,
    onNavigateBack: () -> Unit,
    onViewHistory: () -> Unit,
    activeScenario: ScreeningPresetScenario? = null
) {
    val callState by viewModel.callState.collectAsState()
    val callerName by viewModel.activeCallerName.collectAsState()
    val phoneNumber by viewModel.activePhoneNumber.collectAsState()
    val organization by viewModel.activeOrganization.collectAsState()
    val category by viewModel.activeCategory.collectAsState()
    val language by viewModel.activeLanguage.collectAsState()
    val conversation by viewModel.conversation.collectAsState()
    val durationSeconds by viewModel.callDurationSeconds.collectAsState()
    val isAiThinking by viewModel.isAiThinking.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val latestSummary by viewModel.latestSummaryResult.collectAsState()

    var callerInputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll when new message appears
    LaunchedEffect(conversation.size) {
        if (conversation.isNotEmpty()) {
            listState.animateScrollToItem(conversation.size - 1)
        }
    }

    when (callState) {
        ScreeningCallState.IDLE -> {
            // Idle screen - Select Scenario or Trigger Custom Call
            ScenarioPickerView(
                scenarios = viewModel.presetScenarios,
                onSelectScenario = { scenario ->
                    viewModel.triggerIncomingCall(
                        scenario = scenario,
                        initialSpeech = scenario.initialCallerSpeech
                    )
                },
                onCustomCall = { name, num, org, speech ->
                    viewModel.triggerIncomingCall(
                        customName = name,
                        customNumber = num,
                        customOrg = org,
                        initialSpeech = speech
                    )
                },
                onBack = onNavigateBack
            )
        }

        ScreeningCallState.RINGING -> {
            // Incoming Call Ringing Screen
            IncomingCallRingingView(
                callerName = callerName,
                phoneNumber = phoneNumber,
                organization = organization,
                initialLanguage = language.displayName,
                onScreenWithAi = {
                    val initialSpeech = activeScenario?.initialCallerSpeech
                        ?: "Hello, calling for Chandan."
                    viewModel.acceptAndScreenWithAi(initialSpeech)
                },
                onAnswerAsChandan = {
                    viewModel.acceptAndScreenWithAi(null)
                    viewModel.handoffToChandan()
                },
                onDecline = {
                    viewModel.dismissCallSimulator()
                }
            )
        }

        ScreeningCallState.ACTIVE_SCREENING, ScreeningCallState.HANDED_OFF_TO_CHANDAN -> {
            // Live Screening Dialogue View
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = callerName,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = if (callState == ScreeningCallState.HANDED_OFF_TO_CHANDAN)
                                        "Connected with Chandan • ${rememberDuration(durationSeconds)}"
                                    else
                                        "Chandan AI Screening Live • ${rememberDuration(durationSeconds)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (callState == ScreeningCallState.HANDED_OFF_TO_CHANDAN)
                                        MaterialTheme.colorScheme.tertiary
                                    else
                                        MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { viewModel.endCallAndGenerateSummary() }) {
                                Icon(Icons.Default.CallEnd, contentDescription = "End Call", tint = MaterialTheme.colorScheme.error)
                            }
                        },
                        actions = {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = language.displayName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                },
                bottomBar = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .imePadding()
                    ) {
                        // Quick Follow-up Chips for Caller
                        val quickReplies = activeScenario?.followUpScript ?: listOf(
                            "Yes, this is regarding an urgent delivery.",
                            "Sir, do you need a personal loan?",
                            "Can you please connect me with Chandan?"
                        )

                        Text(
                            text = "Simulate Caller Reply:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(quickReplies) { reply ->
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        viewModel.sendCallerSpeech(reply)
                                    },
                                    label = {
                                        Text(
                                            text = reply,
                                            maxLines = 1,
                                            fontSize = 12.sp
                                        )
                                    },
                                    modifier = Modifier.testTag("quick_reply_chip")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Custom Caller Input Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = callerInputText,
                                onValueChange = { callerInputText = it },
                                placeholder = {
                                    Text(
                                        text = if (callState == ScreeningCallState.HANDED_OFF_TO_CHANDAN)
                                            "Speak as Chandan..."
                                        else
                                            "Type what caller says (EN or KN)...",
                                        fontSize = 13.sp
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("caller_input_field"),
                                singleLine = true,
                                shape = RoundedCornerShape(24.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                )
                            )

                            IconButton(
                                onClick = {
                                    if (callerInputText.isNotBlank()) {
                                        if (callState == ScreeningCallState.HANDED_OFF_TO_CHANDAN) {
                                            viewModel.sendChandanSpeech(callerInputText)
                                        } else {
                                            viewModel.sendCallerSpeech(callerInputText)
                                        }
                                        callerInputText = ""
                                    }
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .testTag("send_caller_speech_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Send Speech",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Call Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (callState == ScreeningCallState.ACTIVE_SCREENING) {
                                OutlinedButton(
                                    onClick = { viewModel.handoffToChandan() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("take_over_call_button"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhoneForwarded,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Connect to Chandan", fontSize = 12.sp)
                                }
                            }

                            Button(
                                onClick = { viewModel.endCallAndGenerateSummary() },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("end_call_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CallEnd,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("End & Summarize", fontSize = 12.sp)
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    // AI Status & Waveform Header Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (isAiThinking) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Screening response...",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else if (isSpeaking) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.secondary)
                                    )
                                    Text(
                                        text = "Chandan AI Assistant Speaking...",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Listening to caller • ${category.displayName.substringBefore(" /")}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            LiveAudioVisualizer(
                                isSpeaking = isSpeaking || isAiThinking,
                                activeColor = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Conversation Transcript Stream
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(conversation) { msg ->
                            DialogueBubble(message = msg)
                        }
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }

        ScreeningCallState.CALL_COMPLETED -> {
            // Post-Call Summary Screen
            latestSummary?.let { summary ->
                PostCallSummaryView(
                    summary = summary,
                    callerName = callerName,
                    phoneNumber = phoneNumber,
                    durationSeconds = durationSeconds,
                    conversation = conversation,
                    onViewHistory = onViewHistory,
                    onTestAnother = { viewModel.dismissCallSimulator() }
                )
            } ?: run {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
fun DialogueBubble(message: DialogueMessage) {
    val isAi = message.sender == SpeakerType.AI_ASSISTANT
    val isChandan = message.sender == SpeakerType.CHANDAN

    val alignment = if (isAi || isChandan) Alignment.End else Alignment.Start
    val bgGradient = when {
        isChandan -> Brush.linearGradient(listOf(Color(0xFF059669), Color(0xFF10B981)))
        isAi -> Brush.linearGradient(listOf(Color(0xFF4F46E5), Color(0xFF6366F1)))
        else -> Brush.linearGradient(listOf(Color(0xFF334155), Color(0xFF475569)))
    }

    val senderLabel = when (message.sender) {
        SpeakerType.AI_ASSISTANT -> "Chandan AI Assistant"
        SpeakerType.CHANDAN -> "Chandan (Live)"
        SpeakerType.CALLER -> "Caller"
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Text(
            text = senderLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )

        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isAi || isChandan) 16.dp else 4.dp,
                bottomEnd = if (isAi || isChandan) 4.dp else 16.dp
            ),
            color = Color.Transparent,
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isAi || isChandan) 16.dp else 4.dp,
                        bottomEnd = if (isAi || isChandan) 4.dp else 16.dp
                    )
                )
                .background(bgGradient)
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White,
                    lineHeight = 20.sp
                ),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
fun ScenarioPickerView(
    scenarios: List<ScreeningPresetScenario>,
    onSelectScenario: (ScreeningPresetScenario) -> Unit,
    onCustomCall: (String, String, String, String) -> Unit,
    onBack: () -> Unit
) {
    var customName by remember { mutableStateOf("") }
    var customNumber by remember { mutableStateOf("+91 ") }
    var customOrg by remember { mutableStateOf("") }
    var customSpeech by remember { mutableStateOf("") }
    var showCustomDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "AI Call Screening Simulator",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Test screening scenarios in English and Kannada",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Column {
                            Text(
                                text = "Screening for Chandan",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Select an incoming caller scenario below to launch the live screening interface with English & Kannada voice synthesis.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Preset Incoming Scenarios",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            items(scenarios) { scenario ->
                val (badgeColor, badgeTextColor, icon) = when (scenario.category) {
                    CallCategory.SPAM -> Triple(Color(0xFFFEE2E2), Color(0xFFDC2626), Icons.Default.Block)
                    CallCategory.DELIVERY -> Triple(Color(0xFFD1FAE5), Color(0xFF059669), Icons.Default.DeliveryDining)
                    CallCategory.PERSONAL -> Triple(Color(0xFFEDE9FE), Color(0xFF7C3AED), Icons.Default.Person)
                    CallCategory.WORK -> Triple(Color(0xFFDBEAFE), Color(0xFF2563EB), Icons.Default.Work)
                    CallCategory.URGENT -> Triple(Color(0xFFFEF3C7), Color(0xFFD97706), Icons.Default.Emergency)
                    CallCategory.UNKNOWN -> Triple(Color(0xFFF1F5F9), Color(0xFF475569), Icons.Default.Call)
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("scenario_card_${scenario.title}"),
                    onClick = { onSelectScenario(scenario) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(badgeColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = badgeTextColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = scenario.title,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "${scenario.callerName} • ${scenario.organization}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "${scenario.initialLanguage.displayName} (${scenario.initialLanguage.nativeName})",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Start Call",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showCustomDialog = !showCustomDialog },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("toggle_custom_caller_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Person, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (showCustomDialog) "Hide Custom Caller" else "+ Simulate Custom Caller")
                }
            }

            if (showCustomDialog) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Custom Caller Details",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            OutlinedTextField(
                                value = customName,
                                onValueChange = { customName = it },
                                label = { Text("Caller Name") },
                                placeholder = { Text("e.g. Swiggy Agent / Manager") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = customOrg,
                                onValueChange = { customOrg = it },
                                label = { Text("Organization / Company") },
                                placeholder = { Text("e.g. Swiggy / ABC Tech") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = customSpeech,
                                onValueChange = { customSpeech = it },
                                label = { Text("Initial Caller Speech (EN or KN)") },
                                placeholder = { Text("e.g. Sir package ತಂದಿದ್ದೇನೆ / Hi regarding meeting") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = {
                                    onCustomCall(
                                        customName.ifBlank { "Custom Caller" },
                                        customNumber.ifBlank { "+91 99887 76655" },
                                        customOrg.ifBlank { "Direct Call" },
                                        customSpeech
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("start_custom_call_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Launch Incoming Call")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IncomingCallRingingView(
    callerName: String,
    phoneNumber: String,
    organization: String,
    initialLanguage: String,
    onScreenWithAi: () -> Unit,
    onAnswerAsChandan: () -> Unit,
    onDecline: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E1B4B),
                        Color(0xFF312E81)
                    )
                )
            )
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Caller Info Top
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    color = Color(0x3338BDF8),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Incoming Call • Chandan AI Ready",
                            color = Color(0xFF38BDF8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B))
                        .border(3.dp, Color(0xFF38BDF8), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(56.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = callerName,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = organization,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color(0xFF94A3B8)
                    ),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = phoneNumber,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF64748B)
                    ),
                    textAlign = TextAlign.Center
                )
            }

            // Central AI Secretary Prompt Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0x22FFFFFF)
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Turn on AI Screening to find out who is calling and why before answering.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFFE2E8F0),
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }

            // Bottom Actions: Screen with AI (Primary), Answer Directly, Decline
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Large Screen with AI Assistant Button
                Button(
                    onClick = onScreenWithAi,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("screen_with_ai_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4F46E5)
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Screen with Chandan AI Assistant",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Decline
                    IconButton(
                        onClick = onDecline,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444))
                            .testTag("decline_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "Decline Call",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Answer as Chandan
                    IconButton(
                        onClick = onAnswerAsChandan,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                            .testTag("answer_as_chandan_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Answer Directly",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PostCallSummaryView(
    summary: com.example.data.model.ScreeningResult,
    callerName: String,
    phoneNumber: String,
    durationSeconds: Int,
    conversation: List<DialogueMessage>,
    onViewHistory: () -> Unit,
    onTestAnother: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var playbackProgress by remember { mutableStateOf(0.4f) }

    val (badgeColor, badgeTextColor, icon) = when (summary.category) {
        CallCategory.SPAM -> Triple(Color(0xFFFEE2E2), Color(0xFFDC2626), Icons.Default.Block)
        CallCategory.DELIVERY -> Triple(Color(0xFFD1FAE5), Color(0xFF059669), Icons.Default.DeliveryDining)
        CallCategory.PERSONAL -> Triple(Color(0xFFEDE9FE), Color(0xFF7C3AED), Icons.Default.Person)
        CallCategory.WORK -> Triple(Color(0xFFDBEAFE), Color(0xFF2563EB), Icons.Default.Work)
        CallCategory.URGENT -> Triple(Color(0xFFFEF3C7), Color(0xFFD97706), Icons.Default.Emergency)
        CallCategory.UNKNOWN -> Triple(Color(0xFFF1F5F9), Color(0xFF475569), Icons.Default.Call)
    }

    Scaffold(
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onTestAnother,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("test_another_call_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Simulate Next")
                    }

                    Button(
                        onClick = onViewHistory,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("view_in_history_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("View in History")
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Call Completed Badge
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            color = Color(0xFFD1FAE5),
                            shape = CircleShape,
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF059669),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Call Completed & Screened",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Processed by Chandan AI Assistant",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Category & Language Pills
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = badgeColor,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = badgeTextColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = summary.category.displayName,
                                        color = badgeTextColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${summary.detectedLanguage.displayName} (${summary.detectedLanguage.nativeName})",
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Caller Details Table
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DetailRow(label = "Caller Name", value = callerName)
                        DetailRow(label = "Organization", value = summary.organization)
                        DetailRow(label = "Phone Number", value = phoneNumber)
                        DetailRow(label = "Call Duration", value = rememberDuration(durationSeconds))
                        DetailRow(label = "Detected Language", value = summary.detectedLanguage.displayName)
                    }
                }
            }

            // AI Generated Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "AI Call Summary",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = summary.summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Important Information Noted:",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = summary.importantDetails,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Audio Player
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Mic,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Audio Recording",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Text(
                                text = rememberDuration(durationSeconds),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        PlaybackWaveform(
                            progress = playbackProgress,
                            onSeek = { playbackProgress = it }
                        )
                    }
                }
            }

            // Full Transcript Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Complete Transcribed Conversation",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            IconButton(
                                onClick = {
                                    val fullTranscript = conversation.joinToString("\n") {
                                        "${it.sender.name}: ${it.text}"
                                    }
                                    clipboardManager.setText(AnnotatedString(fullTranscript))
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Transcript",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        conversation.forEach { msg ->
                            val isAi = msg.sender == SpeakerType.AI_ASSISTANT
                            val isChandan = msg.sender == SpeakerType.CHANDAN
                            val tagColor = when {
                                isChandan -> Color(0xFF059669)
                                isAi -> Color(0xFF4F46E5)
                                else -> Color(0xFF475569)
                            }
                            val tagText = when (msg.sender) {
                                SpeakerType.AI_ASSISTANT -> "AI Assistant"
                                SpeakerType.CHANDAN -> "Chandan"
                                SpeakerType.CALLER -> "Caller"
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Surface(
                                    color = tagColor.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = tagText,
                                        color = tagColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = msg.text,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
