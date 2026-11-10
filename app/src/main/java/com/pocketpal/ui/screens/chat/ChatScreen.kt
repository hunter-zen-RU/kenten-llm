package com.pocketpal.ui.screens.chat

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import coil.compose.AsyncImage
import com.pocketpal.data.model.ChatMessage
import com.pocketpal.data.model.Pal
import com.pocketpal.data.screen.ScreenContextManager
import com.pocketpal.data.screen.ScreenContextResult
import com.pocketpal.ui.components.EdgeLightingVisualizer
import com.pocketpal.ui.components.FormattedMarkdownText
import com.pocketpal.ui.components.MessageSkeletonItem
import com.pocketpal.ui.components.PalIcon
import com.pocketpal.ui.screens.voice.VoiceState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onNavigateToPals: () -> Unit,
    onNavigateToModels: () -> Unit,
    onNavigateToBenchmark: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToHandsFreeVoice: () -> Unit,
    onNavigateToServer: () -> Unit,
    onNavigateToVoicePacks: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val screenManager = remember { ScreenContextManager(context) }

    LaunchedEffect(Unit) {
        viewModel.initTts(context)
    }

    // Dynamic Status Bar Visibility when Keyboard is Active
    val isImeVisible = WindowInsets.isImeVisible
    LaunchedEffect(isImeVisible) {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, view)
            if (isImeVisible) {
                insetsController.hide(WindowInsetsCompat.Type.statusBars())
            } else {
                insetsController.show(WindowInsetsCompat.Type.statusBars())
            }
        }
    }

    val messages by viewModel.messages.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val pals by viewModel.pals.collectAsState()
    val models by viewModel.models.collectAsState()
    val activeModelId by viewModel.activeModelId.collectAsState()
    val selectedPal by viewModel.selectedPal.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val temperature by viewModel.temperature.collectAsState()
    val attachedImageUri by viewModel.attachedImageUri.collectAsState()
    val currentSessionId by viewModel.currentSessionId.collectAsState()
    val exportJson by viewModel.exportJson.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var showQuickActions by remember { mutableStateOf(false) }
    var showSessionsSheet by remember { mutableStateOf(false) }
    var showModelSheet by remember { mutableStateOf(false) }
    var showPalSheet by remember { mutableStateOf(false) }
    var showParamsSheet by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var isEdgeLightingActive by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.setAttachedImage(it.toString()) }
    }

    fun handleScreenUnderstanding() {
        scope.launch {
            val activeModel = models.find { it.id == activeModelId }
            val provider = activeModel?.provider ?: "Local"
            val result = screenManager.captureScreenContext(view, activeModelId, provider)

            when (result) {
                is ScreenContextResult.VisionImage -> {
                    viewModel.setAttachedImage(result.imageUri)
                    Toast.makeText(context, "Screen image captured for Vision Model!", Toast.LENGTH_SHORT).show()
                }
                is ScreenContextResult.TextOcr -> {
                    viewModel.sendMessage(result.extractedText)
                    Toast.makeText(context, "Screen text extracted via OCR for Text Model!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.imePadding(),
            topBar = {
                Surface(
                    tonalElevation = 3.dp,
                    shadowElevation = 2.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column {
                        TopAppBar(
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { showSessionsSheet = true }
                                        .padding(vertical = 4.dp, horizontal = 6.dp)
                                        .testTag("session_selector_button")
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            PalIcon(
                                                iconKey = selectedPal?.avatar ?: "bot",
                                                size = 22.dp
                                            )
                                        }
                                    }
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = selectedPal?.name ?: "Kenten",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = "Switch Session",
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Text(
                                            text = models.find { it.id == activeModelId }?.name ?: "Llama 3.2 1B (Offline)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            actions = {
                                // Access Current Screen Button (Vision or OCR)
                                IconButton(
                                    onClick = { handleScreenUnderstanding() },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("screen_aware_ai_button")
                                ) {
                                    Icon(
                                        Icons.Default.Screenshot,
                                        contentDescription = "Screen Aware AI",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(
                                    onClick = onNavigateToHandsFreeVoice,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("hands_free_voice_button")
                                ) {
                                    Icon(
                                        Icons.Default.RecordVoiceOver,
                                        contentDescription = "Hands-Free Voice Mode",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(
                                    onClick = { showPalSheet = true },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("pal_picker_button")
                                ) {
                                    Icon(Icons.Default.People, contentDescription = "Choose Pal")
                                }
                                IconButton(
                                    onClick = { showModelSheet = true },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("model_picker_button")
                                ) {
                                    Icon(Icons.Default.Memory, contentDescription = "Choose Model")
                                }
                                IconButton(
                                    onClick = { showParamsSheet = true },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("chat_params_button")
                                ) {
                                    Icon(Icons.Default.Tune, contentDescription = "Parameters")
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )

                        // Clean Full-Width Security Banner
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFF0F172A)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Strict Offline Mode • Zero Network Leakage",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFF38BDF8),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                AssistChip(
                                    onClick = onNavigateToServer,
                                    label = { Text("Local :8080", style = MaterialTheme.typography.labelSmall) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Dns,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = Color(0xFF818CF8)
                                        )
                                    }
                                )
                            }
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
                // Messages List or Starter Hero Placeholder
                if (messages.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.verticalScroll(rememberScrollState())
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shadowElevation = 6.dp,
                                modifier = Modifier.size(80.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    PalIcon(
                                        iconKey = selectedPal?.avatar ?: "bot",
                                        size = 40.dp
                                    )
                                }
                            }

                            Text(
                                text = "Chat with ${selectedPal?.name ?: "Kenten LLM"}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            Text(
                                text = selectedPal?.description ?: "Your private, on-device AI assistant for coding, writing, reasoning, and voice chat.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )

                            // Action Buttons Row (Hands-Free & Screen Aware)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilledTonalButton(
                                    onClick = onNavigateToHandsFreeVoice,
                                    shape = RoundedCornerShape(24.dp),
                                    modifier = Modifier.height(52.dp)
                                ) {
                                    Icon(Icons.Default.RecordVoiceOver, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Hands-Free Voice")
                                }

                                Button(
                                    onClick = { handleScreenUnderstanding() },
                                    shape = RoundedCornerShape(24.dp),
                                    modifier = Modifier.height(52.dp)
                                ) {
                                    Icon(Icons.Default.Screenshot, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Analyze Screen")
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Quick Starter Prompts
                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val starterPrompts = listOf(
                                    "Explain Jetpack Compose state management",
                                    "Write a creative sci-fi story opening",
                                    "Help me solve a calculus problem step-by-step",
                                    "Benchmark local AI hardware speed"
                                )
                                starterPrompts.forEach { prompt ->
                                    ElevatedCard(
                                        onClick = { viewModel.sendMessage(prompt) },
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Lightbulb,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = prompt,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            ChatMessageBubble(
                                message = msg,
                                onTtsClick = { viewModel.toggleTtsPlayback(msg.content) }
                            )
                        }

                        if (isGenerating && messages.lastOrNull()?.role != "assistant") {
                            item {
                                MessageSkeletonItem()
                            }
                        }
                    }
                }

                // Attached Photo Preview
                if (attachedImageUri != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AsyncImage(
                                    model = attachedImageUri,
                                    contentDescription = "Attached Image",
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Photo attached for visual analysis",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            IconButton(onClick = { viewModel.setAttachedImage(null) }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove Photo")
                            }
                        }
                    }
                }

                // Ultra-Professional AI Input Capsule
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        // Expandable Quick Tools Row
                        AnimatedVisibility(
                            visible = showQuickActions,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilterChip(
                                    selected = attachedImageUri != null,
                                    onClick = { photoPickerLauncher.launch("image/*") },
                                    label = { Text("Attach Photo", style = MaterialTheme.typography.labelSmall) },
                                    leadingIcon = {
                                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                    },
                                    shape = RoundedCornerShape(20.dp)
                                )
                                FilterChip(
                                    selected = false,
                                    onClick = { handleScreenUnderstanding() },
                                    label = { Text("Screen OCR", style = MaterialTheme.typography.labelSmall) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Screenshot, contentDescription = null, modifier = Modifier.size(16.dp))
                                    },
                                    shape = RoundedCornerShape(20.dp)
                                )
                                FilterChip(
                                    selected = false,
                                    onClick = onNavigateToHandsFreeVoice,
                                    label = { Text("Hands-Free", style = MaterialTheme.typography.labelSmall) },
                                    leadingIcon = {
                                        Icon(Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(16.dp))
                                    },
                                    shape = RoundedCornerShape(20.dp)
                                )
                            }
                        }

                        // Unified Pill Input Box
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(26.dp),
                            color = Color(0xFF1E293B),
                            border = BorderStroke(1.dp, Color(0xFF334155))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Toggle Quick Tools (+ / X)
                                IconButton(
                                    onClick = { showQuickActions = !showQuickActions },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .testTag("attach_image_button")
                                ) {
                                    Icon(
                                        imageVector = if (showQuickActions) Icons.Default.Close else Icons.Default.AddCircleOutline,
                                        contentDescription = "Quick Tools",
                                        tint = if (showQuickActions) MaterialTheme.colorScheme.primary else Color(0xFF94A3B8),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                OutlinedTextField(
                                    value = inputText,
                                    onValueChange = { inputText = it },
                                    placeholder = {
                                        Text(
                                            "Message ${selectedPal?.name ?: "Kenten"}...",
                                            color = Color(0xFF64748B),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("chat_input_field"),
                                    maxLines = 4,
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent,
                                        disabledBorderColor = Color.Transparent,
                                        errorBorderColor = Color.Transparent,
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent
                                    )
                                )

                                if (isGenerating) {
                                    IconButton(
                                        onClick = { viewModel.stopGeneration() },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(MaterialTheme.colorScheme.error, CircleShape)
                                            .testTag("stop_generation_button")
                                    ) {
                                        Icon(
                                            Icons.Default.Stop,
                                            contentDescription = "Stop",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                } else if (inputText.isNotBlank() || attachedImageUri != null) {
                                    IconButton(
                                        onClick = {
                                            val textToSend = inputText
                                            inputText = ""
                                            viewModel.sendMessage(textToSend)
                                        },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                                            .testTag("send_message_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowUpward,
                                            contentDescription = "Send",
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                } else {
                                    // Quick Trailing Action Icons (Mic & Screen AI)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        IconButton(
                                            onClick = { handleScreenUnderstanding() },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .testTag("input_screen_ocr_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Screenshot,
                                                contentDescription = "Screen Context",
                                                tint = Color(0xFF94A3B8),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = onNavigateToHandsFreeVoice,
                                            modifier = Modifier
                                                .size(36.dp)
                                                .testTag("input_bar_hands_free_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Mic,
                                                contentDescription = "Hands-Free Voice",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Edge Lighting Overlay Visualizer
        if (isGenerating || isEdgeLightingActive) {
            EdgeLightingVisualizer(
                voiceState = if (isGenerating) VoiceState.PROCESSING else VoiceState.IDLE,
                isActive = true
            )
        }
    }

    // Sessions Modal Sheet with Export & Import SQLite Buttons
    if (showSessionsSheet) {
        ModalBottomSheet(onDismissRequest = { showSessionsSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Chat Sessions (SQLite)",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Button(
                        onClick = {
                            viewModel.createNewSession()
                            showSessionsSheet = false
                        },
                        modifier = Modifier.testTag("new_chat_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("New Chat")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.exportChatHistory() },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_chat_history_button")
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export JSON")
                    }
                    OutlinedButton(
                        onClick = { showImportDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("import_chat_history_button")
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Import JSON")
                    }
                }

                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(sessions) { session ->
                        ElevatedCard(
                            onClick = {
                                viewModel.selectSession(session.id)
                                showSessionsSheet = false
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = session.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.deleteSession(session.id) },
                                    modifier = Modifier.testTag("delete_session_button")
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Export Dialog
    if (exportJson != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearExportJson() },
            title = { Text("Exported Chat History (JSON)", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Copy or backup your SQLite conversation history:")
                    OutlinedTextField(
                        value = exportJson ?: "",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("ChatExport", exportJson)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Exported chat history copied to clipboard!", Toast.LENGTH_SHORT).show()
                    viewModel.clearExportJson()
                }) {
                    Text("Copy to Clipboard")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.clearExportJson() }) {
                    Text("Close")
                }
            }
        )
    }

    // Import Dialog
    if (showImportDialog) {
        var importInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Chat History (JSON)", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Paste exported JSON backup to restore into SQLite:")
                    OutlinedTextField(
                        value = importInput,
                        onValueChange = { importInput = it },
                        placeholder = { Text("Paste JSON here...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (importInput.isNotBlank()) {
                        viewModel.importChatHistory(importInput)
                        Toast.makeText(context, "Chat history imported into SQLite!", Toast.LENGTH_SHORT).show()
                        showImportDialog = false
                    }
                }) {
                    Text("Import Backup")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Pal Picker Sheet
    if (showPalSheet) {
        ModalBottomSheet(onDismissRequest = { showPalSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Persona (Pal)",
                        style = MaterialTheme.typography.titleLarge
                    )
                    TextButton(onClick = {
                        showPalSheet = false
                        onNavigateToPals()
                    }) {
                        Text("Manage Pals")
                    }
                }

                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(pals) { pal ->
                        ElevatedCard(
                            onClick = {
                                viewModel.selectPal(pal)
                                showPalSheet = false
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        PalIcon(iconKey = pal.avatar, size = 24.dp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(pal.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        pal.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Model Picker Sheet
    if (showModelSheet) {
        ModalBottomSheet(onDismissRequest = { showModelSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Active AI Model",
                        style = MaterialTheme.typography.titleLarge
                    )
                    TextButton(onClick = {
                        showModelSheet = false
                        onNavigateToModels()
                    }) {
                        Text("Model Catalog")
                    }
                }

                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(models) { model ->
                        ElevatedCard(
                            onClick = {
                                viewModel.selectModel(model.id)
                                showModelSheet = false
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(model.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text(model.provider, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    model.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Parameters Sheet
    if (showParamsSheet) {
        ModalBottomSheet(onDismissRequest = { showParamsSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = "Generation Settings",
                    style = MaterialTheme.typography.titleLarge
                )

                Column {
                    Text(
                        text = "Temperature: ${String.format("%.2f", temperature)}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Slider(
                        value = temperature,
                        onValueChange = { viewModel.setTemperature(it) },
                        valueRange = 0.0f..1.5f
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = {
                            viewModel.clearChatHistory()
                            showParamsSheet = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Clear History")
                    }
                    Button(onClick = { showParamsSheet = false }) {
                        Text("Done")
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(
    message: ChatMessage,
    onTtsClick: () -> Unit
) {
    val isUser = message.role == "user"

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 340.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 20.dp,
                    topEnd = 20.dp,
                    bottomStart = if (isUser) 20.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 20.dp
                ),
                color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (message.imageUri != null) {
                        AsyncImage(
                            model = message.imageUri,
                            contentDescription = "User Attachment",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .padding(bottom = 12.dp)
                        )
                    }

                    if (isUser) {
                        Text(
                            text = message.content,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        FormattedMarkdownText(
                            text = message.content,
                            textColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Stats badge row for Assistant messages
            if (!isUser && message.content.isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(top = 6.dp, start = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Speed,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${message.speedTps} t/s",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${message.thinkingTimeMs}ms",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onTtsClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.VolumeUp,
                            contentDescription = "Speak",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
