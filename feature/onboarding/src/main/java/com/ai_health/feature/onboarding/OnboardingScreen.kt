package com.ai_health.feature.onboarding

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.ai_health.core.health.HealthConnectManager
import com.ai_health.ui.theme.AppTheme
import com.ai_health.ui.theme.AppDimensions
import com.ai_health.ui.components.AppButton
import com.ai_health.ui.components.ButtonVariant
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Check if onboarding is already complete
    LaunchedEffect(Unit) {
        if (viewModel.isOnboardingComplete()) {
            onOnboardingComplete()
            return@LaunchedEffect
        }
        viewModel.checkPermissions()
    }

    // Re-check permissions when returning from settings
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.checkPermissions()
    }

    val pagerState = rememberPagerState(pageCount = { uiState.steps.size })

    // Sync pager state with viewModel
    LaunchedEffect(pagerState.currentPage) {
        viewModel.goToStep(pagerState.currentPage)
    }

    // Runtime permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.checkPermissions()
        if (isGranted) {
            scope.launch {
                if (pagerState.currentPage < uiState.steps.size - 1) {
                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                }
            }
        }
    }

    // Health Connect permission launcher
    val healthConnectLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { grantedPermissions ->
        viewModel.checkPermissions()
        if (grantedPermissions.isNotEmpty()) {
            scope.launch {
                if (pagerState.currentPage < uiState.steps.size - 1) {
                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                }
            }
        }
    }

    Scaffold(
        containerColor = AppTheme.colors.backgroundPrimary,
        topBar = {
            OnboardingTopBar(
                currentStep = uiState.currentStep,
                totalSteps = uiState.steps.size,
                backgroundColor = AppTheme.colors.surfacePrimary,
                textColor = AppTheme.colors.textPrimary,
                onBackClick = {
                    scope.launch {
                        if (pagerState.currentPage > 0) {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    }
                }
            )
        },
        bottomBar = {
            OnboardingBottomBar(
                currentStep = uiState.currentStep,
                totalSteps = uiState.steps.size,
                permissionGranted = uiState.permissionStatuses[uiState.steps[uiState.currentStep]] == PermissionStatus.Granted,
                backgroundColor = AppTheme.colors.surfacePrimary,
                accentColor = AppTheme.colors.accentBlue,
                onNextClick = {
                    scope.launch {
                        if (pagerState.currentPage < uiState.steps.size - 1) {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        } else {
                            viewModel.completeOnboarding()
                            onOnboardingComplete()
                        }
                    }
                },
                onFinishClick = {
                    viewModel.completeOnboarding()
                    onOnboardingComplete()
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppTheme.colors.backgroundPrimary)
                .padding(paddingValues)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                val step = uiState.steps[page]
                val status = uiState.permissionStatuses[step]

                PermissionPage(
                    step = step,
                    permissionStatus = status,
                    onGrantClick = {
                        handlePermissionRequest(
                            step = step,
                            context = context,
                            permissionLauncher = permissionLauncher,
                            healthConnectLauncher = healthConnectLauncher
                        )
                    },
                    onSkipClick = {
                        scope.launch {
                            if (pagerState.currentPage < uiState.steps.size - 1) {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            } else {
                                viewModel.completeOnboarding()
                                onOnboardingComplete()
                            }
                        }
                    }
                )
            }

            // Page indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(uiState.steps.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(if (isSelected) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                 if (isSelected) 
                                    AppTheme.colors.accentBlue
                                 else 
                                    AppTheme.colors.textSecondary.copy(alpha = 0.3f)
                            )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OnboardingTopBar(
    currentStep: Int,
    totalSteps: Int,
    backgroundColor: Color,
    textColor: Color,
    onBackClick: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = "Setup (${currentStep + 1}/$totalSteps)",
                style = MaterialTheme.typography.titleMedium,
                color = textColor
            )
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = backgroundColor,
            navigationIconContentColor = textColor
        ),
        navigationIcon = {
            if (currentStep > 0) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = textColor
                    )
                }
            }
        }
    )
}

@Composable
private fun OnboardingBottomBar(
    currentStep: Int,
    totalSteps: Int,
    permissionGranted: Boolean,
    backgroundColor: Color,
    accentColor: Color,
    onNextClick: () -> Unit,
    onFinishClick: () -> Unit
) {
    Surface(
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth(),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentStep < totalSteps - 1) {
                Button(
                    onClick = onNextClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = AppTheme.colors.backgroundPrimary
                    )
                ) {
                    Text("Next")
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null
                    )
                }
            } else {
                Button(
                    onClick = onFinishClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = AppTheme.colors.backgroundPrimary
                    )
                ) {
                    Text("Finish Setup")
                }
            }
        }
    }
}

private fun handlePermissionRequest(
    step: OnboardingStep,
    context: android.content.Context,
    permissionLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    healthConnectLauncher: androidx.activity.result.ActivityResultLauncher<Set<String>>
) {
    when (step.permissionType) {
        is PermissionType.RuntimePermission -> {
            permissionLauncher.launch(step.permissionType.permission)
        }
        is PermissionType.HealthConnectPermission -> {
            healthConnectLauncher.launch(HealthConnectManager.permissions)
        }
        is PermissionType.UsageStatsPermission -> {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(intent)
        }
    }
}
