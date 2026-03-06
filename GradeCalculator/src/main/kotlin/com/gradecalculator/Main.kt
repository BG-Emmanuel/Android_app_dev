package com.gradecalculator

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.*
import com.gradecalculator.models.demonstrateHigherOrderFunctions
import com.gradecalculator.theme.AppColors
import com.gradecalculator.theme.GradeCalculatorTheme
import com.gradecalculator.viewmodels.AppView
import com.gradecalculator.viewmodels.AppViewModel
import com.gradecalculator.views.HomeView
import com.gradecalculator.views.LoadingOverlay
import com.gradecalculator.views.SettingsView
import com.gradecalculator.views.VaultView

// ─────────────────────────────────────────────────────────────────────────────
// Main Entry Point
// ─────────────────────────────────────────────────────────────────────────────

fun main() {
    // Demonstrate higher-order functions at startup (console output)
    demonstrateHigherOrderFunctions()

    application {
        val windowState = rememberWindowState(
            size     = DpSize(1280.dp, 800.dp),
            position = WindowPosition(Alignment.Center)
        )

        Window(
            onCloseRequest = ::exitApplication,
            state          = windowState,
            title          = "GradeCalculator"
        ) {
            GradeCalculatorApp()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Root App Composable
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun GradeCalculatorApp() {
    val vm = remember { AppViewModel() }

    GradeCalculatorTheme(darkTheme = vm.isDarkTheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color    = MaterialTheme.colorScheme.background
        ) {
            Row(Modifier.fillMaxSize()) {

                // ── Sidebar ───────────────────────────────────────────────────
                AppSidebar(vm)

                VerticalDivider(Modifier.fillMaxHeight())

                // ── Main content ──────────────────────────────────────────────
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    AnimatedContent(
                        targetState    = vm.currentView,
                        transitionSpec = {
                            (fadeIn() + slideInHorizontally { it / 10 }) togetherWith
                            (fadeOut() + slideOutHorizontally { -it / 10 })
                        }
                    ) { view ->
                        when (view) {
                            AppView.HOME     -> HomeView(vm)
                            AppView.VAULT    -> VaultView(vm)
                            AppView.SETTINGS -> SettingsView(vm)
                        }
                    }

                    // Global loading overlay
                    if (vm.isLoading) LoadingOverlay(vm.loadingMessage)

                    // Toast notifications
                    Column(
                        Modifier.align(Alignment.BottomEnd).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        vm.successMessage?.let { msg -> ToastNotification(msg, isError = false) }
                        vm.errorMessage?.let   { msg -> ToastNotification(msg, isError = true) }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sidebar Navigation
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AppSidebar(vm: AppViewModel) {
    Column(
        modifier            = Modifier.width(200.dp).fillMaxHeight()
                                      .background(MaterialTheme.colorScheme.surface)
                                      .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            // Logo / App name
            Row(
                Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.size(32.dp),
                        shape    = RoundedCornerShape(8.dp),
                        color    = AppColors.Violet
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("G", fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp, color = Color.White)
                        }
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("GradeCalc", fontWeight = FontWeight.Bold, fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface)
                    Text("v1.0.0", fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 4.dp))

            // Nav items
            SidebarNavItem(
                label    = "Home",
                icon     = Icons.Default.Home,
                selected = vm.currentView == AppView.HOME,
                badge    = if (vm.importedStudents.isNotEmpty()) "${vm.importedStudents.size}" else null,
                onClick  = { vm.navigateTo(AppView.HOME) }
            )
            SidebarNavItem(
                label    = "Vault",
                icon     = Icons.Default.FolderOpen,
                selected = vm.currentView == AppView.VAULT,
                badge    = if (vm.vault.isNotEmpty()) "${vm.vault.size}" else null,
                onClick  = { vm.navigateTo(AppView.VAULT) }
            )
            SidebarNavItem(
                label    = "Settings",
                icon     = Icons.Default.Settings,
                selected = vm.currentView == AppView.SETTINGS,
                onClick  = { vm.navigateTo(AppView.SETTINGS) }
            )
        }

        // Bottom: theme toggle shortcut
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            HorizontalDivider()
            Spacer(Modifier.height(4.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = vm::toggleTheme)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (vm.isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    if (vm.isDarkTheme) "Light Mode" else "Dark Mode",
                    fontSize = 13.sp,
                    color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun SidebarNavItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    badge: String? = null,
    onClick: () -> Unit
) {
    val bgColor = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val fgColor = if (selected) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(8.dp),
        color    = bgColor
    ) {
        Row(
            Modifier.clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icon, contentDescription = label, tint = fgColor, modifier = Modifier.size(20.dp))
            Text(label, color = fgColor, fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.weight(1f))
            badge?.let {
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primary) {
                    Text(it, modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                        fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Toast Notification
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ToastNotification(message: String, isError: Boolean) {
    val bgColor = if (isError) MaterialTheme.colorScheme.errorContainer
                  else Color(0xFF1B5E20)
    val fgColor = if (isError) MaterialTheme.colorScheme.onErrorContainer
                  else Color.White

    AnimatedVisibility(
        visible = true,
        enter   = slideInHorizontally { it } + fadeIn(),
        exit    = slideOutHorizontally { it } + fadeOut()
    ) {
        Surface(
            shape  = RoundedCornerShape(12.dp),
            color  = bgColor,
            shadowElevation = 8.dp
        ) {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (isError) Icons.Default.Error else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint     = fgColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(message, color = fgColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// Extension for clip
private fun Modifier.clip(shape: androidx.compose.ui.graphics.Shape) =
    this.then(Modifier.background(Color.Transparent, shape).border(0.dp, Color.Transparent, shape))
