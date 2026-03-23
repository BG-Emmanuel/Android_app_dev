package views

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import models.*
import state.AppState
import theme.gradeColor
import javax.swing.JFileChooser

@Composable
fun SettingsView(state: AppState) {
    var showScaleDialog by remember { mutableStateOf(false) }
    var editingScale by remember { mutableStateOf<GradingScale?>(null) }

    Row(modifier = Modifier.fillMaxSize()) {
        // ── Left: navigation tabs ─────────────────────────────────────────────
        // (for simplicity, show all settings in a single scrollable column)

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item { Text("Settings", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold) }

            // ── Grading Scales ────────────────────────────────────────────────
            item {
                SectionCard(title = "Grading Scales", icon = Icons.Default.Grading) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.gradingScales.forEach { scale ->
                            GradingScaleRow(
                                scale = scale,
                                isSelected = state.selectedScale.id == scale.id,
                                onSelect = { state.setDefaultScale(scale) },
                                onEdit = { editingScale = scale; showScaleDialog = true }
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = { editingScale = null; showScaleDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Create Custom Grading Scale")
                        }
                    }
                }
            }

            // ── Export Preferences ────────────────────────────────────────────
            item {
                SectionCard(title = "Export Preferences", icon = Icons.Default.FileDownload) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Default Export Format", style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ExportFormat.values().forEach { fmt ->
                                FilterChip(
                                    selected = state.defaultExportFormat == fmt,
                                    onClick  = { state.defaultExportFormat = fmt },
                                    label    = { Text(fmt.name) },
                                    leadingIcon = {
                                        if (state.defaultExportFormat == fmt)
                                            Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                                    }
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Save Location", style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = state.defaultSaveLocation,
                                onValueChange = { state.defaultSaveLocation = it },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                leadingIcon = { Icon(Icons.Default.Folder, null) }
                            )
                            OutlinedButton(onClick = {
                                val chooser = JFileChooser().apply {
                                    fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                                    dialogTitle = "Choose Save Location"
                                }
                                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                                    state.defaultSaveLocation = chooser.selectedFile.absolutePath
                                }
                            }) { Text("Browse") }
                        }
                    }
                }
            }

            // ── Appearance ────────────────────────────────────────────────────
            item {
                SectionCard(title = "Appearance", icon = Icons.Default.Palette) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Dark Mode", fontWeight = FontWeight.Medium)
                            Text("Switch between light and dark themes",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(0.55f))
                        }
                        Switch(
                            checked = state.isDarkTheme,
                            onCheckedChange = { state.isDarkTheme = it }
                        )
                    }
                }
            }

            // ── About ─────────────────────────────────────────────────────────
            item {
                SectionCard(title = "About", icon = Icons.Default.Info) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        InfoRow("App",      "Grade Calculator Pro")
                        InfoRow("Version",  "1.0.0")
                        InfoRow("Language", "Kotlin + Compose Desktop")
                        InfoRow("Stack",    "Apache POI · OpenCSV · PDFBox · Coroutines")
                    }
                }
            }
        }
    }

    // ── Grading Scale Dialog ───────────────────────────────────────────────────
    if (showScaleDialog) {
        GradingScaleDialog(
            existing = editingScale,
            onDismiss = { showScaleDialog = false },
            onSave = { scale ->
                state.addOrUpdateScale(scale)
                showScaleDialog = false
            }
        )
    }
}

// ─── Section Card ─────────────────────────────────────────────────────────────
@Composable
private fun SectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            HorizontalDivider()
            content()
        }
    }
}

// ─── Grading Scale Row ────────────────────────────────────────────────────────
@Composable
private fun GradingScaleRow(
    scale: GradingScale,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        RadioButton(selected = isSelected, onClick = onSelect)
        Column(modifier = Modifier.weight(1f)) {
            Text(scale.name, fontWeight = FontWeight.Medium)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                scale.ranges.forEach { range ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(gradeColor(range.grade).copy(0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("${range.grade}: ${range.minScore.toInt()}–${range.maxScore.toInt()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = gradeColor(range.grade), fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
        if (isSelected) {
            AssistChip(
                onClick = {},
                label = { Text("Active", style = MaterialTheme.typography.labelSmall) },
                leadingIcon = { Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp)) }
            )
        }
        IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Edit, "Edit", modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(0.6f))
        }
    }
}

// ─── Info Row ─────────────────────────────────────────────────────────────────
@Composable
private fun InfoRow(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, fontWeight = FontWeight.Medium,
            modifier = Modifier.width(80.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(0.6f),
            style = MaterialTheme.typography.bodySmall)
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

// ─── Grading Scale Dialog ─────────────────────────────────────────────────────
@Composable
private fun GradingScaleDialog(
    existing: GradingScale?,
    onDismiss: () -> Unit,
    onSave: (GradingScale) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var ranges by remember {
        mutableStateOf(
            existing?.ranges ?: listOf(
                GradeRange(70.0, 100.0, "A", "#4CAF50"),
                GradeRange(60.0, 69.99, "B", "#8BC34A"),
                GradeRange(50.0, 59.99, "C", "#FF9800"),
                GradeRange(40.0, 49.99, "D", "#FF5722"),
                GradeRange(0.0,  39.99, "F", "#F44336")
            )
        )
    }
    var nameError by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New Grading Scale" else "Edit: ${existing.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.width(480.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameError = "" },
                    label = { Text("Scale Name") },
                    isError = nameError.isNotEmpty(),
                    supportingText = { if (nameError.isNotEmpty()) Text(nameError) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Text("Grade Ranges", fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleSmall)
                ranges.forEachIndexed { i, range ->
                    GradeRangeRow(
                        range = range,
                        onChange = { ranges = ranges.toMutableList().also { it[i] = range } }
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isBlank()) { nameError = "Name is required"; return@Button }
                val id = existing?.id ?: "scale_${System.currentTimeMillis()}"
                onSave(GradingScale(id = id, name = name.trim(), ranges = ranges))
            }) { Text("Save Scale") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun GradeRangeRow(range: GradeRange, onChange: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(28.dp).clip(RoundedCornerShape(6.dp))
                .background(gradeColor(range.grade).copy(0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(range.grade, fontWeight = FontWeight.Bold, color = gradeColor(range.grade))
        }
        Text("${range.minScore.toInt()} – ${range.maxScore.toInt()}",
            modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
    }
}
