# Grade Calculator Pro
### Kotlin Compose Desktop Application — Complete Setup Guide

---

## PREREQUISITES — Install These First

| # | Tool | Version | Download |
|---|------|---------|----------|
| 1 | **JDK (Java Development Kit)** | 17 or 21 LTS | https://adoptium.net |
| 2 | **IntelliJ IDEA** | Community or Ultimate | https://www.jetbrains.com/idea/download |
| 3 | **Git** | Latest | https://git-scm.com |

> ⚠️ **Important:** This is a **Kotlin Desktop** project, NOT Android.
> Use **IntelliJ IDEA**, not Android Studio.
> Compose Desktop does NOT run in Android Studio without special plugins.

---

## FOLDER STRUCTURE

```
GradeCalculator/
├── build.gradle.kts               ← Gradle build + all dependencies
├── settings.gradle.kts            ← Project name
└── src/
    └── main/
        ├── kotlin/
        │   ├── Main.kt            ← App entry point (main function)
        │   ├── models/
        │   │   └── Models.kt      ← All data classes + extension functions
        │   ├── state/
        │   │   └── AppState.kt    ← Central state manager (ViewModel)
        │   ├── theme/
        │   │   └── AppTheme.kt    ← Colours, typography, Material3 theme
        │   ├── views/
        │   │   ├── MainWindow.kt  ← Sidebar navigation + notification bar
        │   │   ├── HomeView.kt    ← Import, process, preview table
        │   │   ├── VaultView.kt   ← Saved files viewer
        │   │   └── SettingsView.kt← Grading scales + preferences
        │   ├── services/
        │   │   ├── FileParserService.kt ← CSV + Excel file parsing
        │   │   └── ExportService.kt     ← Excel, CSV, PDF, JSON export
        │   └── utils/
        │       └── VaultManager.kt      ← Local JSON vault persistence
        └── resources/
            └── icons/
                └── app_icon.png   ← (optional) app icon
```

---

## STEP-BY-STEP SETUP IN INTELLIJ IDEA

### Step 1 — Create the Project Folder

Create the exact folder structure shown above and copy all the `.kt` files into the corresponding locations.

### Step 2 — Open in IntelliJ IDEA

1. Launch IntelliJ IDEA
2. Click **"Open"** (NOT "New Project")
3. Browse to the `GradeCalculator` folder
4. Select the folder and click **OK**
5. IntelliJ will detect the `build.gradle.kts` file and ask to **"Trust this project"** — click **Trust**
6. Wait for Gradle to sync (bottom status bar shows progress — this may take 2-5 minutes on first run as it downloads all dependencies)

### Step 3 — Verify Gradle Sync

After sync, confirm in the Gradle tool window (View → Tool Windows → Gradle) that you see:
- `Tasks > application > run`
- No red error icons

### Step 4 — Run the Application

**Option A — From IntelliJ:**
1. Open `src/main/kotlin/Main.kt`
2. Click the green ▶ play button beside `fun main()`
3. Or use the Run menu → Run 'MainKt'

**Option B — From Terminal:**
```bash
cd GradeCalculator
./gradlew run          # Mac/Linux
gradlew.bat run        # Windows
```

### Step 5 — Build a Standalone Executable (optional)

```bash
./gradlew packageDistributionForCurrentOS
```
The installer will appear in `build/compose/binaries/`.

---

## DEPENDENCIES (auto-downloaded by Gradle)

| Library | Purpose |
|---------|---------|
| `compose.desktop.currentOs` | Compose Desktop UI framework |
| `compose.material3` | Material Design 3 components |
| `org.apache.poi:poi-ooxml:5.2.5` | Read/write Excel .xlsx files |
| `com.opencsv:opencsv:5.9` | Parse CSV files |
| `kotlinx-serialization-json:1.6.3` | Save/load vault (JSON) |
| `kotlinx-coroutines-swing:1.8.1` | Async file import/export |
| `org.apache.pdfbox:pdfbox:3.0.2` | Generate PDF reports |
| `slf4j-simple:2.0.13` | Suppress noisy log output |

---

## HOW TO USE THE APP

### Importing a File

Your CSV or Excel file must have these columns (names are flexible — the parser auto-detects):

| Column | Accepted Names |
|--------|---------------|
| Student ID | `id`, `student_id`, `no`, `number`, `sn` |
| Student Name | `name`, `student_name`, `full_name` |
| CA Score | `ca`, `ca_score`, `continuous`, `coursework`, `test` |
| Exam Score | `exam`, `exam_score`, `examination` |

#### Sample CSV Format (`sample_grades.csv`):
```csv
ID,Name,CA Score,Exam Score
001,Alice Johnson,25,65
002,Bob Smith,18,52
003,Carol Williams,30,70
004,David Brown,10,35
005,Eva Martinez,22,48
```

### Processing Grades

1. Click **"Import File"** on the Home screen
2. Select your `.xlsx`, `.xls`, or `.csv` file
3. Preview loads instantly — check the table for correct data
4. Click **"Process Grades"** — scores are validated, capped, and graded
5. See stats (Average, Pass Rate, etc.) appear above the table
6. Click **"Save to Vault"** to persist the results

### Grading Scale (CA max: 30, Exam max: 70)

| Final Score | Grade | Status |
|-------------|-------|--------|
| 70 – 100 | A | Excellent |
| 60 – 69 | B | Very Good |
| 50 – 59 | C | Good |
| 40 – 49 | D | Pass |
| 0 – 39 | F | Fail |

### Exporting Results

After processing, click **"Export"** and choose:
- **EXCEL** — Multi-sheet workbook (Grades + Statistics)
- **CSV** — Simple comma-separated file
- **PDF** — Formatted grade report with stats
- **JSON** — Machine-readable format for APIs

Files are saved to: `~/GradeCalculator/` (configurable in Settings)

---

## GITHUB WORKFLOW (for presentation)

### Recommended Branch Strategy

```
main          ← stable, production-ready code
develop       ← integration branch
feature/*     ← individual feature branches
hotfix/*      ← urgent fixes
```

### Suggested Commit Log Structure

```bash
git init
git add .
git commit -m "chore: initial project structure"

git checkout -b feature/models-and-data-classes
# Add Models.kt
git add src/main/kotlin/models/Models.kt
git commit -m "feat: add StudentRecord, GradingScale, and ProcessedFile data classes"
git commit -m "feat: implement validateScores() extension function"
git commit -m "feat: implement processWithRule() higher-order function"
git commit -m "feat: implement computeStatistics() extension function"
git checkout develop && git merge feature/models-and-data-classes

git checkout -b feature/file-parser
git commit -m "feat: implement CSV parser with auto column detection"
git commit -m "feat: implement Excel parser using Apache POI"
git commit -m "test: add unit tests for FileParserService"
git checkout develop && git merge feature/file-parser

git checkout -b feature/grade-engine
git commit -m "feat: implement grade calculation engine in AppState"
git commit -m "feat: add score validation and capping (CA≤30, Exam≤70)"
git checkout develop && git merge feature/grade-engine

git checkout -b feature/ui-home-view
git commit -m "feat: build HomeView with import drop zone"
git commit -m "feat: add sortable, paginated student table"
git commit -m "feat: add real-time search with debounce"
git commit -m "feat: add statistics cards row"
git checkout develop && git merge feature/ui-home-view

git checkout -b feature/ui-vault-view
git commit -m "feat: implement VaultView with file cards"
git commit -m "feat: add grade distribution progress bars"
git commit -m "feat: add vault file preview panel"
git checkout develop && git merge feature/ui-vault-view

git checkout -b feature/export
git commit -m "feat: implement Excel export with Apache POI"
git commit -m "feat: implement PDF export using PDFBox"
git commit -m "feat: implement CSV and JSON export"
git checkout develop && git merge feature/export

git checkout -b feature/settings-and-theme
git commit -m "feat: implement Settings view with grading scale editor"
git commit -m "feat: add dark/light theme toggle"
git checkout develop && git merge feature/settings-and-theme

git checkout main && git merge develop
git tag v1.0.0
```

### PR Description Template

```markdown
## Summary
Brief description of what this PR implements.

## Changes
- Added `FileParserService` for CSV and Excel parsing
- Auto-detects column names (CA, Exam, ID, Name)
- Handles malformed data gracefully

## Testing
- [ ] Imported 100-row CSV successfully
- [ ] Imported multi-sheet .xlsx successfully
- [ ] Edge case: empty rows ignored
- [ ] Edge case: missing CA column shows error

## Screenshots
[Attach screenshot of the feature]
```

### Target GitHub Stats for Presentation

| Metric | Target |
|--------|--------|
| Total Commits | 25–40 |
| Pull Requests | 6–10 |
| Code Reviews | 4–8 (reviewer comments) |
| Merges | 6–10 |
| Branches | 8–12 |
| Contributors | 2–4 (use teammates) |

---

## PRESENTATION CHECKLIST

- [ ] **Live Demo:** Run `./gradlew run` and import `sample_grades.csv`
- [ ] **Show Grade Processing:** Click Process Grades, show the stats
- [ ] **Show Export:** Export to PDF and open the generated file
- [ ] **Show Vault:** Save to Vault, switch to Vault tab, preview
- [ ] **Show Dark Mode:** Toggle in sidebar or Settings
- [ ] **Show Settings:** Demonstrate grading scale switching
- [ ] **Show GitHub:** Open repo, navigate Commits, PRs, and Insights

---

## TROUBLESHOOTING

| Problem | Fix |
|---------|-----|
| Gradle sync fails | Check JDK version: `java -version` must be 17+ |
| "SDK not found" | File → Project Structure → SDK → add JDK 17 path |
| `ClassNotFoundException` | Re-run Gradle sync (Shift+F5 in Gradle panel) |
| UI not rendering | Ensure IntelliJ version ≥ 2023.1 |
| CSV parse error | Ensure first row has headers (ID, Name, CA, Exam) |
| PDF not opening | Check `~/GradeCalculator/` folder for the file |

---

*Built with Kotlin + Jetpack Compose Desktop · Apache POI · PDFBox · OpenCSV*
