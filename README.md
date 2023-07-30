# A Kotlin Multiplatform UUID

[![Maven Central](https://img.shields.io/maven-central/v/dev.elide/uuid.svg?label=mavenCentral%28%29)](https://search.maven.org/artifact/dev.elide/uuid)
[![Build](https://github.com/elide-dev/uuid/actions/workflows/step.build.yml/badge.svg)](https://github.com/elide-dev/uuid/actions/workflows/step.build.yml)
[![Contributor Covenant](https://img.shields.io/badge/Contributor%20Covenant-v1.4%20adopted-ff69b4.svg)](CODE_OF_CONDUCT.md)
[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2Felide-dev%2Fuuid.svg?type=shield)](https://app.fossa.com/projects/git%2Bgithub.com%2Felide-dev%2Fuuid?ref=badge_shield)

K/N doesn't have a UUID yet. This brings a UUID that matches UUIDs on various platforms:

- iOS/Mac: `NSUUID`
- Java: `java.util.UUID`

### `UUID`

- Frozen
- Thread-safe (thread-safe randomness in native)
- Adheres to RFC4122
- Tested
- Tested against macOS/iOS UUID to verify correctness

### Setup

In your build.gradle(.kts):

- Add `mavenCentral()` to your repositories
- Add `implementation "dev.elide:uuid:<version>"` as a dependency in your `commonMain` `sourceSets`.

This library publishes gradle module metadata. If you're using Gradle prior to version 6, you should have `enableFeaturePreview("GRADLE_METADATA")` in your settings.gradle(.kts).

### Future Goals

- Develop UUID functionality that can be contributed back to the Kotlin stdlib (see latest issues, PRs, and CHANGELOG.md for updates)


## License
[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2Felide-dev%2Fuuid.svg?type=large)](https://app.fossa.com/projects/git%2Bgithub.com%2Felide-dev%2Fuuid?ref=badge_large)