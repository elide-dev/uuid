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

enableFeaturePreview("STABLE_CONFIGURATION_CACHE")

