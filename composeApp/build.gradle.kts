@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.sqlDelight)
    alias(libs.plugins.buildConfig)
}

kotlin {
    jvmToolchain(17)

    androidTarget { publishLibraryVariants("release") }

    jvm("desktop")

    sourceSets {

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(compose.desktop.currentOs) // This downloads the .dll files for Windows

                implementation(libs.kotlinx.coroutines.swing)
                //implementation(libs.ktor.client.okhttp)
                implementation(libs.ok.http3)
                implementation(libs.sqlDelight.driver.sqlite)
                implementation(libs.multiplatformSettings)

                // Add this line to provide the "suitable driver"
                implementation("com.mysql:mysql-connector-j:8.3.0")

                // Optional: Add this to fix the SLF4J warning in your logs
                implementation("org.slf4j:slf4j-simple:2.0.9")
            }
        }

        val commonMain by getting {
            dependencies {
                implementation(compose.foundation)
                implementation(libs.compose.material3)
                implementation(compose.components.resources)
                implementation(compose.materialIconsExtended)
                implementation(compose.runtime)
                implementation(compose.ui)

                implementation(libs.dropbox)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)
                implementation(libs.kermit)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.client.serialization)
                implementation(libs.ktor.serialization.json)
                implementation(libs.ktor.client.logging)
                implementation(libs.multiplatformSettings)
                implementation(libs.ok.http3)

                implementation(libs.google.code.gson)
                implementation(libs.jetbrains.androidx.lifecycle.viewmodel.compose)

            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.activityCompose) // This is the big one
                implementation(libs.kotlinx.coroutines.android)
                //implementation(libs.ktor.client.okhttp)
                implementation(libs.ok.http3)
                implementation(libs.sqlDelight.driver.android)

                // If you don't have this in your libs.versions.toml yet,
                // you can use the string version:
                // implementation("androidx.activity:activity-compose:1.10.1")
            }
        }

//        linuxMain.dependencies {
//            implementation(libs.ktor.client.curl)
//            implementation(libs.sqlDelight.driver.native)
//        }
//
//        mingwMain.dependencies {
//            implementation(libs.ktor.client.winhttp)
//            implementation(libs.sqlDelight.driver.native)
//        }
    }

    //https://kotlinlang.org/docs/native-objc-interop.html#export-of-kdoc-comments-to-generated-objective-c-headers
    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
        compilations["main"].compileTaskProvider.configure {
            compilerOptions {
                freeCompilerArgs.add("-Xexport-kdoc")
            }
        }
    }

}

compose.desktop {
    application {
        mainClass = "org.elsoft.bkdb.MainKt"
        from(kotlin.targets.getByName("desktop"))

        nativeDistributions {
            includeAllModules = true

            targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi, org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb)
            packageName = "EBookLibrary"
            packageVersion = "1.0.0"

            windows {
                menu = true
                shortcut = true
                // iconFile.set(project.file("commonMain/composeResources/drawable/books.ico"))
            }
        }
    }
}

android {
    namespace = "org.elsoft.bkdb"
    compileSdk = 35

    defaultConfig {
        minSdk = 21
    }
}

//Publishing your Kotlin Multiplatform library to Maven Central
//https://www.jetbrains.com/help/kotlin-multiplatform-dev/multiplatform-publish-libraries.html
mavenPublishing {
    publishToMavenCentral()
    coordinates("org.elsoft.bkdb", "composeApp", "1.0.0")

    pom {
        name = "KMP library"
        description = "Kotlin Multiplatform library"
        url = "github url" //todo

        licenses {
            license {
                name = "MIT"
                url = "https://opensource.org/licenses/MIT"
            }
        }

        developers {
            developer {
                id = "" //todo
                name = "" //todo
                email = "" //todo
            }
        }

        scm {
            url = "github url" //todo
        }
    }
    if (project.hasProperty("signing.keyId")) signAllPublications()
}

buildConfig {
    // BuildConfig configuration here.
    // https://github.com/gmazzo/gradle-buildconfig-plugin#usage-in-kts
}

sqldelight {
    databases {
        create("MyDatabase") {
            // Database configuration here.
            // https://cashapp.github.io/sqldelight
            packageName.set("org.elsoft.bkdb.db")
        }
    }
}
