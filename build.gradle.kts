@file:Suppress(
    "unused",
    "DSL_SCOPE_VIOLATION",
    "UNUSED_VARIABLE",
)

import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompileTool
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension

plugins {
    alias(libs.plugins.versionCheck)
    alias(libs.plugins.doctor)
    alias(libs.plugins.dokka)
    alias(libs.plugins.sonar)
    alias(libs.plugins.kover)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlinx.benchmark)
    alias(libs.plugins.kotlinx.apiValidator)

    id("project-report")
    id("maven-publish")
    id("signing")
}

val defaultJavaToolchain: Int = 11
val jvmTargetMinimum: String by properties
val kotlinLanguage: String by properties
val lockDeps: String by properties
val nodeVersion: String by properties
val sonarScan: String by properties

val kotlinCompilerArgs = listOf(
    "-progressive",
    "-Xcontext-receivers",
)

val jvmFlags = kotlinCompilerArgs.plus(listOf(
    "-Xjvm-default=all",
    "-Xjsr305=strict",
    "-Xallow-unstable-dependencies",
    "-Xemit-jvm-type-annotations",
))

val cacheDisabledTasks = listOf(
    "compileNix64MainKotlinMetadata",
    "jvmTest",
    "compileTestKotlinLinuxX64",
    "linkDebugTestLinuxX64",
    "koverXmlReport",
)

repositories {
    maven("https://maven.pkg.st/")
}

kotlin {
    explicitApi()

    targets {
        js {
            compilations.all {
                kotlinOptions {
                    sourceMap = true
                    moduleKind = "umd"
                    metaInfo = true
                }
            }
            browser()
            nodejs()
        }
        jvm {
            jvmToolchain(jvmTargetMinimum.toIntOrNull() ?: defaultJavaToolchain)
        }
        if (HostManager.hostIsMac) {
            macosX64()
            macosArm64()
            iosX64()
            iosArm64()
            iosSimulatorArm64()
            watchosArm32()
            watchosArm64()
            watchosX64()
            watchosSimulatorArm64()
            tvosArm64()
            tvosX64()
            tvosSimulatorArm64()
        }
        if (HostManager.hostIsMingw || HostManager.hostIsMac) {
            mingwX64 {
                binaries.findTest(DEBUG)!!.linkerOpts = mutableListOf("-Wl,--subsystem,windows")
            }
        }
        if (HostManager.hostIsLinux || HostManager.hostIsMac) {
            linuxX64()
            linuxArm64()
        }
    }

    sourceSets {
        val commonMain by getting
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val nonJvmMain by creating { dependsOn(commonMain) }
        val nonJvmTest by creating { dependsOn(commonTest) }
        val jsMain by getting { dependsOn(nonJvmMain) }
        val jsTest by getting { dependsOn(nonJvmTest) }
        val nativeMain by creating { dependsOn(nonJvmMain) }
        val nativeTest by creating { dependsOn(nonJvmTest) }
        val nix64Main by creating { dependsOn(nativeMain) }
        val nix64Test by creating { dependsOn(nativeTest) }
        val nix32Main by creating { dependsOn(nativeMain) }
        val nix32Test by creating { dependsOn(nativeTest) }

        if (HostManager.hostIsMac) {
            val appleMain by creating { dependsOn(nativeMain) }
            val appleTest by creating { dependsOn(nativeTest) }
            val apple64Main by creating {
                dependsOn(appleMain)
                dependsOn(nix64Main)
            }
            val apple64Test by creating {
                dependsOn(appleTest)
                dependsOn(nix64Test)
            }
            val apple32Main by creating {
                dependsOn(appleMain)
                dependsOn(nix32Main)
            }
            val apple32Test by creating {
                dependsOn(appleTest)
                dependsOn(nix32Test)
            }
            val iosX64Main by getting { dependsOn(apple64Main) }
            val iosX64Test by getting { dependsOn(apple64Test) }
            val iosArm64Main by getting { dependsOn(apple64Main) }
            val iosArm64Test by getting { dependsOn(apple64Test) }
            val macosX64Main by getting { dependsOn(apple64Main) }
            val macosX64Test by getting { dependsOn(apple64Test) }
            val macosArm64Main by getting { dependsOn(apple64Main) }
            val macosArm64Test by getting { dependsOn(apple64Test) }
            val iosSimulatorArm64Main by getting { dependsOn(apple64Main) }
            val iosSimulatorArm64Test by getting { dependsOn(apple64Test) }
            val watchosArm32Main by getting { dependsOn(apple32Main) }
            val watchosArm32Test by getting { dependsOn(apple32Test) }
            val watchosArm64Main by getting { dependsOn(apple64Main) }
            val watchosArm64Test by getting { dependsOn(apple64Test) }
            val watchosX64Main by getting { dependsOn(apple64Main) }
            val watchosX64Test by getting { dependsOn(apple64Test) }
            val watchosSimulatorArm64Main by getting { dependsOn(apple64Main) }
            val watchosSimulatorArm64Test by getting { dependsOn(apple64Test) }
            val tvosArm64Main by getting { dependsOn(apple64Main) }
            val tvosArm64Test by getting { dependsOn(apple64Test) }
            val tvosX64Main by getting { dependsOn(apple64Main) }
            val tvosX64Test by getting { dependsOn(apple64Test) }
            val tvosSimulatorArm64Main by getting { dependsOn(apple64Main) }
            val tvosSimulatorArm64Test by getting { dependsOn(apple64Test) }
        }

        if (HostManager.hostIsMingw || HostManager.hostIsMac) {
            val mingwMain by creating { dependsOn(nativeMain) }
            val mingwTest by creating { dependsOn(nativeTest) }
            val mingwX64Main by getting { dependsOn(mingwMain) }
            val mingwX64Test by getting { dependsOn(mingwTest) }
        }

        if (HostManager.hostIsLinux || HostManager.hostIsMac) {
            val linuxX64Main by getting { dependsOn(nix64Main) }
            val linuxX64Test by getting { dependsOn(nix64Test) }
            val linuxArm64Main by getting { dependsOn(nix64Main) }
            val linuxArm64Test by getting { dependsOn(nix64Test) }
        }
    }

    targets.all {
        compilations.all {
            kotlinOptions {
                apiVersion = kotlinLanguage
                languageVersion = kotlinLanguage
                allWarningsAsErrors = true
                if (this is KotlinJvmOptions) {
                    jvmTarget = jvmTargetMinimum
                    javaParameters = true
                    freeCompilerArgs = jvmFlags
                } else {
                    freeCompilerArgs = kotlinCompilerArgs
                }
            }
        }
    }
}

rootProject.plugins.withType(NodeJsRootPlugin::class.java) {
    // 16+ required for Apple Silicon support
    // https://youtrack.jetbrains.com/issue/KT-49109#focus=Comments-27-5259190.0-0
    rootProject.the<NodeJsRootExtension>().download = true
    rootProject.the<NodeJsRootExtension>().nodeVersion = nodeVersion
    rootProject.the<NodeJsRootExtension>().nodeDownloadBaseUrl = "https://node.pkg.st/"
}
rootProject.plugins.withType(YarnPlugin::class.java) {
    rootProject.the<YarnRootExtension>().yarnLockMismatchReport = YarnLockMismatchReport.WARNING
    rootProject.the<YarnRootExtension>().reportNewYarnLock = false
    rootProject.the<YarnRootExtension>().yarnLockAutoReplace = true
}

val ktlintConfig: Configuration by configurations.creating

dependencies {
    ktlintConfig(libs.ktlint)
}

detekt {
    parallel = true
    ignoreFailures = true
    config = rootProject.files(".github/detekt.yml")
}

val ktlint by tasks.registering(JavaExec::class) {
    group = "verification"
    description = "Check Kotlin code style."
    classpath = ktlintConfig
    mainClass.set("com.pinterest.ktlint.Main")
    args = listOf("src/**/*.kt")
}

val ktlintformat by tasks.registering(JavaExec::class) {
    group = "formatting"
    description = "Fix Kotlin code style deviations."
    classpath = ktlintConfig
    mainClass.set("com.pinterest.ktlint.Main")
    args = listOf("-F", "src/**/*.kt", "*.kts")
}

val checkTask: TaskProvider<Task> = tasks.named("check")

checkTask.configure {
    dependsOn(ktlint)
}

// apply(from = "publish.gradle")

// Generate PROJECT_DIR_ROOT for referencing local mocks in tests

val projectDirGenRoot = "$buildDir/generated/projectdir/kotlin"
val projectDirPath: String = projectDir.absolutePath
val generateProjDirValTask: TaskProvider<Task> = tasks.register("generateProjectDirectoryVal") {
    mkdir(projectDirGenRoot)
    val projDirFile = File("$projectDirGenRoot/projdir.kt")
    projDirFile.writeText("")
    projDirFile.appendText(
        """
            |package com.benasher44.uuid
            |
            |import kotlin.native.concurrent.SharedImmutable
            |
            |@SharedImmutable
            |internal const val PROJECT_DIR_ROOT = ""${'"'}${projectDirPath}""${'"'}
            |
        """.trimMargin()
    )
}

kotlin.sourceSets.named("commonTest") {
    this.kotlin.srcDir(projectDirGenRoot)
}

// Ensure this runs before any test compile task
tasks.withType<AbstractCompile>().configureEach {
    if (name.lowercase().contains("test")) {
        dependsOn(generateProjDirValTask)
    }
}

tasks.withType<AbstractKotlinCompileTool<*>>().configureEach {
    if (name.lowercase().contains("test")) {
        dependsOn(generateProjDirValTask)
    }
}

listOf(
    "jsTest",
    "compileTestDevelopmentExecutableKotlinJs",
).forEach { taskName ->
    tasks.named(taskName) {
        enabled = false  // disabled because it breaks on macOS
    }
}

sonarqube {
    properties {
        listOf(
            "sonar.projectKey" to "elide-dev_uuid",
            "sonar.organization" to "elide-dev",
            "sonar.host.url" to "https://sonarcloud.io",
            "sonar.coverage.jacoco.xmlReportPaths" to "${project.rootDir}/build/reports/kover/xml/report.xml",
        ).forEach { (key, value) ->
            property(key, value)
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.isFork = true
    options.isIncremental = true
}

plugins.withType(io.gitlab.arturbosch.detekt.DetektPlugin::class) {
    tasks.withType(io.gitlab.arturbosch.detekt.Detekt::class) detekt@{
        reports.sarif.required.set(true)
        reports.sarif.outputLocation.set(
            rootProject.buildDir.resolve("reports/detekt/report.sarif")
        )
    }
}

tasks.withType<DokkaTask> {
    dokkaSourceSets {
        register("main") {
            samples.from("src/commonTest/kotlin")
        }
    }
}

if (lockDeps == "true") {
    dependencyLocking {
        lockAllConfigurations()
    }
}

val resolveAndLockAll: TaskProvider<Task> = tasks.register("resolveAndLockAll") {
    doFirst {
        require(gradle.startParameter.isWriteDependencyLocks)
    }
    doLast {
        configurations.filter {
            // Add any custom filtering on the configurations to be resolved
            it.isCanBeResolved
        }.forEach { it.resolve() }
    }
}

tasks.register("relock") {
    dependsOn(
        tasks.dependencies,
        resolveAndLockAll,
    )
}

val reports: TaskProvider<Task> = tasks.register("reports") {
    dependsOn(
        tasks.koverXmlReport,
        tasks.dependencyReport,
        tasks.htmlDependencyReport,
    )
}

val allTests: TaskProvider<Task> = tasks.named("allTests")
val test: Task = tasks.create("test") {
    dependsOn(
        allTests,
    )
}

val check: TaskProvider<Task> = tasks.named("check") {
    dependsOn(
        test,
        ktlint,
        tasks.apiCheck,
        tasks.koverVerify,
    )
}

tasks.create("preMerge") {
    dependsOn(
        tasks.build,
        tasks.check,
        reports,
    )
    if (sonarScan == "true") {
        dependsOn(
            tasks.sonar,
        )
    }
}

afterEvaluate {
    cacheDisabledTasks.forEach {
        try {
            tasks.named(it).configure {
                doNotTrackState("too big for build cache")
            }
        } catch (err: Throwable) {
            // ignore
        }
    }
}
