package com.ai_health.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ai_health.ui.theme.AppTheme
import com.ai_health.ui.theme.AppDimensions

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    // Scroll to bottom when new messages arrive
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "AI Assistant",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppTheme.colors.textPrimary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(AppTheme.colors.success, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Online",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppTheme.colors.success
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Torna indietro",
                            tint = AppTheme.colors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppTheme.colors.backgroundPrimary,
                    scrolledContainerColor = AppTheme.colors.backgroundPrimary
                )
            )
        },
        containerColor = AppTheme.colors.backgroundPrimary
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Chat Content
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = AppDimensions.space4),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(uiState.messages) { message ->
                    ChatBubble(
                        message = message.text,
                        isUser = message.isUser
                    )
                }

                if (uiState.isLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                             CircularProgressIndicator(
                                 modifier = Modifier.size(24.dp),
                                 color = AppTheme.colors.accentPurple,
                                 strokeWidth = 2.dp
                             )
                        }
                    }
                }
                
                if (uiState.error != null) {
                     item {
                        Text(
                            text = "Errore: ${uiState.error}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(8.dp)
                        )
                     }
                }
            }

            // Input Area
            ChatInputArea(
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                onSend = { text -> viewModel.sendMessage(text) }
            )
        }
    }
}

@Composable
fun ChatBubble(
    message: String,
    isUser: Boolean
) {
    val bubbleColor = if (isUser) AppTheme.colors.accentBlue else AppTheme.colors.surfacePrimary
    val textColor = if (isUser) Color.White else AppTheme.colors.textPrimary
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                if (!isUser) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(AppTheme.colors.accentPurple, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("AI", style = MaterialTheme.typography.labelSmall, color = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Surface(
                    color = bubbleColor,
                    shape = RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = if (isUser) 20.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 20.dp
                    ),
                    modifier = Modifier.widthIn(max = 280.dp)
                ) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ChatInputArea(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onSend: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    with(sharedTransitionScope) {
        // Contenitore trasparente di base
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppTheme.colors.backgroundPrimary)
                .padding(16.dp)
                .navigationBarsPadding()
        ) {
            // --- LA PILLOLA CHE SI ANIMA ---
            // Ora contiene TUTTO: Testo E Bottone
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp) // Altezza fissa
                    // Shared Element applicato al contenitore Row
                    .sharedElement(
                        rememberSharedContentState(key = "chat_input_area"),
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                    // Stile della pillola
                    .background(AppTheme.colors.surfacePrimary, CircleShape)
                    .padding(start = 20.dp, end = 8.dp), // Padding interno (meno a destra per il bottone)
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Testo Input
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = {
                        Text(
                            text = "Scrivi un messaggio...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppTheme.colors.textTertiary
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )

                // Bottone Invia (ORA DENTRO LA PILLOLA)
                IconButton(
                    onClick = { 
                        if (text.isNotBlank()) {
                            onSend(text) 
                            text = ""
                        }
                    },
                    modifier = Modifier
                        .size(40.dp) // Un po' più piccolo per stare bene dentro
                        .background(if (text.isNotBlank()) AppTheme.colors.accentBlue else AppTheme.colors.surfaceSecondary, CircleShape),
                    enabled = text.isNotBlank()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = "Invia",
                        tint = if (text.isNotBlank()) Color.White else AppTheme.colors.textTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}