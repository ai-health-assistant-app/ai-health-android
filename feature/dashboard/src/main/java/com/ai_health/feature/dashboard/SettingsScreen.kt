package com.ai_health.feature.dashboard

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.ai_health.ui.components.AppButton
import com.ai_health.ui.components.ButtonVariant
import com.ai_health.ui.theme.AppTheme
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

import android.util.Log

private const val TAG = "SettingsScreenAuth"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val user by viewModel.userProfile.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Local state for form fields
    var name by remember(user) { mutableStateOf(user.name) }
    var weight by remember(user) { mutableStateOf(if (user.weight > 0f) user.weight.toString() else "") }
    var height by remember(user) { mutableStateOf(if (user.height > 0) user.height.toString() else "") }
    var selectedGender by remember(user) { mutableStateOf(user.gender) }

    // Google Sign-In Logic
    fun handleSignIn() {
        Log.d(TAG, "handleSignIn: Button clicked")
        Log.d(TAG, "handleSignIn: Button clicked")
        coroutineScope.launch {
            try {
                Log.d(TAG, "handleSignIn: Building GetGoogleIdOption")
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId("68795703297-67uqr8suijo43ptp0d045i1ja9rb8g3v.apps.googleusercontent.com") // User needs to replace this, currently placeholders usually
                    .setAutoSelectEnabled(true)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                Log.d(TAG, "handleSignIn: Requesting credential from CredentialManager")
                val result = CredentialManager.create(context).getCredential(
                    request = request,
                    context = context
                )

                Log.d(TAG, "handleSignIn: Credential received. Type: ${result.credential.type}")
                val credential = result.credential
                
                if (credential is androidx.credentials.CustomCredential) {
                     Log.d(TAG, "handleSignIn: data keys: ${credential.data.keySet()}")
                }

                try {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken
                    Log.d(TAG, "handleSignIn: ID Token obtained (length: ${idToken.length})")
                    viewModel.signIn(idToken)
                } catch (e: Exception) {
                    Log.e(TAG, "FAILED to parse GoogleIdTokenCredential", e)
                    throw e // Re-throw to hit the outer catch
                }
                
            } catch (e: GetCredentialException) {
                Log.e(TAG, "Credential Manager Error: ${e.message}", e)
                e.printStackTrace()
                Toast.makeText(context, "Errore Credential: ${e.message}", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Log.e(TAG, "Generic Auth Error: ${e.message}", e)
                e.printStackTrace()
                Toast.makeText(context, "Errore Generico: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Impostazioni Profilo") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppTheme.colors.surfacePrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.saveProfile(name, weight, height, selectedGender)
                    onBack()
                },
                containerColor = AppTheme.colors.accentBlue,
                contentColor = Color.White
            ) {
                Icon(Icons.Rounded.Save, "Salva")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            
            // --- Account Section ---
            SettingsSection("Account") {
                if (user.uid.isNotBlank()) {
                    // Logged In View
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (user.photoUrl.isNotBlank()) {
                            AsyncImage(
                                model = user.photoUrl,
                                contentDescription = "Profile Picture",
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, AppTheme.colors.accentPurple, CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .background(AppTheme.colors.surfaceSecondary, CircleShape)
                                    .border(2.dp, AppTheme.colors.accentPurple, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = user.name.firstOrNull()?.uppercase() ?: "?", // Initial
                                    style = MaterialTheme.typography.titleLarge,
                                    color = AppTheme.colors.textPrimary
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = user.name.ifBlank { "Utente" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = AppTheme.colors.textPrimary
                            )
                            if (user.email.isNotBlank()) {
                                Text(
                                    text = user.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppTheme.colors.textSecondary
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedButton(
                        onClick = { viewModel.signOut() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Disconnetti")
                    }
                    
                } else {
                    // Logged Out View
                    Text(
                        text = "Accedi per sincronizzare i tuoi dati salute e ricevere insight personalizzati.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppTheme.colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    AppButton(
                        text = "Accedi con Google",
                        onClick = { handleSignIn() },
                        variant = ButtonVariant.PRIMARY,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // --- Personal Info Section ---
            SettingsSection("Informazioni Personali") {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // --- Biometrics Section ---
            SettingsSection("Biometria") {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { weight = it },
                        label = { Text("Peso (kg)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = height,
                        onValueChange = { height = it },
                        label = { Text("Altezza (cm)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = AppTheme.colors.accentPurple,
            fontWeight = FontWeight.Bold
        )
        content()
    }
}