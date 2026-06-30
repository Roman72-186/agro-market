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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.agromarket.data.repository.AgroRepository
import ru.agromarket.data.repository.ApiResult
import ru.agromarket.ui.components.AuthHero
import ru.agromarket.ui.components.ErrorBanner

enum class RegisterStep { EMAIL, CODE, PASSWORD }

class RegisterViewModel(
    private val repository: AgroRepository
) : ViewModel() {
    var step by mutableStateOf(RegisterStep.EMAIL)
    var email by mutableStateOf("")
    var code by mutableStateOf("")
    var password by mutableStateOf("")
    var passwordConfirm by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var message by mutableStateOf<String?>(null)

    fun sendCode() {
        if (email.isBlank() || !email.contains("@")) { error = "Введите корректный email"; return }
        viewModelScope.launch {
            isLoading = true; error = null
            when (val result = repository.register(email.trim())) {
                is ApiResult.Success -> { message = result.data.message; step = RegisterStep.CODE }
                is ApiResult.Error -> error = result.message
            }
            isLoading = false
        }
    }

    fun verifyCode() {
        if (code.length < 4) { error = "Введите код из письма"; return }
        viewModelScope.launch {
            isLoading = true; error = null
            when (val result = repository.verify(email.trim(), code.trim())) {
                is ApiResult.Success -> step = RegisterStep.PASSWORD
                is ApiResult.Error -> error = result.message
            }
            isLoading = false
        }
    }

    fun setPassword(onSuccess: () -> Unit) {
        if (password.length < MIN_PASSWORD_LENGTH) { error = PASSWORD_LENGTH_ERROR; return }
        if (password != passwordConfirm) { error = "Пароли не совпадают"; return }
        viewModelScope.launch {
            isLoading = true; error = null
            when (val result = repository.setPassword(email.trim(), code.trim(), password)) {
                is ApiResult.Success -> onSuccess()
                is ApiResult.Error -> error = result.message
            }
            isLoading = false
        }
    }
}

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: RegisterViewModel = koinViewModel()
) {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        AuthHero(
            title = "Регистрация",
            subtitle = when (viewModel.step) {
                RegisterStep.EMAIL -> "Шаг 1 из 3 — укажите email"
                RegisterStep.CODE -> "Шаг 2 из 3 — код подтверждения"
                RegisterStep.PASSWORD -> "Шаг 3 из 3 — придумайте пароль"
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
                    RegisterStep.EMAIL -> {
                        OutlinedTextField(
                            value = viewModel.email, onValueChange = { viewModel.email = it },
                            label = { Text("Email") }, leadingIcon = { Icon(Icons.Default.Email, null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    RegisterStep.CODE -> {
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
                    }
                    RegisterStep.PASSWORD -> {
                        OutlinedTextField(
                            value = viewModel.password, onValueChange = { viewModel.password = it },
                            label = { Text("Пароль") }, leadingIcon = { Icon(Icons.Default.Lock, null) },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = viewModel.passwordConfirm, onValueChange = { viewModel.passwordConfirm = it },
                            label = { Text("Подтвердите пароль") }, leadingIcon = { Icon(Icons.Default.Lock, null) },
                            visualTransformation = PasswordVisualTransformation(),
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
                            RegisterStep.EMAIL -> viewModel.sendCode()
                            RegisterStep.CODE -> viewModel.verifyCode()
                            RegisterStep.PASSWORD -> viewModel.setPassword(onRegisterSuccess)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled = !viewModel.isLoading,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    if (viewModel.isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    else Text(
                        when (viewModel.step) {
                            RegisterStep.EMAIL -> "Получить код"
                            RegisterStep.CODE -> "Подтвердить"
                            RegisterStep.PASSWORD -> "Создать аккаунт"
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = onNavigateBack, modifier = Modifier.fillMaxWidth()) {
                    Text("Уже есть аккаунт? Войти")
                }
            }
        }
    }
}
