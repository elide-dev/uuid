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
  "UnstableApiUsage",
)

pluginManagement {
  repositories {
    maven("https://gradle.pkg.st/")
  }
}

plugins {
  id("build.less") version("1.0.0-rc2")
  id("com.gradle.enterprise") version("3.16.2")
  id("com.gradle.common-custom-user-data-gradle-plugin") version("1.12.1")
  id("org.gradle.toolchains.foojay-resolver-convention") version("0.8.0")
}

dependencyResolutionManagement {
  repositoriesMode = RepositoriesMode.PREFER_PROJECT
  repositories {
    maven("https://maven.pkg.st/")
  }
}

rootProject.name = "uuid"

gradleEnterprise {
  buildScan {
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    termsOfServiceAgree = "yes"
  }
}

// KotlinX extensions and support package.
include(":subprojects:uuid-core")
include(":subprojects:uuid-kotlinx")

enableFeaturePreview("STABLE_CONFIGURATION_CACHE")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
enableFeaturePreview("GROOVY_COMPILATION_AVOIDANCE")
