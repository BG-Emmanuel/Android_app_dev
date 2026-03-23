import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "1.9.23"
    id("org.jetbrains.compose") version "1.6.11"
    kotlin("plugin.serialization") version "1.9.23"
}

group = "com.gradecalculator"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

dependencies {
    // Compose Desktop
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(compose.animation)

    // Apache POI for Excel (.xlsx / .xls)
    implementation("org.apache.poi:poi:5.2.5")
    implementation("org.apache.poi:poi-ooxml:5.2.5")

    // OpenCSV for CSV
    implementation("com.opencsv:opencsv:5.9")

    // Kotlinx Serialization (JSON vault storage)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.1")

    // Apache PDFBox for PDF export
    implementation("org.apache.pdfbox:pdfbox:3.0.2")

    // SLF4J (suppress logging noise from POI / PDFBox)
    implementation("org.slf4j:slf4j-simple:2.0.13")
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "GradeCalculator"
            packageVersion = "1.0.0"
            description = "Student Grade Calculator Pro"
            copyright = " 2025 Grade Calculator"

            windows {
                iconFile.set(project.file("src/main/resources/icons/app_icon.ico"))
            }
            macOS {
                iconFile.set(project.file("src/main/resources/icons/app_icon.icns"))
            }
            linux {
                iconFile.set(project.file("src/main/resources/icons/app_icon.png"))
            }
        }
    }
}
