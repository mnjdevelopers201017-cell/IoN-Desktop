plugins {
    kotlin("jvm") version "1.9.22"
    id("org.jetbrains.compose") version "1.6.2"
}

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            // This line is the magic for all 3 OSs!
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi, // Windows
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg, // Mac
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb  // Linux
            )
            packageName = "IoN-Desktop"
            packageVersion = "1.0.0"
        }
    }
}
