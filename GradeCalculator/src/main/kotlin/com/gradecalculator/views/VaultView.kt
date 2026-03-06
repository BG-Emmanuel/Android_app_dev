package com.gradecalculator.views

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gradecalculator.models.ExportFormat
import com.gradecalculator.models.ProcessedFile
import com.gradecalculator.theme.AppColors
import com.gradecalculator.viewmodels.AppViewModel
import com.gradecalculator.viewmodels.VaultViewMode
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

// ─────────────────────────────────────────────────────────────────────────────
// Vault View
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun VaultView(vm: AppViewModel) {
    Row(Modifier.fillMaxSize()) {

        // ── Left panel: file list ─────────────────────────────────────────────
        Column(
            Modifier.width(320.dp).fillMaxHeight().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            VaultTopBar(vm)
            if (vm.filteredVault.isEmpty()) {
                EmptyVaultMessage()
            } else {
                when (vm.vaultViewMode) {
                    VaultViewMode.GRID -> VaultGrid(vm)
                    VaultViewMode.LIST -> VaultList(vm)
                }
            }
        }

        // ── Divider ───────────────────────────────────────────────────────────
        VerticalDivider(Modifier.fillMaxHeight())

        // ── Right panel: detail ───────────────────────────────────────────────
        Box(Modifier.weight(1f).fillMaxHeight().padding(16.dp)) {
            val selected = vm.selectedVaultFile
            if (selected == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f))
                        Spacer(Modifier.height(8.dp))
                        Text("Select a file to preview",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
                    }
                }
            } else {
                VaultDetailPanel(selected, vm)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Vault Top Bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun VaultTopBar(vm: AppViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text("Vault", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground)
            Row {
                IconButton(onClick = { vm.vaultViewMode = VaultViewMode.GRID },
                    colors = if (vm.vaultViewMode == VaultViewMode.GRID)
                        IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    else IconButtonDefaults.iconButtonColors()) {
                    Icon(Icons.Default.GridView, contentDescription = "Grid view")
                }
                IconButton(onClick = { vm.vaultViewMode = VaultViewMode.LIST },
                    colors = if (vm.vaultViewMode == VaultViewMode.LIST)
                        IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    else IconButtonDefaults.iconButtonColors()) {
                    Icon(Icons.Default.ViewList, contentDescription = "List view")
                }
            }
        }
        Text("${vm.filteredVault.size} saved files",
            fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
        OutlinedTextField(
            value         = vm.vaultSearch,
            onValueChange = { vm.vaultSearch = it },
            placeholder   = { Text("Search vault…") },
            leadingIcon   = { Icon(Icons.Default.Search, null) },
            singleLine    = true,
            modifier      = Modifier.fillMaxWidth(),
            shape         = RoundedCornerShape(8.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Grid and List views
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun VaultGrid(vm: AppViewModel) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        verticalArrangement   = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(vm.filteredVault) { file ->
            VaultFileCard(file, selected = file.id == vm.selectedVaultFile?.id) {
                vm.selectedVaultFile = file
            }
        }
    }
}

@Composable
private fun VaultList(vm: AppViewModel) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items(vm.filteredVault) { file ->
            VaultFileListItem(file, selected = file.id == vm.selectedVaultFile?.id) {
                vm.selectedVaultFile = file
            }
        }
    }
}

@Composable
private fun VaultFileCard(file: ProcessedFile, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(
            containerColor = if (selected)
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        ),
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(
                when (file.fileType.name) {
                    "EXCEL"        -> Icons.Default.GridOn
                    "GOOGLE_SHEET" -> Icons.Default.Cloud
                    else           -> Icons.Default.TableChart
                },
                contentDescription = null,
                tint     = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Text(file.fileName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(file.formattedDate(), fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            Text("${file.students.size} students", fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun VaultFileListItem(file: ProcessedFile, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape    = RoundedCornerShape(8.dp),
        colors   = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface
        ),
        border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Description, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(file.fileName, fontWeight = FontWeight.Medium, fontSize = 13.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${file.students.size} students  •  ${file.formattedDate()}",
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Detail Panel
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun VaultDetailPanel(file: ProcessedFile, vm: AppViewModel) {
    var showDeleteDialog  by remember { mutableStateOf(false) }
    var showExportMenu    by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {

        // ── Header ────────────────────────────────────────────────────────────
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.Top
        ) {
            Column(Modifier.weight(1f)) {
                Text(file.fileName, fontWeight = FontWeight.Bold, fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground)
                Text(file.formattedDate(), fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                Text("Scale: ${file.gradingScaleUsed}", fontSize = 12.sp,
                    color = AppColors.Violet)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Export
                Box {
                    Button(onClick = { showExportMenu = true }) {
                        Icon(Icons.Default.FileDownload, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Export")
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                    DropdownMenu(expanded = showExportMenu, onDismissRequest = { showExportMenu = false }) {
                        ExportFormat.values().forEach { fmt ->
                            DropdownMenuItem(
                                text    = { Text("${fmt.name}") },
                                onClick = {
                                    showExportMenu = false
                                    exportVaultFile(vm, file, fmt)
                                }
                            )
                        }
                    }
                }
                // Delete
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    colors  = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Delete")
                }
            }
        }

        // ── Statistics cards ──────────────────────────────────────────────────
        val stats = file.statistics
        if (stats != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Students",   "${stats.totalStudents}",                Modifier.weight(1f))
                StatCard("Average",    "%.1f".format(stats.average),            Modifier.weight(1f))
                StatCard("Pass Rate",  "${"%.0f".format(stats.passRate)}%",     Modifier.weight(1f))
                StatCard("Highest",    "%.1f".format(stats.highest),            Modifier.weight(1f))
                StatCard("Std Dev",    "%.2f".format(stats.standardDeviation),  Modifier.weight(1f))
            }

            // Grade distribution
            Text("Grade Distribution", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                stats.gradeDistribution.entries.sortedBy { it.key }.forEach { (grade, count) ->
                    val pct  = count.toFloat() / stats.totalStudents
                    val col  = AppColors.gradeColor(grade)
                    Card(
                        Modifier.weight(1f),
                        shape  = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = col.copy(alpha = 0.12f)),
                        border = BorderStroke(1.dp, col.copy(alpha = 0.4f))
                    ) {
                        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(grade, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = col)
                            Text("$count students", fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            Text("${"%.0f".format(pct * 100)}%", fontSize = 12.sp, color = col)
                        }
                    }
                }
            }
        }

        // ── Student list preview ──────────────────────────────────────────────
        Text("Students", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        Card(
            shape  = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column {
                // Header
                Row(
                    Modifier.fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                            .padding(12.dp, 8.dp)
                ) {
                    listOf("ID" to 1f, "Name" to 2f, "CA" to 0.8f, "Exam" to 0.8f, "Final" to 0.8f, "Grade" to 0.8f)
                        .forEach { (h, w) ->
                            Text(h, Modifier.weight(w), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                }
                HorizontalDivider()
                // Rows (show up to 50)
                file.students.take(50).forEachIndexed { idx, s ->
                    val bg = if (idx % 2 == 0) Color.Transparent
                             else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f)
                    Row(Modifier.fillMaxWidth().background(bg).padding(12.dp, 7.dp)) {
                        Text(s.id,                         Modifier.weight(1f),  fontSize = 12.sp, maxLines = 1)
                        Text(s.name,                       Modifier.weight(2f),  fontSize = 12.sp, maxLines = 1)
                        Text("%.1f".format(s.caScore),     Modifier.weight(0.8f), fontSize = 12.sp)
                        Text("%.1f".format(s.examScore),   Modifier.weight(0.8f), fontSize = 12.sp)
                        Text("%.1f".format(s.finalScore),  Modifier.weight(0.8f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text(s.grade,                      Modifier.weight(0.8f), fontSize = 12.sp,
                            color = AppColors.gradeColor(s.grade), fontWeight = FontWeight.Bold)
                    }
                    if (idx < file.students.lastIndex) HorizontalDivider(thickness = 0.5.dp)
                }
                if (file.students.size > 50) {
                    Text("… and ${file.students.size - 50} more students",
                        Modifier.padding(12.dp), fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
            }
        }
    }

    // Delete confirmation
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title   = { Text("Delete File") },
            text    = { Text("Are you sure you want to remove '${file.fileName}' from the vault? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = { vm.deleteFromVault(file.id); showDeleteDialog = false },
                    colors  = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { OutlinedButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape    = RoundedCornerShape(10.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun EmptyVaultMessage() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Inbox, contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f))
            Spacer(Modifier.height(8.dp))
            Text("Vault is empty", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
            Text("Process and save files from Home", fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun exportVaultFile(vm: AppViewModel, file: ProcessedFile, format: ExportFormat) {
    val ext     = format.name.lowercase()
    val chooser = JFileChooser().apply {
        dialogTitle  = "Export as ${format.name}"
        selectedFile = File("${file.fileName.substringBeforeLast('.')}.$ext")
        fileFilter   = FileNameExtensionFilter("${format.name} file", ext)
    }
    if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
        vm.exportFile(file, format, chooser.selectedFile)
    }
}
