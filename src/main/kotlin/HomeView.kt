package views

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import models.*
import state.AppState
import theme.gradeColor
import theme.statusColor
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
fun HomeView(state: AppState) {
    var showExportMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Grade Calculator", style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold)
                Text(
                    if (state.importedStudents.isEmpty()) "Import a file to get started"
                    else "${state.importedStudents.size} students loaded  •  ${state.currentFileName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.importedStudents.isNotEmpty()) {
                    Button(
                        onClick = { state.processGrades() },
                        enabled = !state.isProcessed && !state.isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Calculate, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Process Grades")
                    }
                    if (state.isProcessed) {
                        Button(
                            onClick = { state.saveToVault() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Save to Vault")
                        }
                        Box {
                            OutlinedButton(onClick = { showExportMenu = true }) {
                                Icon(Icons.Default.FileDownload, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Export")
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(16.dp))
                            }
                            DropdownMenu(
                                expanded = showExportMenu,
                                onDismissRequest = { showExportMenu = false }
                            ) {
                                ExportFormat.values().forEach { fmt ->
                                    DropdownMenuItem(
                                        text = { Text("Export as ${fmt.name}") },
                                        leadingIcon = {
                                            Icon(
                                                when (fmt) {
                                                    ExportFormat.EXCEL -> Icons.Default.TableChart
                                                    ExportFormat.CSV   -> Icons.Default.DataArray
                                                    ExportFormat.PDF   -> Icons.Default.PictureAsPdf
                                                    ExportFormat.JSON  -> Icons.Default.Code
                                                }, null
                                            )
                                        },
                                        onClick = {
                                            state.exportStudents(fmt)
                                            showExportMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Loading
        AnimatedVisibility(state.isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        // Stats cards (only when processed)
        AnimatedVisibility(state.isProcessed && state.classStats != null) {
            state.classStats?.let { StatsRow(it) }
        }

        if (state.importedStudents.isEmpty()) {
            // Import drop zone
            ImportDropZone(onImport = { file -> state.importFile(file) })
        } else {
            // Search + table
            SearchAndTable(state)
        }
    }
}

// ─── Import Drop Zone ─────────────────────────────────────────────────────────
@Composable
private fun ImportDropZone(onImport: (java.io.File) -> Unit) {
    var hovering by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (hovering) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .border(
                width = 2.dp,
                color = if (hovering) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable {
                val chooser = JFileChooser().apply {
                    dialogTitle = "Select Grade File"
                    fileFilter  = FileNameExtensionFilter("Excel & CSV Files", "xlsx", "xls", "csv")
                    isAcceptAllFileFilterUsed = false
                }
                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    onImport(chooser.selectedFile)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CloudUpload, null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary)
            }
            Text("Click to import a file", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold)
            Text("Supported formats: Excel (.xlsx, .xls) and CSV (.csv)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f))
            Text("Columns auto-detected: ID, Name, CA Score, Exam Score",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
        }
    }
}

// ─── Statistics Row ───────────────────────────────────────────────────────────
@Composable
private fun StatsRow(stats: ClassStatistics) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        listOf(
            Triple("Students",   stats.totalStudents.toString(),         MaterialTheme.colorScheme.primary),
            Triple("Average",    "%.1f".format(stats.average),           MaterialTheme.colorScheme.secondary),
            Triple("Highest",    "%.1f".format(stats.highest),           Color(0xFF4CAF50)),
            Triple("Lowest",     "%.1f".format(stats.lowest),            Color(0xFFF44336)),
            Triple("Pass Rate",  "%.1f%%".format(stats.passRate),        Color(0xFF2196F3)),
            Triple("Std Dev",    "%.2f".format(stats.standardDeviation), MaterialTheme.colorScheme.outline)
        ).forEach { (label, value, colour) ->
            StatCard(label, value, colour, Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, color: Color, modifier: Modifier) {
    Card(modifier = modifier, elevation = CardDefaults.cardElevation(2.dp)) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center)
        }
    }
}

// ─── Search + Sortable Table ──────────────────────────────────────────────────
@Composable
private fun SearchAndTable(state: AppState) {
    // Search bar
    OutlinedTextField(
        value = state.searchQuery,
        onValueChange = { state.searchQuery = it; state.currentPage = 0 },
        placeholder = { Text("Search by name or ID…") },
        leadingIcon = { Icon(Icons.Default.Search, null) },
        trailingIcon = {
            if (state.searchQuery.isNotEmpty())
                IconButton(onClick = { state.searchQuery = ""; state.currentPage = 0 }) {
                    Icon(Icons.Default.Clear, null)
                }
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
    )

    // Table
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column {
            // Header
            TableHeader(state)
            HorizontalDivider()

            // Rows
            if (state.paginatedStudents.isEmpty()) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("No results", color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(state.paginatedStudents) { student ->
                        StudentRow(student)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    }
                }
            }

            // Pagination footer
            HorizontalDivider()
            PaginationBar(state)
        }
    }
}

@Composable
private fun TableHeader(state: AppState) {
    @Composable
    fun SortableHeader(label: String, column: SortColumn, w: Float) {
        Row(
            modifier = Modifier
                .clickable { state.toggleSort(column) }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            if (state.sortColumn == column) {
                Icon(
                    if (state.sortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    null, modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SortableHeader("ID",    SortColumn.ID,          0.7f)
        SortableHeader("Name",  SortColumn.NAME,         2f)
        SortableHeader("CA",    SortColumn.CA_SCORE,     0.7f)
        SortableHeader("Exam",  SortColumn.EXAM_SCORE,   0.7f)
        SortableHeader("Final", SortColumn.FINAL_SCORE,  0.7f)
        SortableHeader("Grade", SortColumn.GRADE,        0.6f)
        Box(modifier = Modifier.weight(1f).padding(12.dp)) {
            Text("Status", fontWeight = FontWeight.Bold, fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun StudentRow(s: StudentRecord) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 0.dp, vertical = 0.dp)
            .background(MaterialTheme.colorScheme.surface),
        verticalAlignment = Alignment.CenterVertically
    ) {
        @Composable fun Cell(text: String, weight: Float, align: TextAlign = TextAlign.Start, color: Color? = null) {
            Text(
                text,
                modifier = Modifier.weight(weight).padding(horizontal = 12.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodySmall,
                textAlign = align,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = color ?: MaterialTheme.colorScheme.onSurface
            )
        }

        Cell(s.id,                         0.7f)
        Cell(s.name,                       2f)
        Cell("%.1f".format(s.caScore),     0.7f, TextAlign.Center)
        Cell("%.1f".format(s.examScore),   0.7f, TextAlign.Center)
        Cell("%.1f".format(s.finalScore),  0.7f, TextAlign.Center,
            if (s.finalScore >= 40) Color(0xFF4CAF50) else Color(0xFFF44336))

        // Grade badge
        Box(modifier = Modifier.weight(0.6f).padding(horizontal = 8.dp, vertical = 6.dp)) {
            if (s.grade.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(gradeColor(s.grade).copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(s.grade, fontWeight = FontWeight.Bold,
                        color = gradeColor(s.grade), fontSize = 11.sp)
                }
            }
        }
        // Status badge
        Box(modifier = Modifier.weight(1f).padding(horizontal = 8.dp, vertical = 6.dp)) {
            if (s.status.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusColor(s.status).copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(s.status, color = statusColor(s.status), fontSize = 11.sp,
                        fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun PaginationBar(state: AppState) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Showing ${state.currentPage * state.pageSize + 1}–${minOf((state.currentPage + 1) * state.pageSize, state.filteredStudents.size)} of ${state.filteredStudents.size}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(0.55f)
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(
                onClick = { if (state.currentPage > 0) state.currentPage-- },
                enabled = state.currentPage > 0
            ) { Icon(Icons.Default.ChevronLeft, "Previous") }
            Text("Page ${state.currentPage + 1} / ${state.totalPages}",
                style = MaterialTheme.typography.bodySmall)
            IconButton(
                onClick = { if (state.currentPage < state.totalPages - 1) state.currentPage++ },
                enabled = state.currentPage < state.totalPages - 1
            ) { Icon(Icons.Default.ChevronRight, "Next") }
        }
    }
}
