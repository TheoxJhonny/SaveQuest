package com.rpgahorro.savequest

import android.content.Intent
import android.app.KeyguardManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.compose.runtime.mutableStateOf
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import com.rpgahorro.savequest.ui.security.AccessScreen
import com.rpgahorro.savequest.ui.security.AccessViewModel
import com.rpgahorro.savequest.ui.theme.SaveQuestTheme

class MainActivity : FragmentActivity() {
    private lateinit var access: AccessViewModel
    private var prompt: BiometricPrompt? = null
    private var availability by mutableIntStateOf(BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE)

    private var credentialAvailable by mutableStateOf(false)
    private val credentialResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val attempt = access.activeAttempt
        if (attempt != null && access.credentialPending) {
            if (result.resultCode == RESULT_OK) {
                access.authenticated(attempt)
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) access.resumed()
            } else access.failed(attempt, "Acceso cancelado. Tus registros siguen bloqueados.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Evita mostrar información financiera en capturas y en la vista de aplicaciones recientes.
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        access = ViewModelProvider(this)[AccessViewModel::class.java]
        // AndroidX conserva el diálogo durante una rotación; reconectar su callback.
        access.activeAttempt?.let { if (!access.credentialPending || Build.VERSION.SDK_INT >= 30) prompt = createPrompt(it) }
        setContent {
            SaveQuestTheme {
                if (access.unlocked) SaveQuestApp()
                else AccessScreen(
                    message = access.message ?: availabilityMessage(),
                    busy = access.activeAttempt != null,
                    canAuthenticate = availability == BiometricManager.BIOMETRIC_SUCCESS,
                    canEnroll = availability == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED,
                    onUnlock = ::authenticate,
                    onConfigure = ::openEnrollment,
                    onRetry = ::refreshAvailability,
                    canUsePin = credentialAvailable,
                    onPin = ::authenticateCredential,
                    onConfigurePin = { openSecuritySettings() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshAvailability()
        access.resumed()
    }

    override fun onStop() {
        if (!isChangingConfigurations) {
            access.backgrounded()
            if (!access.credentialPending) prompt?.cancelAuthentication()
        }
        super.onStop()
    }

    private fun refreshAvailability() {
        availability = BiometricManager.from(this).canAuthenticate(BIOMETRIC_STRONG)
        credentialAvailable = getSystemService(KeyguardManager::class.java).isDeviceSecure
    }

    private fun authenticate() {
        refreshAvailability()
        if (availability != BiometricManager.BIOMETRIC_SUCCESS) return
        val attempt = access.begin() ?: return
        prompt = createPrompt(attempt)
        try {
            prompt!!.authenticate(BiometricPrompt.PromptInfo.Builder()
                .setTitle("Desbloquear SaveQuest")
                .setSubtitle("Confirma tu identidad para ver tus finanzas")
                .setAllowedAuthenticators(BIOMETRIC_STRONG)
                .setNegativeButtonText("Cancelar")
                .build())
        } catch (_: RuntimeException) {
            access.failed(attempt, "No se pudo iniciar la autenticación. Inténtalo nuevamente.")
        }
    }

    private fun createPrompt(attempt: Long) = BiometricPrompt(this, ContextCompat.getMainExecutor(this),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                if (access.credentialPending) {
                    access.authenticated(attempt)
                    if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) access.resumed()
                } else if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) access.authenticated(attempt)
                else access.lock()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                access.failed(attempt, when (errorCode) {
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON, BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_CANCELED -> "Acceso cancelado. Tus registros siguen bloqueados."
                    else -> errString.toString()
                })
            }
        })

    private fun authenticateCredential() {
        refreshAvailability()
        if (!credentialAvailable) return
        val attempt = access.begin(deviceCredential = true) ?: return
        try {
            if (Build.VERSION.SDK_INT >= 30) {
                prompt = createPrompt(attempt)
                prompt!!.authenticate(BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Desbloquear SaveQuest")
                    .setSubtitle("Usa el PIN, patrón o contraseña de tu teléfono")
                    .setAllowedAuthenticators(DEVICE_CREDENTIAL)
                    .build())
            } else launchLegacyCredential(attempt)
        } catch (_: RuntimeException) {
            access.failed(attempt, "No se pudo abrir el bloqueo de Android. Inténtalo nuevamente.")
        }
    }

    @Suppress("DEPRECATION") // DEVICE_CREDENTIAL no está soportado por BiometricPrompt antes de API 30.
    private fun launchLegacyCredential(attempt: Long) {
        val intent = getSystemService(KeyguardManager::class.java)
            .createConfirmDeviceCredentialIntent("Desbloquear SaveQuest", "Confirma el bloqueo de tu teléfono")
        if (intent != null) credentialResult.launch(intent)
        else access.failed(attempt, "Configura primero el bloqueo de pantalla del teléfono.")
    }

    private fun openSecuritySettings() {
        val intent = Intent(Settings.ACTION_SECURITY_SETTINGS)
        if (intent.resolveActivity(packageManager) != null) startActivity(intent)
    }

    private fun openEnrollment() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_BIOMETRIC_ENROLL).putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, BIOMETRIC_STRONG)
        } else Intent(Settings.ACTION_SECURITY_SETTINGS)
        if (intent.resolveActivity(packageManager) != null) startActivity(intent)
    }

    private fun availabilityMessage(): String = when (availability) {
        BiometricManager.BIOMETRIC_SUCCESS -> "Usa la biometría de tu dispositivo para acceder."
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "Configura una huella o biometría compatible en los ajustes del dispositivo."
        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "Este dispositivo no dispone de biometría compatible. Puedes usar el bloqueo del teléfono."
        else -> "La biometría no está disponible en este momento. Comprueba los ajustes del dispositivo e inténtalo nuevamente."
    }
}
