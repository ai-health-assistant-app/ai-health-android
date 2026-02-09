package com.ai_health.feature.onboarding

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.health.connect.client.HealthConnectClient
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai_health.core.data.sync.HealthSyncScheduler
import com.ai_health.core.health.HealthConnectManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val currentStep: Int = 0,
    val steps: List<OnboardingStep> = OnboardingStep.getAllSteps(),
    val permissionStatuses: Map<OnboardingStep, PermissionStatus> = mapOf(),
    val isOnboardingComplete: Boolean = false
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val healthConnectManager: HealthConnectManager,
    private val syncScheduler: HealthSyncScheduler,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val TAG = "OnboardingViewModel"
        private const val KEY_CURRENT_STEP = "current_step"
    }

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val sharedPrefs by lazy {
        context.getSharedPreferences("onboarding_prefs", Context.MODE_PRIVATE)
    }

    init {
        // Ripristina currentStep da SavedStateHandle se disponibile
        val savedStep = savedStateHandle.get<Int>(KEY_CURRENT_STEP) ?: 0
        _uiState.update { it.copy(currentStep = savedStep) }
        
        checkOnboardingCompletion()
    }

    fun checkPermissions() {
        viewModelScope.launch {
            val statuses = mutableMapOf<OnboardingStep, PermissionStatus>()
            
            OnboardingStep.getAllSteps().forEach { step ->
                statuses[step] = when (step.permissionType) {
                    is PermissionType.RuntimePermission -> {
                        checkRuntimePermission(step.permissionType.permission)
                    }
                    is PermissionType.HealthConnectPermission -> {
                        checkHealthConnectPermission()
                    }
                    is PermissionType.UsageStatsPermission -> {
                        checkUsageStatsPermission()
                    }
                }
            }
            
            _uiState.update { it.copy(permissionStatuses = statuses) }
            
            // PRIVACY-PROOF: Trigger sync when Health Connect permissions are granted
            val healthConnectStep = OnboardingStep.getAllSteps().find { 
                it.permissionType is PermissionType.HealthConnectPermission 
            }
            if (healthConnectStep != null && statuses[healthConnectStep] == PermissionStatus.Granted) {
                Log.d(TAG, "Health Connect permissions granted, triggering sync")
                syncScheduler.scheduleForegroundSync()
            }
        }
    }

    private fun checkRuntimePermission(permission: String): PermissionStatus {
        return when {
            ContextCompat.checkSelfPermission(context, permission) == 
                PackageManager.PERMISSION_GRANTED -> PermissionStatus.Granted
            else -> PermissionStatus.NotRequested
        }
    }

    private suspend fun checkHealthConnectPermission(): PermissionStatus {
        return try {
            val sdkStatus = HealthConnectClient.getSdkStatus(context)
            if (sdkStatus == HealthConnectClient.SDK_AVAILABLE) {
                val hasPermissions = healthConnectManager.hasAllPermissions()
                if (hasPermissions) PermissionStatus.Granted else PermissionStatus.NotRequested
            } else {
                PermissionStatus.Denied
            }
        } catch (e: Exception) {
            PermissionStatus.Denied
        }
    }

    private fun checkUsageStatsPermission(): PermissionStatus {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        
        return if (mode == AppOpsManager.MODE_ALLOWED) {
            PermissionStatus.Granted
        } else {
            PermissionStatus.NotRequested
        }
    }

    fun nextStep() {
        val currentStep = _uiState.value.currentStep
        if (currentStep < _uiState.value.steps.size - 1) {
            val newStep = currentStep + 1
            savedStateHandle[KEY_CURRENT_STEP] = newStep
            _uiState.update { it.copy(currentStep = newStep) }
        }
    }

    fun previousStep() {
        val currentStep = _uiState.value.currentStep
        if (currentStep > 0) {
            val newStep = currentStep - 1
            savedStateHandle[KEY_CURRENT_STEP] = newStep
            _uiState.update { it.copy(currentStep = newStep) }
        }
    }

    fun goToStep(index: Int) {
        if (index in 0 until _uiState.value.steps.size) {
            savedStateHandle[KEY_CURRENT_STEP] = index
            _uiState.update { it.copy(currentStep = index) }
        }
    }

    fun skipCurrentStep() {
        nextStep()
    }

    fun completeOnboarding() {
        sharedPrefs.edit().putBoolean("onboarding_completed", true).apply()
        _uiState.update { it.copy(isOnboardingComplete = true) }
    }

    fun isOnboardingComplete(): Boolean {
        return sharedPrefs.getBoolean("onboarding_completed", false)
    }

    private fun checkOnboardingCompletion() {
        val isComplete = isOnboardingComplete()
        _uiState.update { it.copy(isOnboardingComplete = isComplete) }
    }

    fun updatePermissionStatus(step: OnboardingStep, status: PermissionStatus) {
        _uiState.update { 
            it.copy(
                permissionStatuses = it.permissionStatuses + (step to status)
            )
        }
    }
}
