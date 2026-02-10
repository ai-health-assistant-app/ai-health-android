package com.ai_health.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onBack: () -> Unit
) {
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
                        Text(
                            "Online",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppTheme.colors.success
                        )
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
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = AppDimensions.space4),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                item {
                    ChatBubble(
                        message = "Ciao Angelo! 👋\nHo analizzato i tuoi dati del sonno. Sembra che tu abbia dormito bene, ma la fase profonda è stata un po' breve.",
                        isUser = false
                    )
                }
                item {
                    ChatBubble(
                        message = "Come posso migliorare la fase profonda?",
                        isUser = true
                    )
                }
                item {
                    ChatBubble(
                        message = "Prova ad evitare schermi luminosi 30 minuti prima di dormire e mantieni la stanza fresca. Vuoi che ti prepari una routine?",
                        isUser = false
                    )
                }
            }

            // Input Area
            ChatInputArea(
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope
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
    animatedVisibilityScope: AnimatedVisibilityScope
) {
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
                Text(
                    text = "Scrivi un messaggio...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppTheme.colors.textTertiary,
                    modifier = Modifier.weight(1f)
                )

                // Bottone Invia (ORA DENTRO LA PILLOLA)
                IconButton(
                    onClick = { /* TODO */ },
                    modifier = Modifier
                        .size(40.dp) // Un po' più piccolo per stare bene dentro
                        .background(AppTheme.colors.accentBlue, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = "Invia",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}