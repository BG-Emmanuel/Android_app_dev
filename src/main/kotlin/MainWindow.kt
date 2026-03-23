package views

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import models.AppView
import state.AppState
import theme.Blue600
import theme.GradeCalculatorTheme
import theme.Violet700

@Composable
fun MainWindow(state: AppState) {
    GradeCalculatorTheme(darkTheme = state.isDarkTheme) {
        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxSize()) {
                // ── Sidebar ───────────────────────────────────────────────────
                Sidebar(state)

                // ── Main content ──────────────────────────────────────────────
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    // Notifications bar
                    AnimatedVisibility(
                        visible = state.errorMessage != null || state.successMessage != null,
                        enter = slideInVertically() + fadeIn(),
                        exit  = slideOutVertically() + fadeOut()
                    ) {
                        state.errorMessage?.let { msg ->
                            NotificationBanner(msg, isError = true) { state.clearMessages() }
                        }
                        state.successMessage?.let { msg ->
                            NotificationBanner(msg, isError = false) { state.clearMessages() }
                        }
                    }

                    // Active view
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when (state.currentView) {
                            AppView.HOME     -> HomeView(state)
                            AppView.VAULT    -> VaultView(state)
                            AppView.SETTINGS -> SettingsView(state)
                        }
                    }
                }
            }
        }
    }
}

// ─── Sidebar ──────────────────────────────────────────────────────────────────
@Composable
private fun Sidebar(state: AppState) {
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF1A237E), Color(0xFF4A148C))
    )

    Column(
        modifier = Modifier
            .width(220.dp)
            .fillMaxHeight()
            .background(gradientBrush)
            .padding(vertical = 20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            // Logo
            Row(
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.School, null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Column {
                    Text("Grade", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 18.sp)
                    Text("Calculator", color = Color.White.copy(0.65f), fontSize = 12.sp, lineHeight = 14.sp)
                }
            }

            // Nav items
            NavItem(Icons.Default.Home,        "Home",     AppView.HOME,     state)
            NavItem(Icons.Default.FolderOpen,  "Vault",    AppView.VAULT,    state)
            NavItem(Icons.Default.Settings,    "Settings", AppView.SETTINGS, state)
        }

        // Bottom: theme + version
        Column(modifier = Modifier.padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            HorizontalDivider(color = Color.White.copy(0.2f))
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(0.08f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (state.isDarkTheme) "Dark Mode" else "Light Mode",
                    color = Color.White.copy(0.8f), fontSize = 12.sp)
                Switch(
                    checked = state.isDarkTheme,
                    onCheckedChange = { state.isDarkTheme = it },
                    modifier = Modifier.height(20.dp),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor  = Color.White,
                        checkedTrackColor  = Blue600,
                        uncheckedThumbColor = Color.White.copy(0.7f),
                        uncheckedTrackColor = Color.White.copy(0.2f)
                    )
                )
            }
            Text("v1.0.0  •  Kotlin Desktop",
                color = Color.White.copy(0.35f), fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 4.dp))
        }
    }
}

@Composable
private fun NavItem(
    icon: ImageVector,
    label: String,
    view: AppView,
    state: AppState
) {
    val selected = state.currentView == view
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Color.White.copy(0.15f) else Color.Transparent)
            .clickable { state.currentView = view }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (selected) {
            Box(
                modifier = Modifier.size(6.dp).clip(CircleShape)
                    .background(Color.White)
            )
        } else {
            Spacer(Modifier.size(6.dp))
        }
        Icon(icon, label, tint = if (selected) Color.White else Color.White.copy(0.55f),
            modifier = Modifier.size(18.dp))
        Text(label, color = if (selected) Color.White else Color.White.copy(0.65f),
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 14.sp)
    }
}

// ─── Notification Banner ──────────────────────────────────────────────────────
@Composable
private fun NotificationBanner(message: String, isError: Boolean, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isError) MaterialTheme.colorScheme.errorContainer
                        else Color(0xFF1B5E20).copy(alpha = 0.15f))
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            if (isError) Icons.Default.Error else Icons.Default.CheckCircle,
            null,
            tint = if (isError) MaterialTheme.colorScheme.error else Color(0xFF4CAF50),
            modifier = Modifier.size(18.dp)
        )
        Text(
            message,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = if (isError) MaterialTheme.colorScheme.onErrorContainer else Color(0xFF1B5E20)
        )
        IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.Close, "Dismiss", modifier = Modifier.size(16.dp))
        }
    }
}
