package ru.agromarket.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.agromarket.data.repository.AgroRepository
import ru.agromarket.data.repository.ApiResult
import ru.agromarket.ui.components.AuthHero
import ru.agromarket.ui.components.ErrorBanner
import javax.inject.Inject

enum class ForgotPasswordStep { EMAIL, RESET }

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val repository: AgroRepository
) : ViewModel() {
    var step by mutableStateOf(ForgotPasswordStep.EMAIL)
    var email by mutableStateOf("")
    var code by mutableStateOf("")
    var newPassword by mutableStateOf("")
    var newPasswordConfirm by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var message by mutableStateOf<String?>(null)

    fun sendCode() {
        if (email.isBlank() || !email.contains("@")) { error = "Введите корректный email"; return }
        viewModelScope.launch {
            isLoading = true; error = null
            when (val result = repository.forgotPassword(email.trim())) {
                is ApiResult.Success -> { message = result.data.message; step = ForgotPasswordStep.RESET }
                is ApiResult.Error -> error = result.message
            }
            isLoading = false
        }
    }

    fun resetPassword(onSuccess: () -> Unit) {
        if (code.length < 6) { error = "Введите код из письма"; return }
        if (newPassword.length < MIN_PASSWORD_LENGTH) { error = PASSWORD_LENGTH_ERROR; return }
        if (newPassword != newPasswordConfirm) { error = "Пароли не совпадают"; return }
        viewModelScope.launch {
            isLoading = true; error = null
            when (val result = repository.resetPassword(email.trim(), code.trim(), newPassword)) {
                is ApiResult.Success -> onSuccess()
                is ApiResult.Error -> error = result.message
            }
            isLoading = false
        }
    }
}

@Composable
fun ForgotPasswordScreen(
    onResetSuccess: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ForgotPasswordViewModel = hiltViewModel()
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        AuthHero(
            title = "Восстановление пароля",
            subtitle = when (viewModel.step) {
                ForgotPasswordStep.EMAIL -> "Укажите email от аккаунта"
                ForgotPasswordStep.RESET -> "Введите код из письма и новый пароль"
            },
            modifier = Modifier.fillMaxWidth().weight(0.38f)
        )

        Surface(
            modifier = Modifier.fillMaxWidth().weight(0.62f),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                when (viewModel.step) {
                    ForgotPasswordStep.EMAIL -> {
                        OutlinedTextField(
                            value = viewModel.email, onValueChange = { viewModel.email = it },
                            label = { Text("Email") }, leadingIcon = { Icon(Icons.Default.Email, null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    ForgotPasswordStep.RESET -> {
                        viewModel.message?.let {
                            Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        OutlinedTextField(
                            value = viewModel.code, onValueChange = { viewModel.code = it },
                            label = { Text("Код из письма") }, leadingIcon = { Icon(Icons.Default.Pin, null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = viewModel.newPassword, onValueChange = { viewModel.newPassword = it },
                            label = { Text("Новый пароль") }, leadingIcon = { Icon(Icons.Default.Lock, null) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                                }
                            },
                            visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = viewModel.newPasswordConfirm, onValueChange = { viewModel.newPasswordConfirm = it },
                            label = { Text("Подтвердите пароль") }, leadingIcon = { Icon(Icons.Default.Lock, null) },
                            visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                viewModel.error?.let {
                    Spacer(modifier = Modifier.height(12.dp))
                    ErrorBanner(message = it)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        when (viewModel.step) {
                            ForgotPasswordStep.EMAIL -> viewModel.sendCode()
                            ForgotPasswordStep.RESET -> viewModel.resetPassword(onResetSuccess)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled = !viewModel.isLoading,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    if (viewModel.isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    else Text(
                        when (viewModel.step) {
                            ForgotPasswordStep.EMAIL -> "Получить код"
                            ForgotPasswordStep.RESET -> "Сменить пароль"
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = onNavigateBack, modifier = Modifier.fillMaxWidth()) {
                    Text("Назад ко входу")
                }
            }
        }
    }
}
