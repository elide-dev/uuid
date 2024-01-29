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
  kotlin("plugin.serialization")
  id(libs.plugins.testlogger.get().pluginId)
  id(libs.plugins.dokka.get().pluginId)
  id(libs.plugins.sonar.get().pluginId)
  id(libs.plugins.kover.get().pluginId)
  id(libs.plugins.detekt.get().pluginId)
  id(libs.plugins.kotlinx.benchmark.get().pluginId)

  `maven-publish`
  distribution
  signing
}

val defaultJavaToolchain: Int = 11
val jvmTargetMinimum: String by properties
val kotlinLanguage: String by properties
val lockDeps: String by properties
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
        api(kotlin("stdlib"))
        api(libs.kotlinx.serialization.core)
        api(projects.subprojects.uuidCore)
      }
    }
    val commonTest by getting {
      dependencies {
        implementation(kotlin("test"))
        implementation(libs.kotlinx.serialization.core)
        implementation(libs.kotlinx.serialization.json)
        implementation(libs.kotlinx.serialization.protobuf)
      }
    }
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

tasks.withType<Jar>().configureEach {
  isReproducibleFileOrder = true
  isPreserveFileTimestamps = false
}

tasks.withType<Zip>().configureEach {
  isReproducibleFileOrder = true
  isPreserveFileTimestamps = false
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

signing {
  isRequired = isReleaseBuild
  sign(publishing.publications)
}

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
    artifactId = "elide-$artifactId"
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

tasks.withType(Sign::class) {
  onlyIf { isReleaseBuild }
}
tasks.withType(SigstoreSignFilesTask::class) {
  onlyIf { isReleaseBuild }
}
tasks.withType(AbstractPublishToMaven::class.java) {
  val signingTasks = tasks.withType(Sign::class)
  val sigstoreTasks = tasks.withType(SigstoreSignFilesTask::class)

  dependsOn(signingTasks, sigstoreTasks)
  mustRunAfter(signingTasks, sigstoreTasks)
}
