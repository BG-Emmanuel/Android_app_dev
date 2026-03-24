# 📚 Lambda Functions Guide - Grade Calculator App

This document catalogs all lambda functions used in the Grade Calculator application, explaining their patterns, usage, and benefits.

---

## 📋 Table of Contents

1. [Model Layer Lambdas (StudentRecord.kt)](#model-layer-lambdas)
2. [UI Layer Lambdas (HomeView.kt)](#ui-layer-lambdas)
3. [Lambda Pattern Reference](#lambda-pattern-reference)
4. [Key Concepts & Benefits](#key-concepts--benefits)

---

## Model Layer Lambdas

### **Model Layer Lambdas (StudentRecord.kt)**

The StudentRecord model file contains **15+ lambda function implementations** that demonstrate functional programming patterns in Kotlin:

---

### ✓ **LAMBDA #1: `processWithRule`** - Higher-Order Function Pattern
**Location:** `StudentRecord.kt` line ~78

```kotlin
fun List<StudentRecord>.processWithRule(
    rule: (StudentRecord) -> Boolean,        // Lambda parameter 1: predicate
    transformer: (StudentRecord) -> StudentRecord  // Lambda parameter 2: transformer
): List<StudentRecord> = this.filter(rule).map(transformer)
```

**Pattern:** Takes **two lambdas as parameters** and applies them in sequence.

**Usage Example:**
```kotlin
val passedStudent = students.processWithRule(
    rule = { it.finalScore >= 40 },              // Filters passing students
    transformer = { it.copy(grade = "PASS") }   // Marks them as PASS
)
```

**Explanation:** 
- Higher-order function: a function that takes functions as parameters
- Demonstrates composition: `filter(rule).map(transformer)`
- Reusable for any filtering + transformation logic

---

### ✓ **LAMBDA #2: `calculatePassRate`** - Count with Predicate Lambda
**Location:** `StudentRecord.kt` line ~88

```kotlin
fun List<StudentRecord>.calculatePassRate(): Double {
    if (isEmpty()) return 0.0
    return count { it.hasPassed() }.toDouble() / size * 100.0
}
```

**Pattern:** Inline lambda in `.count { }` extension function.

**Key Feature:** Closure - the lambda captures context from outer scope.

**Explanation:**
- `count { it.hasPassed() }` - counts elements where lambda returns true
- Lambda `{ it.hasPassed() }` is inline (not assigned to variable)
- Functional programming: describes "what to count" not "how to count"

---

### ✓ **LAMBDA #3: `topPerformers`** - Sorting with Lambda
**Location:** `StudentRecord.kt` line ~95

```kotlin
fun List<StudentRecord>.topPerformers(n: Int = 5): List<StudentRecord> =
    sortedByDescending { it.finalScore }.take(n)
```

**Pattern:** Lambda extracts comparison key from complex object.

**Explanation:**
- `{ it.finalScore }` - lambda extracts the value to sort by
- Cleaner than manual comparator
- Declarative: reads as "sort by final score, descending"

---

### ✓ **LAMBDA #4: `detectDuplicates`** - ForEach with Side Effects
**Location:** `StudentRecord.kt` line ~101

```kotlin
fun List<StudentRecord>.detectDuplicates(): List<String> {
    val seen = mutableSetOf<String>()
    val duplicates = mutableListOf<String>()
    forEach { student ->  // Lambda parameter: student
        if (!seen.add(student.id)) duplicates.add(student.id)
    }
    return duplicates
}
```

**Pattern:** Lambda with mutable state capture (closure).

**Explanation:**
- Loops with side effects (modifies `seen` & `duplicates` sets)
- Lambda captures mutable variables from outer scope
- Functional programming with stateful operations

---

### ✓ **LAMBDA #5: `withRankings`** - MapIndexed with Index-Aware Lambda
**Location:** `StudentRecord.kt` line ~112

```kotlin
fun List<StudentRecord>.withRankings(): Map<String, Int> {
    return sortedByDescending { it.finalScore }
        .mapIndexed { index, student -> student.id to (index + 1) }
        .toMap()
}
```

**Pattern:** `mapIndexed` lambda receives both index and element.

**Explanation:**
- `{ index, student -> ... }` - destructs both index and element
- Creates pairs using `to` operator
- Converts list of students to rank map (ID → Rank)

---

### ✓ **LAMBDA #6: `calculateClassStatistics`** - Complex Lambda Chain
**Location:** `StudentRecord.kt` line ~120

Demonstrates **FOUR different lambdas** used together:

```kotlin
val scores = map { it.finalScore }.sorted()           // LAMBDA: extract score
val variance = scores.map { (it - mean) * (it - mean) }.average()  // LAMBDA: compute variance
val pass = count { it.hasPassed() }                   // LAMBDA: count passing
val gradeDistribution = groupingBy { it.grade.ifBlank { "Ungraded" } }.eachCount()
                       // LAMBDA: group by grade with default value
```

**Patterns Shown:**
1. `.map { }` - transformation
2. `.map { }` - mathematical computation (variance)
3. `.count { }` - conditional counting
4. `.groupingBy { }` - grouping/bucketing with lambda

---

### ✓ **LAMBDA #7: `identifyAtRiskStudents`** - Percentile Filtering
**Location:** `StudentRecord.kt` line ~168

```kotlin
fun List<StudentRecord>.identifyAtRiskStudents(): List<StudentRecord> {
    if (size < 5) return emptyList()
    val sorted = sortedBy { it.finalScore }  // LAMBDA: extract sort key
    val bottomThreshold = (size * 0.2).toInt().coerceAtLeast(1)
    return sorted.take(bottomThreshold)
}
```

**Pattern:** Chain operations: sort → filter

---

### ✓ **LAMBDA #8: `getPercentile`** - Count with Complex Predicate
**Location:** `StudentRecord.kt` line ~178

```kotlin
fun List<StudentRecord>.getPercentile(student: StudentRecord): Double {
    if (isEmpty()) return 0.0
    val better = count { it.finalScore > student.finalScore }  // Captures 'student'
    return (100.0 * (size - better)) / size
}
```

**Pattern:** Lambda closure capturing outer variable.

**Explanation:**
- `{ it.finalScore > student.finalScore }` captures `student` from outer scope
- Demonstrates lexical scoping/closure

---

### ✓ **LAMBDA #9: `filterAndEnrich`** - NEW Pattern (Added)
**Location:** `StudentRecord.kt` line ~191

```kotlin
fun List<StudentRecord>.filterAndEnrich(
    predicate: (StudentRecord) -> Boolean,
    enricher: (StudentRecord) -> Pair<StudentRecord, String>
): List<Pair<StudentRecord, String>> =
    this.filter(predicate).map(enricher)
```

**Pattern:** Combines filter + map with data enrichment.

**Usage:**
```kotlin
val enriched = students.filterAndEnrich(
    predicate = { it.finalScore < 40 },
    enricher = { s -> s to "Needs Support" }
)
```

---

### ✓ **LAMBDA #10: `batchProcess`** - Chunked + FlatMap
**Location:** `StudentRecord.kt` line ~203

```kotlin
fun List<StudentRecord>.batchProcess(
    batchSize: Int,
    processor: (List<StudentRecord>) -> List<StudentRecord>
): List<StudentRecord> =
    this.chunked(batchSize).flatMap(processor)
```

**Pattern:** Batch processing with flatMap lambda.

**Explanation:**
- Divides list into chunks
- Applies processor lambda to each batch
- Flattens results back to single list

---

### ✓ **LAMBDA #11: `transformWithFallback`** - Error Handling Lambda
**Location:** `StudentRecord.kt` line ~215

```kotlin
fun <T> List<StudentRecord>.transformWithFallback(
    transformer: (StudentRecord) -> T,
    fallback: (StudentRecord, Exception) -> T
): List<T> = this.map { student ->
    try {
        transformer(student)
    } catch (e: Exception) {
        fallback(student, e)
    }
}
```

**Pattern:** Try-catch inside map lambda for resilience.

**Explanation:**
- Passes error handling to caller via `fallback` lambda
- Demonstrates composition with error recovery

---

### ✓ **LAMBDA #12: `conditionalAggregate`** - Fold Pattern
**Location:** `StudentRecord.kt` line ~230

```kotlin
fun List<StudentRecord>.conditionalAggregate(
    condition: (StudentRecord) -> Boolean,
    aggregator: (Double, StudentRecord) -> Double
): Double =
    this.filter(condition)
        .fold(0.0) { acc, student -> aggregator(acc, student) }
```

**Pattern:** `fold` with accumulator and aggregator lambda.

**Usage:**
```kotlin
val totalPassing = students.conditionalAggregate(
    condition = { it.hasPassed() },
    aggregator = { acc, s -> acc + s.finalScore }
)
```

**Explanation:**
- `fold` - accumulator pattern (like reduce but with initial value)
- Aggregates conditionally selected elements

---

### ✓ **LAMBDA #13: `findByCriteria`** - Varargs Predicates
**Location:** `StudentRecord.kt` line ~248

```kotlin
fun List<StudentRecord>.findByCriteria(
    vararg predicates: (StudentRecord) -> Boolean
): List<StudentRecord> =
    this.filter { student -> predicates.all { it(student) } }
```

**Pattern:** Multiple lambda predicates with `all` quantifier.

**Usage:**
```kotlin
val results = students.findByCriteria(
    { it.finalScore >= 50 },
    { it.grade in listOf("A", "B") }
)
```

**Explanation:**
- Varargs: accepts variable number of lambda functions
- `all { }` - checks if ALL predicates match
- Demonstrates lambda array composition

---

### ✓ **LAMBDA #14: `scoreDistribution`** - GroupBy with Bucketing
**Location:** `StudentRecord.kt` line ~262

```kotlin
fun List<StudentRecord>.scoreDistribution(
    bucketizer: (StudentRecord) -> String
): Map<String, List<StudentRecord>> =
    groupBy(bucketizer)
```

**Pattern:** Flexible bucketing via lambda.

**Usage:**
```kotlin
val byRange = students.scoreDistribution { student ->
    when {
        student.finalScore >= 70 -> "High (70+)"
        student.finalScore >= 50 -> "Medium (50-69)"
        else -> "Low (<50)"
    }
}
```

**Explanation:**
- Categorizes items using lambda logic
- Lambda can contain complex when-expressions

---

### ✓ **LAMBDA #15: `comparePerformance`** - Zip + Map Pattern
**Location:** `StudentRecord.kt` line ~278

```kotlin
fun List<StudentRecord>.comparePerformance(
    other: List<StudentRecord>,
    comparator: (StudentRecord, StudentRecord) -> Boolean
): List<Triple<StudentRecord, StudentRecord, Boolean>> =
    this.zip(other).map { (s1, s2) -> Triple(s1, s2, comparator(s1, s2)) }
```

**Pattern:** Parallel structure comparison with lambda comparator.

**Explanation:**
- `zip` - pairs elements from two lists
- `map` - applies comparator to each pair
- Creates triple: (student1, student2, comparison result)

---

## UI Layer Lambdas

### **UI Layer Lambdas (HomeView.kt)**

The HomeView Composable file demonstrates **14+ UI-specific lambda patterns**:

---

### ✓ **LAMBDA #UI-1: Method Reference (Shorthand)**
**Location:** `HomeView.kt` - HomeTopBar (Theme toggle button)

```kotlin
IconButton(onClick = vm::toggleTheme) {
    // ... icon
}
```

**Pattern:** Method reference as lambda shorthand.

**Equivalent full lambda:** `onClick = { vm.toggleTheme() }`

**Benefits:**
- Concise syntax
- Reads naturally
- No parameter binding needed

---

### ✓ **LAMBDA #UI-2: Simple Lambda with Side Effect**
**Location:** `HomeView.kt` - HomeTopBar (Demo Data button)

```kotlin
OutlinedButton(onClick = vm::loadSampleData) {
    // ... Button content
}
```

**Pattern:** Direct method reference to state-modifying function.

---

### ✓ **LAMBDA #UI-3: Clickable Lambda**
**Location:** `HomeView.kt` - ImportSection (Drag-drop box)

```kotlin
.clickable { openFileChooser(vm) }
```

**Pattern:** Lambda wrapping function call.

**Explanation:**
- Opens file dialog when clicked
- Demonstrates UI interaction with business logic

---

### ✓ **LAMBDA #UI-4 & #UI-5: Click Handler Lambdas**
**Location:** `HomeView.kt` - ImportSection (Browse & Google Sheets buttons)

```kotlin
Button(onClick = { openFileChooser(vm) }, ...) { }
OutlinedButton(onClick = { showGoogleSheetDialog = true }, ...) { }
```

**Pattern:** State toggling lambdas.

**Explanation:**
- First: calls function
- Second: modifies composable state

---

### ✓ **LAMBDA #UI-6: Callback Lambda Chain**
**Location:** `HomeView.kt` - ImportSection (Google Sheets Dialog)

```kotlin
if (showGoogleSheetDialog) {
    GoogleSheetsDialog(
        url = vm.googleSheetUrl,
        onUrlChange = { vm.googleSheetUrl = it },          // LAMBDA #1
        onImport = { vm.importGoogleSheet(it); showGoogleSheetDialog = false },  // LAMBDA #2
        onDismiss = { showGoogleSheetDialog = false }      // LAMBDA #3
    )
}
```

**Pattern:** Multiple callback lambdas.

**Explanation:**
- Each parameter is a lambda (callback)
- `onUrlChange` - state update lambda
- `onImport` - multi-statement lambda (compound logic)
- `onDismiss` - state toggle lambda

---

### ✓ **LAMBDA #UI-7: ForEach Side-Effect Lambda**
**Location:** `HomeView.kt` - PerformanceInsightsCard (Grade Distribution)

```kotlin
stats.gradeDistribution.entries.forEach { (grade, count) ->
    Column(...) {
        // Render grade distribution UI
    }
}
```

**Pattern:** forEach lambda for UI composition.

**Explanation:**
- Destructures map entry into `(grade, count)`
- Iterates with side effects (composing UI)

---

### ✓ **LAMBDA #UI-8: State Binding Lambdas**
**Location:** `HomeView.kt` - AddStudentDialog (Text Fields)

```kotlin
OutlinedTextField(
    value = studentId,
    onValueChange = { studentId = it },  // LAMBDA: updates state
    label = { Text("ID") }               // LAMBDA: creates text label
)
```

**Pattern:** Two-way binding with lambda.

**Explanation:**
- `onValueChange` - receives new value, updates state
- `label` - lambda creating label composable
- Demonstrates reactive binding

---

### ✓ **LAMBDA #UI-9: Complex Click Handler**
**Location:** `HomeView.kt` - AddStudentDialog (Add Button)

```kotlin
Button(onClick = {
    val ca = caScore.toDoubleOrNull()
    val exam = examScore.toDoubleOrNull()
    when {
        studentId.isBlank() -> error = "..."
        name.isBlank() -> error = "..."
        ca == null || ca < 0.0 || ca > 30.0 -> error = "..."
        exam == null || exam < 0.0 || exam > 70.0 -> error = "..."
        else -> {
            onSubmit(StudentRecord(...))
            error = null
        }
    }
}) { }
```

**Pattern:** Multi-statement lambda with validation logic.

**Explanation:**
- Complex business logic inside lambda
- State validation & error handling
- Conditional callback invocation

---

### ✓ **LAMBDA #UI-10: Callback Lambda Invocation**
**Located within LAMBDA #UI-9**

```kotlin
onSubmit(StudentRecord(studentId.trim(), name.trim(), ca, exam, 0.0, ""))
```

**Pattern:** Explicit lambda parameter invocation.

**Explanation:**
- `onSubmit` is a lambda parameter
- Invoked like a function when conditions met
- Demonstrates callback pattern

---

### ✓ **LAMBDA #UI-11: ItemsIndexed Lambda**
**Location:** `HomeView.kt` - StudentTable (Lazy Column)

```kotlin
LazyColumn(...) {
    itemsIndexed(vm.pagedStudents) { idx, student ->
        StudentRow(vm, student, idx)
        if (idx < vm.pagedStudents.lastIndex) HorizontalDivider(...)
    }
}
```

**Pattern:** Index-aware list composition lambda.

**Explanation:**
- `itemsIndexed` - provides both index and element
- Renders row for each student
- Conditionally renders dividers

---

### ✓ **LAMBDA #UI-12 & #UI-13: ForEachIndexed + Clickable**
**Location:** `HomeView.kt` - TableHeader (Column Headers)

```kotlin
columns.forEachIndexed { idx, (col, label) ->
    Row(
        Modifier.weight(weight).clickable { vm.toggleSort(col) }.padding(...)
    ) {
        // Render header
    }
}
```

**Pattern:** Nested lambdas for headers with click handlers.

**Explanation:**
- Outer: `forEachIndexed` lambda for iteration
- Inner: `clickable` lambda for sort toggle
- Destructures column tuple: `(col, label)`

---

### ✓ **LAMBDA #UI-14: Delete Action Lambda**
**Location:** `HomeView.kt` - StudentRow (Delete Button)

```kotlin
IconButton(onClick = { vm.deleteStudent(student.id) }) {
    Icon(Icons.Default.Delete, ...)
}
```

**Pattern:** Action lambda capturing row context.

**Explanation:**
- Captures `student` from row scope
- Calls ViewModel action with row data
- Demonstrates closure in UI composition

---

## Lambda Pattern Reference

### **Kotlin Lambda Syntax**

```kotlin
// Basic lambda syntax
{ parameter -> body }

// Multi-parameter lambda
{ param1, param2 -> body }

// No parameters
{ -> body }

// Type annotations
{ x: Int -> x * 2 }

// Returning from lambda
{ x -> if (x > 0) "positive" else "negative" }

// Lambda assigned to variable
val doubler: (Int) -> Int = { x -> x * 2 }

// Method reference (shorthand)
::functionName
instanceRef::methodName
```

---

### **Common Extension Functions with Lambdas**

| Function | Signature | Purpose |
|----------|-----------|---------|
| `filter` | `(T) -> Boolean` | Keep items matching predicate |
| `map` | `(T) -> R` | Transform each item |
| `forEach` | `(T) -> Unit` | Iterate with side effects |
| `count` | `(T) -> Boolean` | Count items matching predicate |
| `find` | `(T) -> Boolean` | Find first matching item |
| `any` | `(T) -> Boolean` | Check if ANY match |
| `all` | `(T) -> Boolean` | Check if ALL match |
| `groupBy` | `(T) -> K` | Group by key |
| `sortedBy` | `(T) -> Comparable` | Sort by value |
| `fold` | `(acc, T) -> acc` | Accumulate result |
| `reduce` | `(acc, T) -> acc` | Accumulate (no initial) |
| `mapIndexed` | `(index, T) -> R` | Transform with index |
| `forEachIndexed` | `(index, T) -> Unit` | Iterate with index |

---

## Key Concepts & Benefits

### **1. Functional Programming Advantages**

**Declarative Code:**
```kotlin
// Functional (declarative) - READS LIKE ENGLISH
val topStudents = students
    .filter { it.finalScore >= 60 }
    .sortedByDescending { it.finalScore }
    .take(5)

// Imperative (procedural) - HOW, not WHAT
val topStudents = mutableListOf<StudentRecord>()
for (student in students) {
    if (student.finalScore >= 60) {
        topStudents.add(student)
    }
}
topStudents.sortByDescending { it.finalScore }
val top5 = topStudents.take(5)
```

### **2. Closures & Variable Capture**

```kotlin
// Lambda captures 'threshold' from outer scope
val threshold = 40.0
val passing = students.filter { it.finalScore > threshold }

// Lambda captures 'student' even though loop finished
val better = students.count { it.finalScore > student.finalScore }
```

### **3. Higher-Order Functions**

```kotlin
// Function takes another function as parameter
fun applyOperation(
    numbers: List<Int>,
    operation: (Int) -> Int
): List<Int> = numbers.map(operation)

// Called with different lambdas
applyOperation(listOf(1, 2, 3)) { it * 2 }  // [2, 4, 6]
applyOperation(listOf(1, 2, 3)) { it + 10 } // [11, 12, 13]
```

### **4. Composition & Chaining**

```kotlin
// Multiple lambdas chained together
students
    .filter { it.finalScore >= 50 }           // LAMBDA #1
    .map { it.name to it.finalScore }         // LAMBDA #2
    .sortedByDescending { it.second }         // LAMBDA #3
    .forEach { (name, score) -> println(...) } // LAMBDA #4
```

### **5. Lazy Evaluation**

```kotlin
// Lambda not executed until called
val getScore: (StudentRecord) -> Double = { it.finalScore }

// Executed here
val scores = students.map(getScore)
```

---

## Summary Statistics

| Category | Count | Examples |
|----------|-------|----------|
| **Model Layer Lambdas** | 15 | filter, map, count, fold, groupBy, etc. |
| **UI Layer Lambdas** | 14 | onClick, onValueChange, itemsIndexed, etc. |
| **Total Lambda Usages** | 29+ | Across data & UI layers |
| **Lambda Patterns** | 8 | Higher-order, closures, callbacks, etc. |

---

## Running the Code

All lambda demonstrations can be tested via:

```kotlin
// In StudentRecord.kt:
fun demonstrateHigherOrderFunctions() {
    val students = listOf(...)
    students.demonstrateHigherOrderFunctions()
}
```

---

**Last Updated:** March 24, 2026  
**Commit:** `1ec87ec`  
**Repository:** https://github.com/BG-Emmanuel/Android_app_dev

