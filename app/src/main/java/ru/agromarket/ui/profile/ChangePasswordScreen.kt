package ru.agromarket.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.agromarket.data.repository.AgroRepository
import ru.agromarket.data.repository.ApiResult
import ru.agromarket.ui.auth.MIN_PASSWORD_LENGTH
import ru.agromarket.ui.components.AppTopBar
import ru.agromarket.ui.components.ErrorBanner
import javax.inject.Inject

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val repository: AgroRepository
) : ViewModel() {
    var currentPassword by mutableStateOf("")
    var newPassword by mutableStateOf("")
    var newPasswordConfirm by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    fun changePassword(onSuccess: () -> Unit) {
        if (currentPassword.isBlank()) { error = "Введите текущий пароль"; return }
        if (newPassword.length < MIN_PASSWORD_LENGTH) { error = "Новый пароль минимум $MIN_PASSWORD_LENGTH символов"; return }
        if (newPassword != newPasswordConfirm) { error = "Пароли не совпадают"; return }
        viewModelScope.launch {
            isLoading = true; error = null
            when (val result = repository.changePassword(currentPassword, newPassword)) {
                is ApiResult.Success -> onSuccess()
                is ApiResult.Error -> error = result.message
            }
            isLoading = false
        }
    }
}

@Composable
fun ChangePasswordScreen(
    onSuccess: () -> Unit,
    onBack: () -> Unit,
    viewModel: ChangePasswordViewModel = hiltViewModel()
) {
    var currentVisible by remember { mutableStateOf(false) }
    var newVisible by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = "Смена пароля", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            OutlinedTextField(
                value = viewModel.currentPassword,
                onValueChange = { viewModel.currentPassword = it },
                label = { Text("Текущий пароль") },
                leadingIcon = { Icon(Icons.Default.Lock, null) },
                trailingIcon = {
                    IconButton(onClick = { currentVisible = !currentVisible }) {
                        Icon(if (currentVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                    }
                },
                visualTransformation = if (currentVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = viewModel.newPassword,
                onValueChange = { viewModel.newPassword = it },
                label = { Text("Новый пароль") },
                leadingIcon = { Icon(Icons.Default.LockReset, null) },
                trailingIcon = {
                    IconButton(onClick = { newVisible = !newVisible }) {
                        Icon(if (newVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                    }
                },
                visualTransformation = if (newVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = viewModel.newPasswordConfirm,
                onValueChange = { viewModel.newPasswordConfirm = it },
                label = { Text("Подтвердите новый пароль") },
                leadingIcon = { Icon(Icons.Default.LockReset, null) },
                visualTransformation = if (newVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            )

            viewModel.error?.let {
                Spacer(modifier = Modifier.height(12.dp))
                ErrorBanner(message = it)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.changePassword(onSuccess) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !viewModel.isLoading,
                shape = MaterialTheme.shapes.medium,
            ) {
                if (viewModel.isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                else Text("Сменить пароль", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
