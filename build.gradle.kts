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

@file:Suppress("PropertyName")

import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension

plugins {
  kotlin("multiplatform") version "2.0.0-Beta3" apply false
  kotlin("plugin.serialization") version "2.0.0-RC1" apply false
  alias(libs.plugins.testlogger) apply false
  alias(libs.plugins.kotlinx.benchmark) apply false

  alias(libs.plugins.dokka)
  alias(libs.plugins.versionCheck)
  alias(libs.plugins.sonar)
  alias(libs.plugins.kover)
  alias(libs.plugins.detekt)
  alias(libs.plugins.nexus)
  alias(libs.plugins.spdx.sbom)
  alias(libs.plugins.cyclonedx)
  alias(libs.plugins.sigstore)
  alias(libs.plugins.kotlinx.apiValidator)

  `project-report`
  signing
}

val enableSbom = true
val enableCyclonedx = false
val GROUP: String by properties
val VERSION: String by properties
val sonarScan: String by properties
val nodeVersion: String by properties
val lockDeps: String by properties
val isReleaseBuild = !VERSION.contains("SNAPSHOT")

group = GROUP
version = VERSION

detekt {
  parallel = true
  ignoreFailures = true
  config.setFrom(rootProject.files(".github/detekt.yml"))
}

sonar {
  properties {
    listOf(
      "sonar.projectKey" to "elide-dev_uuid",
      "sonar.organization" to "elide-dev",
      "sonar.host.url" to "https://sonarcloud.io",
      "sonar.coverage.jacoco.xmlReportPaths" to "${project.rootDir}/build/reports/kover/report.xml",
    ).forEach { (key, value) ->
      property(key, value)
    }
  }
}

nexusPublishing {
  repositories {
    sonatype {
      nexusUrl = uri("https://s01.oss.sonatype.org/service/local/")
      snapshotRepositoryUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
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
  if (isReleaseBuild) {
    finalizedBy(":subprojects:uuid-core:spdxSbomForRelease")
  }
}

subprojects {
  apply(plugin = "dev.sigstore.sign")
  apply(plugin = "org.jetbrains.dokka")
  apply(plugin = "org.jetbrains.kotlinx.kover")
  apply(plugin = "org.sonarqube")
  apply(plugin = "signing")
}

val reports: TaskProvider<Task> by tasks.registering {
  group = "reports"

  dependsOn(
    tasks.koverXmlReport,
    tasks.dependencyReport,
    tasks.htmlDependencyReport,
  )
}

val preMerge: TaskProvider<Task> by tasks.registering {
  group = "test"
  description = "Run all tests and checks"

  listOfNotNull(
    tasks.build,
    tasks.check,
    tasks.koverXmlReport,
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

rootProject.plugins.withType(NodeJsRootPlugin::class.java) {
  rootProject.the<NodeJsRootExtension>().download = true
  rootProject.the<NodeJsRootExtension>().version = nodeVersion
  if (nodeVersion.contains("canary")) {
    rootProject.the<NodeJsRootExtension>().downloadBaseUrl = "https://nodejs.org/download/v8-canary"
  }
}
rootProject.plugins.withType(YarnPlugin::class.java) {
  rootProject.the<YarnRootExtension>().yarnLockMismatchReport = YarnLockMismatchReport.WARNING
  rootProject.the<YarnRootExtension>().reportNewYarnLock = false
  rootProject.the<YarnRootExtension>().yarnLockAutoReplace = true
}
tasks.withType(org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask::class.java).configureEach {
  args.add("--ignore-engines")
}

plugins.withType(io.gitlab.arturbosch.detekt.DetektPlugin::class) {
  tasks.withType(io.gitlab.arturbosch.detekt.Detekt::class) detekt@{
    reports.sarif.required = true
    reports.sarif.outputLocation = rootProject.layout.buildDirectory.file("reports/detekt/report.sarif")
  }
}

if (lockDeps == "true") {
  dependencyLocking {
    lockAllConfigurations()
    lockMode = LockMode.LENIENT
  }
}

val resolveAndLockAll: TaskProvider<Task> by tasks.registering {
  group = "build"
  description = "Resolve and re-lock all dependencies"

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

val relock: TaskProvider<Task> by tasks.registering {
  group = "build"
  description = "Re-lock all dependencies"

  dependsOn(
    tasks.dependencies,
    resolveAndLockAll,
  )
}
