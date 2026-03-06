import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "1.9.21"
    id("org.jetbrains.compose") version "1.5.11"
}

group = "com.gradecalculator"
version = "1.0.0"

repositories {
    mavenCentral()
    google()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)

    // Apache POI for Excel
    implementation("org.apache.poi:poi:5.2.4")
    implementation("org.apache.poi:poi-ooxml:5.2.4")

    // OpenCSV
    implementation("com.opencsv:opencsv:5.8")

    // Google APIs
    implementation("com.google.api-client:google-api-client:2.2.0")
    implementation("com.google.oauth-client:google-oauth-client-jetty:1.34.1")
    implementation("com.google.apis:google-api-services-drive:v3-rev20230822-2.0.0")
    implementation("com.google.apis:google-api-services-sheets:v4-rev20230815-2.0.0")

    // iText PDF generation
    implementation("com.itextpdf:itext7-core:7.2.5")

    // Kotlinx Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.3")

    // Kotlinx Serialization (JSON)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    // Kotlin logging
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    implementation("ch.qos.logback:logback-classic:1.4.11")
}

compose.desktop {
    application {
        mainClass = "com.gradecalculator.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "GradeCalculator"
            packageVersion = "1.0.0"
            description = "Student Grade Calculator"
            copyright = "© 2024 GradeCalc"
            vendor = "GradeCalc"

            windows {
                iconFile.set(project.file("src/main/resources/icons/app_icon.ico"))
                menuGroup = "GradeCalculator"
                upgradeUuid = "18159995-d967-4CD2-8885-77BFA97CFA9F"
            }
            macOS {
                iconFile.set(project.file("src/main/resources/icons/app_icon.icns"))
                bundleID = "com.gradecalculator"
            }
            linux {
                iconFile.set(project.file("src/main/resources/icons/app_icon.png"))
            }
        }
    }
}
