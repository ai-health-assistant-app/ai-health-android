package com.ai_health.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ai_health.ui.theme.AppTheme
import com.ai_health.ui.theme.AppDimensions

/**
 * Chat content — WhatsApp/Telegram style layout:
 *  - Header: pinned at top, never moves
 *  - Messages: fill middle, gravity to bottom
 *  - Input: sits above keyboard via imePadding()
 */
@Composable
fun ChatSheetContent(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier,
    autoFocus: Boolean = false,
    onInputFocused: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    // Track previous message count to only animate on NEW messages
    val previousMessageCount = remember { mutableIntStateOf(uiState.messages.size) }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            if (uiState.messages.size > previousMessageCount.intValue) {
                listState.animateScrollToItem(uiState.messages.size - 1)
            } else {
                listState.scrollToItem(uiState.messages.size - 1)
            }
            previousMessageCount.intValue = uiState.messages.size
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        // ── HEADER: always pinned at top ──────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = AppTheme.colors.backgroundPrimary,
            tonalElevation = 2.dp,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(AppTheme.colors.accentPurple, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "AI Health Coach",
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

                IconButton(
                    onClick = { viewModel.clearChat() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Nuova chat",
                        tint = AppTheme.colors.textTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // ── MESSAGES: fill remaining space, gravity to bottom ─────
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = AppDimensions.space4),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Bottom),
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

        // ── INPUT: above keyboard ────────────────────────────────
        ChatInputBar(
            onSend = { text -> viewModel.sendMessage(text) },
            onFocusGained = onInputFocused,
            autoFocus = autoFocus
        )
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

/**
 * Chat input bar — sits above keyboard via imePadding applied externally.
 */
@Composable
fun ChatInputBar(
    onSend: (String) -> Unit,
    onFocusGained: () -> Unit = {},
    modifier: Modifier = Modifier,
    autoFocus: Boolean = false,
    key: Any? = null
) {
    var text by remember { mutableStateOf("") }
    var hasFiredFocus by remember(key) { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    // Auto-focus when entering fullscreen
    LaunchedEffect(autoFocus) {
        if (autoFocus) {
            focusRequester.requestFocus()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.ime)
            .background(AppTheme.colors.backgroundPrimary)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(AppTheme.colors.surfacePrimary, CircleShape)
                .padding(start = 20.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused && !hasFiredFocus) {
                            hasFiredFocus = true
                            onFocusGained()
                        }
                        if (!focusState.isFocused) {
                            hasFiredFocus = false
                        }
                    },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true
            )

            IconButton(
                onClick = {
                    if (text.isNotBlank()) {
                        onSend(text)
                        text = ""
                    }
                },
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (text.isNotBlank()) AppTheme.colors.accentBlue else AppTheme.colors.surfaceSecondary,
                        CircleShape
                    ),
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