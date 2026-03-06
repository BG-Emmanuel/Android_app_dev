package com.gradecalculator.views

import androidx.compose.foundation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gradecalculator.models.GradeRange
import com.gradecalculator.models.GradingScale
import com.gradecalculator.theme.AppColors
import com.gradecalculator.utils.FormattingUtils
import com.gradecalculator.viewmodels.AppViewModel

// ─────────────────────────────────────────────────────────────────────────────
// Settings View
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SettingsView(vm: AppViewModel) {
    val scrollState = rememberScrollState()

    Column(
        Modifier.fillMaxSize().padding(24.dp).verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text("Settings", fontSize = 22.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground)

        // ── Appearance ────────────────────────────────────────────────────────
        SettingsSection("Appearance") {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text("Dark Mode", fontWeight = FontWeight.Medium)
                    Text("Switch between light and dark theme", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
                Switch(checked = vm.isDarkTheme, onCheckedChange = { vm.isDarkTheme = it })
            }
        }

        // ── Grading Scales ────────────────────────────────────────────────────
        GradingScalesSection(vm)

        // ── Grade Curve ───────────────────────────────────────────────────────
        SettingsSection("Grade Curve") {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text("Enable Grade Curve", fontWeight = FontWeight.Medium)
                    Text("Adds bonus points to all students' final scores", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
                Switch(checked = vm.curveEnabled, onCheckedChange = { vm.curveEnabled = it })
            }
            if (vm.curveEnabled) {
                Spacer(Modifier.height(8.dp))
                Text("Curve Points: +${"%.0f".format(vm.curvePoints)}",
                    fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                Slider(
                    value         = vm.curvePoints.toFloat(),
                    onValueChange = { vm.curvePoints = it.toDouble() },
                    valueRange    = 0f..20f,
                    steps         = 19
                )
                Text("Score cap: 100 (scores will not exceed 100 after curve)",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        }

        // ── About ─────────────────────────────────────────────────────────────
        SettingsSection("About") {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                InfoRow("Application", "GradeCalculator")
                InfoRow("Version",     "1.0.0")
                InfoRow("Language",    "Kotlin + Compose Desktop")
                InfoRow("Author",      "GradeCalc Team")
                InfoRow("License",     "MIT")
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Grading Scales Section
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GradingScalesSection(vm: AppViewModel) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingScale     by remember { mutableStateOf<GradingScale?>(null) }

    SettingsSection("Grading Scales") {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text("Manage grading scales used for grade calculations",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            Button(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("New Scale")
            }
        }

        Spacer(Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            vm.gradingScales.forEach { scale ->
                GradingScaleCard(
                    scale     = scale,
                    isActive  = scale.id == vm.activeScale.id,
                    onSetDefault = { vm.setDefaultScale(scale) },
                    onEdit       = { editingScale = scale },
                    onDelete     = { if (vm.gradingScales.size > 1) vm.deleteGradingScale(scale.id) }
                )
            }
        }
    }

    if (showCreateDialog) {
        GradingScaleEditorDialog(
            initialScale = null,
            onSave       = { vm.addGradingScale(it); showCreateDialog = false },
            onDismiss    = { showCreateDialog = false }
        )
    }

    editingScale?.let { scale ->
        GradingScaleEditorDialog(
            initialScale = scale,
            onSave = { updated ->
                vm.gradingScales = vm.gradingScales.map { if (it.id == updated.id) updated else it }
                if (vm.activeScale.id == updated.id) vm.activeScale = updated
                editingScale = null
            },
            onDismiss = { editingScale = null }
        )
    }
}

@Composable
private fun GradingScaleCard(
    scale: GradingScale,
    isActive: Boolean,
    onSetDefault: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(10.dp),
        colors   = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface
        ),
        border = if (isActive) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(scale.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    if (isActive) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text("Active", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (!isActive) {
                        TextButton(onClick = onSetDefault) { Text("Set Default") }
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, null, Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, null, Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // Grade range preview
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                scale.ranges.sortedBy { it.grade }.forEach { range ->
                    val color = AppColors.fromHex(range.colorCode)
                    Surface(
                        shape  = RoundedCornerShape(6.dp),
                        color  = color.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(range.grade, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = color)
                            Text("${range.minScore.toInt()}–${range.maxScore.toInt()}",
                                fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Grading Scale Editor Dialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GradingScaleEditorDialog(
    initialScale: GradingScale?,
    onSave: (GradingScale) -> Unit,
    onDismiss: () -> Unit
) {
    var name   by remember { mutableStateOf(initialScale?.name ?: "") }
    var ranges by remember {
        mutableStateOf(
            initialScale?.ranges ?: listOf(
                GradeRange(70.0, 100.0, "A", "#4CAF50"),
                GradeRange(60.0, 69.9,  "B", "#8BC34A"),
                GradeRange(50.0, 59.9,  "C", "#FFC107"),
                GradeRange(40.0, 49.9,  "D", "#FF9800"),
                GradeRange(0.0,  39.9,  "F", "#F44336")
            )
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(max = 560.dp),
        title    = { Text(if (initialScale == null) "Create Grading Scale" else "Edit Grading Scale") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it },
                    label         = { Text("Scale Name") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )

                Text("Grade Ranges", fontWeight = FontWeight.SemiBold)

                LazyColumn(
                    Modifier.heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(ranges.indices.toList()) { idx ->
                        val range = ranges[idx]
                        GradeRangeEditor(
                            range     = range,
                            onChange  = { updated -> ranges = ranges.toMutableList().also { it[idx] = updated } },
                            onDelete  = { if (ranges.size > 1) ranges = ranges.toMutableList().also { it.removeAt(idx) } }
                        )
                    }
                }

                OutlinedButton(
                    onClick  = {
                        ranges = ranges + GradeRange(0.0, 0.0, "X", "#607D8B")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Range")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            GradingScale(
                                id     = initialScale?.id ?: FormattingUtils.generateId(),
                                name   = name,
                                ranges = ranges
                            )
                        )
                    }
                },
                enabled = name.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun GradeRangeEditor(
    range: GradeRange,
    onChange: (GradeRange) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value         = range.grade,
            onValueChange = { onChange(range.copy(grade = it.uppercase().take(2))) },
            label         = { Text("Grade") },
            singleLine    = true,
            modifier      = Modifier.width(70.dp)
        )
        OutlinedTextField(
            value         = range.minScore.toInt().toString(),
            onValueChange = { onChange(range.copy(minScore = it.toDoubleOrNull() ?: range.minScore)) },
            label         = { Text("Min") },
            singleLine    = true,
            modifier      = Modifier.weight(1f)
        )
        OutlinedTextField(
            value         = range.maxScore.toInt().toString(),
            onValueChange = { onChange(range.copy(maxScore = it.toDoubleOrNull() ?: range.maxScore)) },
            label         = { Text("Max") },
            singleLine    = true,
            modifier      = Modifier.weight(1f)
        )
        val color = try { AppColors.fromHex(range.colorCode) } catch (_: Exception) { Color.Gray }
        Surface(
            modifier = Modifier.size(36.dp),
            shape    = RoundedCornerShape(6.dp),
            color    = color,
            border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {}
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared Composables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        shape  = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary)
            HorizontalDivider()
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 13.sp)
        Text(value, fontWeight = FontWeight.Medium, fontSize = 13.sp)
    }
}
