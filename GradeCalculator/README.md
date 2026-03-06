# GradeCalculator — Desktop Application

A comprehensive Kotlin + Compose Desktop application for calculating and managing student grades.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 1.9.21 |
| UI Framework | Jetpack Compose Desktop 1.5.11 |
| Architecture | MVVM |
| Excel Processing | Apache POI 5.2.4 |
| CSV Handling | OpenCSV 5.8 |
| PDF Export | iText 7 |
| Google Integration | Google Sheets & Drive API v4 |
| Async | Kotlin Coroutines |
| Serialization | Kotlinx Serialization JSON |

---

## Project Structure

```
src/main/kotlin/com/gradecalculator/
├── Main.kt                          # App entry point, window, navigation
├── models/
│   ├── StudentRecord.kt             # Core data class + HOF demos
│   ├── GradingScale.kt              # Grading scale & ranges
│   └── ProcessedFile.kt             # Vault item / processed result
├── viewmodels/
│   └── AppViewModel.kt              # Shared state, actions, persistence
├── views/
│   ├── HomeView.kt                  # Import, table, process, export
│   ├── VaultView.kt                 # Saved files grid/list + detail
│   └── SettingsView.kt             # Scales, theme, curve settings
├── controllers/
│   ├── ImportController.kt          # Import pipeline orchestration
│   ├── ExportController.kt          # Excel, CSV, PDF, JSON export
│   └── GradeController.kt           # Grade calculation engine
├── services/
│   ├── FileParserService.kt         # Excel & CSV parsing
│   ├── GoogleSheetsService.kt       # Google Sheets API
│   ├── GoogleDriveService.kt        # Google Drive upload/download
│   └── PDFGenerationService.kt      # iText PDF report generation
├── utils/
│   ├── ValidationUtils.kt           # Score validation, outlier detection
│   └── FormattingUtils.kt           # Statistics, formatting helpers
└── theme/
    └── AppTheme.kt                  # Material3 light/dark themes
```

---

## Getting Started

### Prerequisites

- JDK 17+
- IntelliJ IDEA or Android Studio with Desktop Compose support

### Build & Run

```bash
# Clone the repository
git clone <repo-url>
cd GradeCalculator

# Run in development
./gradlew run

# Build distributable
./gradlew packageDistributionForCurrentOS
```

Outputs:
- **Windows**: `build/compose/binaries/main/msi/*.msi`
- **macOS**: `build/compose/binaries/main/dmg/*.dmg`
- **Linux**: `build/compose/binaries/main/deb/*.deb`

---

## Core Concepts

### Data Classes

#### `StudentRecord`
```kotlin
data class StudentRecord(
    val id: String,
    val name: String,
    var caScore: Double = 0.0,      // Max: 30
    var examScore: Double = 0.0,    // Max: 70
    var finalScore: Double = 0.0,
    var grade: String = "",
    var comment: String = "",
    val originalData: Map<String, String> = emptyMap()
)
```

**Function 1 — Validation:**
```kotlin
fun StudentRecord.validateScores(): ValidationResult
// Checks: caScore ≤ 30, examScore ≤ 70, total ≤ 100, no negatives
```

**Function 2 — Export Formatting:**
```kotlin
fun StudentRecord.formatForExport(format: ExportFormat): Map<String, Any>
// Returns format-specific map (EXCEL, CSV, PDF, JSON)
```

#### Higher-Order Functions
```kotlin
fun List<StudentRecord>.processWithRule(
    rule: (StudentRecord) -> Boolean,
    transformer: (StudentRecord) -> StudentRecord
): List<StudentRecord>

// Usage:
val passed = students.processWithRule(
    rule        = { it.finalScore >= 40 },
    transformer = { it.copy(grade = "PASS") }
)
```

### Grade Calculation Engine

```
CA Score (max 30) + Exam Score (max 70) = Final Score (max 100)
```

Default grading scale:
| Grade | Range |
|-------|-------|
| A | 70–100 |
| B | 60–69 |
| C | 50–59 |
| D | 40–49 |
| F | 0–39 |

---

## Features

### Home View
- 📁 File import: Excel (.xlsx/.xls), CSV, Google Sheets URL
- 🔍 Search bar with real-time filtering
- 📊 Sortable columns (click any column header)
- 📄 Pagination (20 rows per page)
- ⚠️ Visual indicators for invalid scores
- 🎯 Grade Curve calculator (+0–20 points)
- 💾 Export: Excel, CSV, PDF, JSON
- 🌙 Theme toggle (light/dark)

### Vault View
- 🗂️ Grid / List view toggle
- 🔍 Search saved files
- 📊 Statistics: average, pass rate, grade distribution
- 📁 Export from vault directly
- 🗑️ Delete with confirmation

### Settings View
- 🎨 Light/Dark theme toggle
- ⚙️ Create, edit, delete custom grading scales
- 📈 Set default grading scale
- 🔢 Grade range editor with color coding

---

## Google API Setup

1. Create a project in [Google Cloud Console](https://console.cloud.google.com)
2. Enable **Google Sheets API** and **Google Drive API**
3. Create a **Service Account** and download credentials JSON
4. Place credentials at `~/.gradecalculator/credentials.json`
5. For Sheets: share the target sheet with the service account email

---

## Supported File Formats

### Input
| Format | Extension | Auto-detection |
|--------|-----------|----------------|
| Excel  | .xlsx, .xls | ✅ |
| CSV    | .csv | ✅ |
| Google Sheets | URL | ✅ |

The parser auto-detects column names for:
- **Student ID**: `id`, `student id`, `reg no`, `matric`
- **Name**: `name`, `student name`, `fullname`
- **CA Score**: `ca`, `ca score`, `coursework`, `test`
- **Exam Score**: `exam`, `exam score`, `examination`

### Output
| Format | Contents |
|--------|---------|
| Excel  | Grades sheet + Statistics sheet |
| CSV    | Standard flat format |
| PDF    | Full report with statistics + grade distribution |
| JSON   | Complete serialized `ProcessedFile` |

---

## Vault Persistence

Processed files are automatically saved to:
- **Linux/macOS**: `~/.gradecalculator/vault.json`
- **Windows**: `C:\Users\<user>\.gradecalculator\vault.json`

The vault persists across sessions automatically.

---

## License

MIT License — see LICENSE file.
