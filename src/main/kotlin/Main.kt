import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import state.AppState
import views.MainWindow

fun main() = application {
    val windowState = rememberWindowState(width = 1280.dp, height = 820.dp)

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "Grade Calculator Pro"
    ) {
        val appState = remember { AppState() }
        MainWindow(appState)
    }
}
