package views

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import models.ExportFormat
import models.ProcessedFile
import models.StudentRecord
import models.computeStatistics
import state.AppState
import theme.gradeColor

@Composable
fun VaultView(state: AppState) {
    var deleteTarget by remember { mutableStateOf<String?>(null) }

    Row(modifier = Modifier.fillMaxSize()) {
        // Left panel: file list
        Column(
            modifier = Modifier.width(380.dp).fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("Vault", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("${state.savedFiles.size} saved files",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.55f))
                }
                // View mode toggle
                Row {
                    IconButton(onClick = { state.vaultViewMode = "grid" }) {
                        Icon(Icons.Default.GridView, "Grid",
                            tint = if (state.vaultViewMode == "grid") MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurface.copy(0.4f))
                    }
                    IconButton(onClick = { state.vaultViewMode = "list" }) {
                        Icon(Icons.Default.ViewList, "List",
                            tint = if (state.vaultViewMode == "list") MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurface.copy(0.4f))
                    }
                }
            }

            // Search
            OutlinedTextField(
                value = state.vaultSearchQuery,
                onValueChange = { state.vaultSearchQuery = it },
                placeholder = { Text("Search files…") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            // File cards
            if (state.filteredVaultFiles.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Inbox, null, modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(0.3f))
                        Text("No saved files", color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
                        Text("Process grades and save to Vault",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.3f),
                            textAlign = TextAlign.Center)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.filteredVaultFiles) { pf ->
                        VaultFileCard(
                            file     = pf,
                            selected = state.selectedVaultFile?.id == pf.id,
                            onClick  = { state.selectedVaultFile = pf },
                            onDelete = { deleteTarget = pf.id }
                        )
                    }
                }
            }
        }

        // Right panel: preview
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            if (state.selectedVaultFile == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onBackground.copy(0.25f))
                        Text("Select a file to preview",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(0.4f))
                    }
                }
            } else {
                VaultPreviewPanel(state.selectedVaultFile!!, state)
            }
        }
    }

    // Delete confirmation dialog
    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            icon = { Icon(Icons.Default.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete File") },
            text  = { Text("This will permanently remove the file from the Vault. Are you sure?") },
            confirmButton = {
                Button(
                    onClick = { state.deleteFromVault(deleteTarget!!); deleteTarget = null },
                    colors  = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }
}

// ─── Vault File Card ──────────────────────────────────────────────────────────
@Composable
private fun VaultFileCard(
    file: ProcessedFile,
    selected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(if (selected) 4.dp else 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                             else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // File icon
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (file.fileType == "CSV") Icons.Default.DataArray else Icons.Default.TableChart,
                    null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(file.fileName, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium)
                Text("${file.totalStudents} students  •  ${file.importDateStr}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.55f))
                Text("Scale: ${file.gradingScaleUsed}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, "Delete", modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.error.copy(0.7f))
            }
        }
    }
}

// ─── Vault Preview Panel ──────────────────────────────────────────────────────
@Composable
private fun VaultPreviewPanel(file: ProcessedFile, state: AppState) {
    var showExportMenu by remember { mutableStateOf(false) }
    val stats = remember(file) { file.students.computeStatistics() }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(file.fileName, style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Imported ${file.importDateStr}  •  Scale: ${file.gradingScaleUsed}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(0.55f))
            }
            Box {
                Button(onClick = { showExportMenu = true }) {
                    Icon(Icons.Default.FileDownload, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp)); Text("Export")
                    Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(16.dp))
                }
                DropdownMenu(expanded = showExportMenu, onDismissRequest = { showExportMenu = false }) {
                    ExportFormat.values().forEach { fmt ->
                        DropdownMenuItem(
                            text = { Text("Export as ${fmt.name}") },
                            onClick = {
                                state.exportStudents(fmt, file.students, file.fileName)
                                showExportMenu = false
                            }
                        )
                    }
                }
            }
        }

        // Stats
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(
                "Total" to file.totalStudents.toString(),
                "Average" to "%.1f".format(stats.average),
                "Highest" to "%.1f".format(stats.highest),
                "Pass Rate" to "%.1f%%".format(stats.passRate)
            ).forEach { (label, value) ->
                Card(modifier = Modifier.weight(1f), elevation = CardDefaults.cardElevation(1.dp)) {
                    Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(value, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleMedium)
                        Text(label, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                    }
                }
            }
        }

        // Grade distribution
        Card(elevation = CardDefaults.cardElevation(1.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Grade Distribution", fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                stats.gradeDistribution.entries.sortedBy { it.key }.forEach { (grade, count) ->
                    val pct = if (stats.totalStudents > 0) count.toFloat() / stats.totalStudents else 0f
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(grade, fontWeight = FontWeight.Bold,
                            color = gradeColor(grade), modifier = Modifier.width(24.dp))
                        LinearProgressIndicator(
                            progress = { pct },
                            modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = gradeColor(grade),
                            trackColor = gradeColor(grade).copy(alpha = 0.15f)
                        )
                        Text("$count (${"%.0f".format(pct * 100)}%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.6f),
                            modifier = Modifier.width(70.dp))
                    }
                }
            }
        }

        // Student table
        Card(modifier = Modifier.weight(1f), elevation = CardDefaults.cardElevation(1.dp)) {
            Column {
                // Table header
                Row(modifier = Modifier.fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 16.dp, vertical = 8.dp)) {
                    listOf("ID" to 0.7f, "Name" to 2f, "CA" to 0.7f, "Exam" to 0.7f, "Final" to 0.7f, "Grade" to 0.6f) .forEach { (h, w) ->
                        Text(h, modifier = Modifier.weight(w),
                            fontWeight = FontWeight.Bold, fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.65f))
                    }
                }
                HorizontalDivider()
                LazyColumn {
                    items(file.students) { s ->
                        Row(modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            @Composable fun C(t: String, w: Float, c: Color? = null) {
                                Text(t, modifier = Modifier.weight(w), fontSize = 12.sp,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    color = c ?: MaterialTheme.colorScheme.onSurface)
                            }
                            C(s.id, 0.7f)
                            C(s.name, 2f)
                            C("%.1f".format(s.caScore), 0.7f)
                            C("%.1f".format(s.examScore), 0.7f)
                            C("%.1f".format(s.finalScore), 0.7f,
                                if (s.finalScore >= 40) Color(0xFF4CAF50) else Color(0xFFF44336))
                            Text(s.grade, modifier = Modifier.weight(0.6f),
                                fontWeight = FontWeight.Bold, fontSize = 12.sp,
                                color = gradeColor(s.grade))
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.1f))
                    }
                }
            }
        }
    }
}
