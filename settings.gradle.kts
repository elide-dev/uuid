/*
 * Copyright (c) 2023 Elide Ventures, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
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
  id("com.gradle.enterprise") version("3.13")
  id("org.gradle.toolchains.foojay-resolver-convention") version("0.4.0")
}

dependencyResolutionManagement {
  repositoriesMode.set(
    RepositoriesMode.PREFER_PROJECT
  )
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

val cacheUsername: String? by settings
val cachePassword: String? by settings
val cachePush: String? by settings
val remoteCache = System.getenv("GRADLE_CACHE_REMOTE")?.toBoolean() ?: true
val localCache = System.getenv("GRADLE_CACHE_LOCAL")?.toBoolean() ?: true

if (remoteCache || localCache) {
  buildCache {
    local {
        isEnabled = localCache
        directory = "$rootDir/build/cache/"
        removeUnusedEntriesAfterDays = 30
    }
    if (!cacheUsername.isNullOrBlank() && !cachePassword.isNullOrBlank()) {
      remote<HttpBuildCache> {
        isEnabled = remoteCache
        isPush = (cachePush ?: System.getenv("GRADLE_CACHE_PUSH")) == "true"
        isUseExpectContinue = true
        url = uri(System.getenv("CACHE_ENDPOINT") ?: "https://gradle.less.build/cache/generic/")
        credentials {
          username = cacheUsername ?: System.getenv("GRADLE_CACHE_USERNAME") ?: "apikey"
          password = cachePassword ?: System.getenv("GRADLE_CACHE_PASSWORD") ?: error("Failed to resolve cache password")
        }
      }
    }
  }
}

enableFeaturePreview("STABLE_CONFIGURATION_CACHE")

