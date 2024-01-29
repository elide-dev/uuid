/*
 * Copyright (c) 2023-2024 Elide Technologies, Inc.
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
import dev.sigstore.sign.tasks.SigstoreSignFilesTask
import org.jetbrains.kotlin.gradle.dsl.KotlinJsOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompileTool
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.konan.target.HostManager

plugins {
  kotlin("multiplatform")
  id(libs.plugins.testlogger.get().pluginId)
  id(libs.plugins.dokka.get().pluginId)
  id(libs.plugins.sonar.get().pluginId)
  id(libs.plugins.kover.get().pluginId)
  id(libs.plugins.detekt.get().pluginId)
  id(libs.plugins.kotlinx.benchmark.get().pluginId)
  id(libs.plugins.spdx.sbom.get().pluginId)

  `maven-publish`
  distribution
  signing
}

val defaultJavaToolchain: Int = 11
val jvmTargetMinimum: String by properties
val kotlinLanguage: String by properties
val GROUP: String by properties
val VERSION: String by properties

group = GROUP
version = VERSION

val kotlinCompilerArgs = listOf(
  "-Xcontext-receivers",
  "-Xexpect-actual-classes",
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

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(21)
  }
}

kotlin {
  explicitApi()

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
    generateTypeScriptDefinitions()
  }

  jvm {
    withJava()
    withSourcesJar(publish = true)

    compilations.all {
      kotlinOptions {
        jvmTarget = jvmTargetMinimum
      }
    }
  }

  wasmJs {
    nodejs()
    browser()
  }
  wasmWasi {
    applyBinaryen()
    nodejs()
  }

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

  linuxX64()
  linuxArm64()

  androidNativeX86()
  androidNativeX64()
  androidNativeArm64()

  mingwX64 {
    binaries.findTest(DEBUG)!!.linkerOpts = mutableListOf("-Wl,--subsystem,windows")
  }

  sourceSets {
    val commonMain by getting {
      dependencies {
        // Nothing.
      }
    }
    val commonTest by getting {
      dependencies {
        implementation(kotlin("stdlib"))
        implementation(kotlin("test"))
      }
    }

    val nonJvmMain by creating { dependsOn(commonMain) }
    val nonJvmTest by creating { dependsOn(commonTest) }
    val jsMain by getting { dependsOn(nonJvmMain) }
    val wasmJsMain by getting { dependsOn(nonJvmMain) }
    val wasmWasiMain by getting { dependsOn(nonJvmMain) }
    val jsTest by getting { dependsOn(nonJvmTest) }
    val nativeMain by creating { dependsOn(nonJvmMain) }
    val nativeTest by creating { dependsOn(nonJvmTest) }
    val nix64Main by creating { dependsOn(nativeMain) }
    val nix64Test by creating { dependsOn(nativeTest) }
    val nix32Main by creating { dependsOn(nativeMain) }
    val nix32Test by creating { dependsOn(nativeTest) }

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

    val mingwMain by creating { dependsOn(nativeMain) }
    val mingwTest by creating { dependsOn(nativeTest) }
    val mingwX64Main by getting { dependsOn(mingwMain) }
    val mingwX64Test by getting { dependsOn(mingwTest) }

    val linuxX64Main by getting { dependsOn(nix64Main) }
    val linuxX64Test by getting { dependsOn(nix64Test) }
    val linuxArm64Main by getting { dependsOn(nix64Main) }
    val linuxArm64Test by getting { dependsOn(nix64Test) }

    val androidNativeX86Main by getting { dependsOn(nix32Main) }
    val androidNativeX86Test by getting { dependsOn(nix32Test) }
    val androidNativeX64Main by getting { dependsOn(nix64Main) }
    val androidNativeX64Test by getting { dependsOn(nix64Test) }
    val androidNativeArm64Main by getting { dependsOn(nix64Main) }
    val androidNativeArm64Test by getting { dependsOn(nix64Test) }
  }

  sourceSets.all {
    languageSettings.apply {
      apiVersion = kotlinLanguage
      languageVersion = kotlinLanguage
      progressiveMode = false
      optIn("kotlin.ExperimentalUnsignedTypes")
    }
  }

  targets.all {
    compilations.all {
      kotlinOptions {
        apiVersion = kotlinLanguage
        languageVersion = kotlinLanguage
        allWarningsAsErrors = false
        freeCompilerArgs = freeCompilerArgs.plus(kotlinCompilerArgs).toSortedSet().toList()

        when (this) {
          is KotlinJvmOptions -> {
            jvmTarget = jvmTargetMinimum
            javaParameters = true
            freeCompilerArgs = freeCompilerArgs.plus(jvmFlags).toSortedSet().toList()
          }
          is KotlinJsOptions -> {
            sourceMap = true
            moduleKind = "umd"
            metaInfo = true
          }
        }
      }
    }
  }
}

tasks.withType<KotlinNativeCompile>().configureEach {
  compilerOptions.freeCompilerArgs.addAll(listOf(
    "-opt-in=kotlinx.cinterop.ExperimentalForeignApi",
  ).plus(kotlinCompilerArgs))
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

val javadocsJar: TaskProvider<Jar> by tasks.registering(Jar::class) {
  dependsOn(tasks.dokkaHtml)
  archiveClassifier = "javadoc"
  from(tasks.dokkaHtml.get().outputDirectory)
}

val checkTask: TaskProvider<Task> = tasks.named("check")

// Generate PROJECT_DIR_ROOT for referencing local mocks in tests

val projectDirGenRoot: Provider<Directory> = layout.buildDirectory.dir("generated/projectdir/kotlin")
val projectDirPath: String = projectDir.absolutePath
val generateProjectDirectoryVal: TaskProvider<Task> by tasks.registering {
  group = "build"
  description = "Generate project directory build-time values"

  mkdir(projectDirGenRoot)
  val projDirFile = File("${projectDirGenRoot.get()}/projdir.kt")
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
    dependsOn(generateProjectDirectoryVal)
  }
}

tasks.withType<AbstractKotlinCompileTool<*>>().configureEach {
  if (name.lowercase().contains("test")) {
    dependsOn(generateProjectDirectoryVal)
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

tasks.withType<JavaCompile>().configureEach {
  options.isFork = true
  options.isIncremental = true
}

val mavenUsername: String? = properties["mavenUsername"] as? String
val mavenPassword: String? = properties["mavenPassword"] as? String

publishing {
  repositories {
    maven {
      name = "GitHubPackages"
      url = uri("https://maven.pkg.github.com/elide-dev/uuid")
      credentials {
        username = System.getenv("GITHUB_ACTOR")
        password = System.getenv("GITHUB_TOKEN")
      }
    }
    maven {
      name = "ElideDev"
      url = uri(project.properties["RELEASE_REPOSITORY_URL"] as String)
    }
  }

  publications.withType<MavenPublication> {
    artifact(javadocsJar)
    artifactId = "elide-uuid"
    groupId = GROUP

    pom {
      name = "Elide UUID"
      url = "https://elide.dev"
      description = "UUID tools for Kotlin Multiplatform."

      licenses {
        license {
          name = "MIT License"
          url = "https://github.com/elide-dev/elide/blob/v3/LICENSE"
        }
      }
      developers {
        developer {
          id = "sgammon"
          name = "Sam Gammon"
          email = "samuel.gammon@gmail.com"
        }
      }
      scm {
        url = "https://github.com/elide-dev/elide"
      }
    }
  }
}

tasks.check {
  dependsOn(
    tasks.apiCheck,
    tasks.koverVerify,
  )
}

val signAll by tasks.registering {
  dependsOn(tasks.withType(Sign::class))
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

val signingTasks = tasks.withType(Sign::class)

tasks.names.forEach {
  if (it.startsWith("linkDebug") || (it.startsWith("compileTest"))) {
    tasks.named(it).configure {
      dependsOn(signingTasks)
    }
  }
}

spdxSbom {
  targets {
    create("release") {
      configurations = listOf("jvmRuntimeClasspath")

      scm {
        uri = "https://github.com/elide-dev/uuid"
      }

      document {
        name = "Elide Multiplatform UUID"
        namespace = "https://elide.dev/spdx/F9B2EC53-49B0-41C7-A013-55FC4BA6B677"
        creator = "Person:Sam Gammon"
        packageSupplier = "Organization:Elide"
      }
    }
  }
}
