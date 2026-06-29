package ru.agromarket

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/**
 * iOS-entrypoint (Фаза 0, заглушка). Возвращает ComposeUIViewController с пустым
 * содержимым — нужен только чтобы source set iosMain существовал. Реальный UI
 * подключится после переноса экранов в commonMain (фазы 1d–3).
 *
 * Внимание: Kotlin/Native компилирует iOS-таргет ТОЛЬКО на macOS. На Windows этот
 * файл не компилируется — это ожидаемо.
 */
fun MainViewController(): UIViewController = ComposeUIViewController {
    // TODO(Фаза 3): подключить корневой Composable приложения из commonMain.
}
