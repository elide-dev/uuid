/*
 * Copyright (c) 2023 Elide Ventures, LLC.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */

@file:Suppress(
    "unused",
    "DSL_SCOPE_VIOLATION",
    "UNUSED_VARIABLE",
    "PropertyName",
)
@file:OptIn(
    org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl::class
)

import com.adarshr.gradle.testlogger.theme.ThemeType.MOCHA_PARALLEL
import org.jetbrains.kotlin.gradle.dsl.KotlinJsOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompileTool
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension

plugins {
    alias(libs.plugins.testlogger)
    alias(libs.plugins.versionCheck)
    alias(libs.plugins.doctor)
    alias(libs.plugins.dokka)
    alias(libs.plugins.sonar)
    alias(libs.plugins.kover)
    alias(libs.plugins.detekt)
    alias(libs.plugins.nexus)
    alias(libs.plugins.spdx.sbom)
    alias(libs.plugins.cyclonedx)
    alias(libs.plugins.sigstore)
    alias(libs.plugins.dependency.analysis)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlinx.benchmark)
    alias(libs.plugins.kotlinx.apiValidator)

    `project-report`
    `maven-publish`
    distribution
    signing
}

val defaultJavaToolchain: Int = 11
val jvmTargetMinimum: String by properties
val kotlinLanguage: String by properties
val lockDeps: String by properties
val nodeVersion: String by properties
val sonarScan: String by properties
val GROUP: String by properties
val VERSION: String by properties
val enableSbom = true
val enableCyclonedx = false

group = GROUP
version = VERSION

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

val isReleaseBuild = !VERSION.contains("SNAPSHOT")

repositories {
    maven("https://maven.pkg.st/")
}

testlogger {
    theme = MOCHA_PARALLEL
    showExceptions = System.getenv("TEST_EXCEPTIONS") == "true"
    showFailed = true
    showPassed = true
    showSkipped = true
    showFailedStandardStreams = true
    showFullStackTraces = true
    slowThreshold = 30000L
}

tasks.dokkaHtml {
    dokkaSourceSets {
        configureEach {
            samples.from("src/commonTest/kotlin")
        }
    }
}

kotlin {
    explicitApi()

    targets {
        js(IR) {
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

            compilations.all {
                kotlinOptions {
                    jvmTarget = jvmTargetMinimum
                }
            }
        }
        wasm {
            d8()
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
            watchosDeviceArm64()
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
        val wasmMain by getting { dependsOn(nonJvmMain) }
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
            val watchosDeviceArm64Main by getting { dependsOn(apple64Main) }
            val watchosDeviceArm64Test by getting { dependsOn(apple64Test) }
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

    sourceSets.all {
        languageSettings.apply {
            apiVersion = kotlinLanguage
            languageVersion = kotlinLanguage
            progressiveMode = true
            optIn("kotlin.ExperimentalUnsignedTypes")
        }
    }

    targets.all {
        compilations.all {
            kotlinOptions {
                apiVersion = kotlinLanguage
                languageVersion = kotlinLanguage
                allWarningsAsErrors = HostManager.hostIsMac
                when (this) {
                    is KotlinJvmOptions -> {
                        jvmTarget = jvmTargetMinimum
                        javaParameters = true
                        freeCompilerArgs = jvmFlags
                    }
                    is KotlinJsOptions -> {
                        sourceMap = true
                        moduleKind = "umd"
                        metaInfo = true
                    }
                    else -> {
                        freeCompilerArgs = kotlinCompilerArgs
                    }
                }
            }
        }

    }
}

tasks.withType<KotlinNativeCompile>().configureEach {
    compilerOptions.freeCompilerArgs.add("-opt-in=kotlinx.cinterop.ExperimentalForeignApi")
}

tasks.withType<Test>().configureEach {
    maxParallelForks = 4
}

tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = jvmTargetMinimum
    targetCompatibility = jvmTargetMinimum
    options.isFork = true
    options.isIncremental = true
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

// Generate PROJECT_DIR_ROOT for referencing local mocks in tests

val projectDirGenRoot = "$buildDir/generated/projectdir/kotlin"
val projectDirPath: String = projectDir.absolutePath
val generateProjDirValTask: TaskProvider<Task> = tasks.register("generateProjectDirectoryVal") {
    mkdir(projectDirGenRoot)
    val projDirFile = File("$projectDirGenRoot/projdir.kt")
    projDirFile.writeText("")
    projDirFile.appendText(
        """
            |package dev.elide.uuid
            |
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

tasks.withType<Jar>().configureEach {
    isReproducibleFileOrder = true
    isPreserveFileTimestamps = false
}

tasks.withType<Zip>().configureEach {
    isReproducibleFileOrder = true
    isPreserveFileTimestamps = false
}

configurations.all {
    resolutionStrategy {
        // fail eagerly on version conflict (includes transitive dependencies)
        failOnVersionConflict()

        // prefer modules that are part of this build
        preferProjectModules()

        if (name.contains("detached")) {
            disableDependencyVerification()
        }
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

//tasks.withType<DokkaTask> {
//    dokkaSourceSets {
//        named("commonMain") {
//            samples.from("src/commonTest/kotlin")
//        }
//    }
//}

if (lockDeps == "true") {
    dependencyLocking {
        lockAllConfigurations()
        lockMode = LockMode.LENIENT
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

spdxSbom {
    targets {
        create("release") {
            configurations.set(listOf("jvmRuntimeClasspath"))

            scm {
                uri.set("https://github.com/elide-dev/uuid")
            }
            // configure here
            document {
                name.set("Elide Multiplatform UUID")
                namespace.set("https://elide.dev/spdx/F9B2EC53-49B0-41C7-A013-55FC4BA6B677")
                creator.set("Person:Sam Gammon")
                packageSupplier.set("Organization:Elide")
            }
        }
    }
}

tasks.cyclonedxBom {
    setIncludeConfigs(listOf("jvmRuntimeClasspath"))
    setProjectType("library")
    setDestination(project.file("build/reports"))
    setOutputFormat("all")
    setIncludeBomSerialNumber(true)
    setComponentVersion("2.0.0")
}

tasks.build {
    finalizedBy(tasks.spdxSbom)
}

val mavenUsername: String? = properties["mavenUsername"] as? String
val mavenPassword: String? = properties["mavenPassword"] as? String

tasks.withType(Sign::class) {
    enabled = isReleaseBuild
}

publishing {
    publications.withType<MavenPublication> {
        artifactId = artifactId.replace("uuid", "elide-uuid")

        pom {
            name.set("Elide UUID")
            url.set("https://elide.dev")
            description.set("UUID tools for Kotlin Multiplatform.")

            licenses {
                license {
                    name.set("MIT License")
                    url.set("https://github.com/elide-dev/elide/blob/v3/LICENSE")
                }
            }
            developers {
                developer {
                    id.set("sgammon")
                    name.set("Sam Gammon")
                    email.set("samuel.gammon@gmail.com")
                }
            }
            scm {
                url.set("https://github.com/elide-dev/elide")
            }
        }
    }
}

nexusPublishing {
    repositories {
        sonatype {  //only for users registered in Sonatype after 24 Feb 2021
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
        }
    }
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
    listOfNotNull(
        tasks.build,
        tasks.check,
        if (enableSbom) tasks.spdxSbom else null,
        if (enableCyclonedx) tasks.cyclonedxBom else null,
        reports,
    ).forEach {
        dependsOn(it)
    }
    if (sonarScan == "true") {
        dependsOn(
            tasks.sonar,
        )
    }
}

afterEvaluate {
    listOf(
        "wasmTest",
        "compileTestDevelopmentExecutableKotlinWasm",
    ).forEach {
        tasks.named(it).configure {
            enabled = false
        }
    }

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

val publishMac by tasks.registering {
    dependsOn(
        "publishIosArm64PublicationToMavenRepository",
        "publishIosSimulatorArm64PublicationToMavenRepository",
        "publishIosX64PublicationToMavenRepository",
        "publishTvosArm64PublicationToMavenRepository",
        "publishTvosSimulatorArm64PublicationToMavenRepository",
        "publishTvosX64PublicationToMavenRepository",
        "publishWatchosArm32PublicationToMavenRepository",
        "publishWatchosArm64PublicationToMavenRepository",
        "publishWatchosSimulatorArm64PublicationToMavenRepository",
        "publishWatchosDeviceArm64PublicationToMavenRepository",
        "publishWatchosX64PublicationToMavenRepository",
        "publishMacosArm64PublicationToMavenRepository",
        "publishMacosX64PublicationToMavenRepository",
        "publishJvmPublicationToMavenRepository",
        "publishJsPublicationToMavenRepository",
        "publishWasmPublicationToMavenRepository",
        "publishKotlinMultiplatformPublicationToMavenRepository",
        "publishWasmPublicationToMavenRepository",
    )
}

val publishWindows by tasks.registering {
    dependsOn(
        "publishMingwX64PublicationToMavenRepository",
    )
}

val publishLinux by tasks.registering {
    dependsOn(
        "publishLinuxX64PublicationToMavenRepository",
        "publishLinuxArm64PublicationToMavenRepository",
    )
}
