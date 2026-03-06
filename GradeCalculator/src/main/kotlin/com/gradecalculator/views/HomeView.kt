package com.gradecalculator.views

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gradecalculator.models.ExportFormat
import com.gradecalculator.models.StudentRecord
import com.gradecalculator.theme.AppColors
import com.gradecalculator.viewmodels.AppViewModel
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

// ─────────────────────────────────────────────────────────────────────────────
// Home View
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun HomeView(vm: AppViewModel) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

        // ── Top bar ───────────────────────────────────────────────────────────
        HomeTopBar(vm)

        // ── Import section ────────────────────────────────────────────────────
        ImportSection(vm)

        // ── Warnings ─────────────────────────────────────────────────────────
        if (vm.importWarnings.isNotEmpty()) {
            WarningBanner(vm.importWarnings)
        }

        // ── Toolbar (search + controls) if we have data ───────────────────────
        if (vm.importedStudents.isNotEmpty()) {
            DataToolbar(vm)
            StudentTable(vm)
            PaginationBar(vm)
        }
    }

    // ── Loading overlay ───────────────────────────────────────────────────────
    if (vm.isLoading) LoadingOverlay(vm.loadingMessage)
}

// ─────────────────────────────────────────────────────────────────────────────
// Top Bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HomeTopBar(vm: AppViewModel) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Column {
            Text("Home", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground)
            Text(
                if (vm.importedStudents.isNotEmpty()) "${vm.importedStudents.size} students loaded"
                else "Import a file to get started",
                fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Theme toggle
            IconButton(onClick = vm::toggleTheme) {
                Icon(
                    if (vm.isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Toggle theme",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            // Load sample data
            OutlinedButton(onClick = vm::loadSampleData) {
                Icon(Icons.Default.Science, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Demo Data")
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Import Section (drag-drop + Google Sheets)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ImportSection(vm: AppViewModel) {
    var showGoogleSheetDialog by remember { mutableStateOf(false) }

    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Import Data", fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)

            // Drag-drop zone
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(
                        2.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        RoundedCornerShape(8.dp)
                    )
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.04f))
                    .clickable { openFileChooser(vm) },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(6.dp))
                    Text("Click to browse or drag & drop",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    Text("Supports .xlsx, .xls, .csv",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Browse button
                Button(
                    onClick = { openFileChooser(vm) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Browse File")
                }
                // Google Sheets button
                OutlinedButton(
                    onClick = { showGoogleSheetDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Link, contentDescription = null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Google Sheets")
                }
            }
        }
    }

    if (showGoogleSheetDialog) {
        GoogleSheetsDialog(
            url       = vm.googleSheetUrl,
            onUrlChange = { vm.googleSheetUrl = it },
            onImport  = { vm.importGoogleSheet(it); showGoogleSheetDialog = false },
            onDismiss = { showGoogleSheetDialog = false }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Data Toolbar (search + process + export)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DataToolbar(vm: AppViewModel) {
    var showExportMenu    by remember { mutableStateOf(false) }
    var showCurveDialog   by remember { mutableStateOf(false) }

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        // Search
        OutlinedTextField(
            value         = vm.searchQuery,
            onValueChange = { vm.searchQuery = it; vm.currentPage = 0 },
            placeholder   = { Text("Search students…") },
            leadingIcon   = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon  = {
                if (vm.searchQuery.isNotEmpty())
                    IconButton(onClick = { vm.searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = null)
                    }
            },
            singleLine    = true,
            modifier      = Modifier.weight(1f),
            shape         = RoundedCornerShape(8.dp)
        )

        // Process Grades
        Button(onClick = vm::processGrades) {
            Icon(Icons.Default.Calculate, contentDescription = null, Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Process Grades")
        }

        // Curve
        OutlinedButton(onClick = { showCurveDialog = true }) {
            Icon(Icons.Default.TrendingUp, contentDescription = null, Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Curve")
        }

        // Save to vault
        Button(
            onClick  = vm::saveToVault,
            colors   = ButtonDefaults.buttonColors(containerColor = AppColors.Violet)
        ) {
            Icon(Icons.Default.Save, contentDescription = null, Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Save to Vault")
        }

        // Export dropdown
        Box {
            OutlinedButton(onClick = { showExportMenu = true }) {
                Icon(Icons.Default.FileDownload, contentDescription = null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Export")
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = showExportMenu, onDismissRequest = { showExportMenu = false }) {
                ExportFormat.values().forEach { format ->
                    DropdownMenuItem(
                        text    = { Text("Export as ${format.name}") },
                        onClick = {
                            showExportMenu = false
                            vm.currentFile?.let { pf -> exportWithChooser(vm, pf, format) }
                        },
                        leadingIcon = {
                            Icon(when (format) {
                                ExportFormat.EXCEL -> Icons.Default.GridOn
                                ExportFormat.CSV   -> Icons.Default.TableChart
                                ExportFormat.PDF   -> Icons.Default.PictureAsPdf
                                ExportFormat.JSON  -> Icons.Default.Code
                            }, contentDescription = null)
                        }
                    )
                }
            }
        }

        // Clear
        IconButton(onClick = vm::clearCurrentImport) {
            Icon(Icons.Default.Delete, contentDescription = "Clear", tint = MaterialTheme.colorScheme.error)
        }
    }

    if (showCurveDialog) {
        GradeCurveDialog(
            enabled     = vm.curveEnabled,
            curvePoints = vm.curvePoints,
            onToggle    = { vm.curveEnabled = it },
            onPointsChange = { vm.curvePoints = it },
            onApply     = { vm.processGrades(); showCurveDialog = false },
            onDismiss   = { showCurveDialog = false }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Student Table
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StudentTable(vm: AppViewModel) {
    Card(
        Modifier.fillMaxWidth(),
        shape  = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            // Header row
            TableHeader(vm)
            HorizontalDivider()

            // Data rows (lazy for performance)
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                itemsIndexed(vm.pagedStudents) { idx, student ->
                    StudentRow(student, idx)
                    if (idx < vm.pagedStudents.lastIndex) HorizontalDivider(thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
private fun TableHeader(vm: AppViewModel) {
    val columns = listOf("id" to "ID", "name" to "Name", "caScore" to "CA Score",
        "examScore" to "Exam Score", "finalScore" to "Final Score", "grade" to "Grade")
    Row(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        columns.forEachIndexed { idx, (col, label) ->
            val weight = if (idx == 1) 2f else 1f
            Row(
                Modifier.weight(weight).clickable { vm.toggleSort(col) }.padding(12.dp, 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface)
                if (vm.sortColumn == col) {
                    Icon(
                        if (vm.sortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = null, modifier = Modifier.size(14.dp).padding(start = 2.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        // Status column
        Text("Status", modifier = Modifier.width(90.dp).padding(12.dp, 10.dp),
            fontWeight = FontWeight.Bold, fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun StudentRow(student: StudentRecord, idx: Int) {
    val bg = if (idx % 2 == 0) Color.Transparent
             else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f)
    val invalid = !student.validateScores().isValid

    Row(
        Modifier.fillMaxWidth().background(bg).padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(student.id,    Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 8.dp),
            fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(student.name, Modifier.weight(2f).padding(horizontal = 12.dp, vertical = 8.dp),
            fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        ScoreCell(student.caScore,    1f, invalid && student.caScore > 30)
        ScoreCell(student.examScore,  1f, invalid && student.examScore > 70)
        ScoreCell(student.finalScore, 1f, false)
        GradeChip(student.grade, Modifier.weight(1f).padding(horizontal = 12.dp))
        StatusChip(student.getGradeStatus(), Modifier.width(90.dp).padding(end = 12.dp))
    }
}

@Composable
private fun ScoreCell(score: Double, weight: Float, highlight: Boolean) {
    Text(
        "%.1f".format(score),
        modifier = Modifier.weight(weight).padding(horizontal = 12.dp, vertical = 8.dp),
        fontSize = 13.sp,
        color = if (highlight) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun GradeChip(grade: String, modifier: Modifier = Modifier) {
    val color = AppColors.gradeColor(grade)
    Box(modifier) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = color.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
        ) {
            Text(grade.ifBlank { "—" }, modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun StatusChip(status: String, modifier: Modifier = Modifier) {
    val color = when (status) {
        "Excellent" -> AppColors.GradeA
        "Very Good" -> AppColors.GradeB
        "Good"      -> AppColors.GradeC
        "Pass"      -> AppColors.GradeD
        else        -> AppColors.GradeF
    }
    Box(modifier, contentAlignment = Alignment.CenterStart) {
        Surface(shape = RoundedCornerShape(12.dp), color = color.copy(alpha = 0.12f)) {
            Text(status, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                fontSize = 11.sp, color = color)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Pagination
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PaginationBar(vm: AppViewModel) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        IconButton(onClick = { if (vm.currentPage > 0) vm.currentPage-- },
            enabled = vm.currentPage > 0) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous")
        }
        Text("Page ${vm.currentPage + 1} of ${vm.totalPages}",
            Modifier.padding(horizontal = 12.dp), fontSize = 13.sp)
        IconButton(onClick = { if (vm.currentPage < vm.totalPages - 1) vm.currentPage++ },
            enabled = vm.currentPage < vm.totalPages - 1) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Next")
        }
        Spacer(Modifier.width(16.dp))
        Text("(${vm.filteredStudents.size} results)", fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Warning Banner
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WarningBanner(warnings: List<String>) {
    Card(
        Modifier.fillMaxWidth(),
        shape  = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Default.Warning, contentDescription = null,
                tint = Color(0xFFE65100), modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Column {
                Text("Import Warnings", fontWeight = FontWeight.Bold,
                    color = Color(0xFFE65100), fontSize = 13.sp)
                warnings.take(5).forEach { Text("• $it", fontSize = 12.sp, color = Color(0xFF5D4037)) }
                if (warnings.size > 5)
                    Text("+ ${warnings.size - 5} more warnings…", fontSize = 12.sp, color = Color(0xFF5D4037))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Loading Overlay
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LoadingOverlay(message: String) {
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(
                Modifier.padding(32.dp),
                horizontalAlignment   = Alignment.CenterHorizontally,
                verticalArrangement   = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator()
                Text(message, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Dialogs
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GoogleSheetsDialog(
    url: String,
    onUrlChange: (String) -> Unit,
    onImport: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import from Google Sheets") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Paste the Google Sheets URL below. Make sure the sheet is publicly accessible or credentials are configured in Settings.")
                OutlinedTextField(
                    value         = url,
                    onValueChange = onUrlChange,
                    placeholder   = { Text("https://docs.google.com/spreadsheets/d/…") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { if (url.isNotBlank()) onImport(url) }, enabled = url.isNotBlank()) {
                Text("Import")
            }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun GradeCurveDialog(
    enabled: Boolean,
    curvePoints: Double,
    onToggle: (Boolean) -> Unit,
    onPointsChange: (Double) -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Grade Curve") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = enabled, onCheckedChange = onToggle)
                    Spacer(Modifier.width(8.dp))
                    Text("Enable grade curve")
                }
                if (enabled) {
                    Text("Add points to all final scores (capped at 100)")
                    Slider(
                        value         = curvePoints.toFloat(),
                        onValueChange = { onPointsChange(it.toDouble()) },
                        valueRange    = 0f..20f,
                        steps         = 19
                    )
                    Text("Curve: +${"%.0f".format(curvePoints)} points",
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        confirmButton = { Button(onClick = onApply) { Text("Apply") } },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// File System helpers (Swing dialogs)
// ─────────────────────────────────────────────────────────────────────────────

private fun openFileChooser(vm: AppViewModel) {
    val chooser = JFileChooser().apply {
        dialogTitle = "Select Grade File"
        fileFilter  = FileNameExtensionFilter("Excel & CSV files", "xlsx", "xls", "csv")
    }
    if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
        vm.importFile(chooser.selectedFile)
    }
}

private fun exportWithChooser(vm: AppViewModel, processedFile: com.gradecalculator.models.ProcessedFile, format: ExportFormat) {
    val ext = format.name.lowercase()
    val chooser = JFileChooser().apply {
        dialogTitle        = "Export as ${format.name}"
        selectedFile       = File("${processedFile.fileName.substringBeforeLast('.')}.$ext")
        fileFilter         = FileNameExtensionFilter("${format.name} file", ext)
    }
    if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
        vm.exportFile(processedFile, format, chooser.selectedFile)
    }
}
