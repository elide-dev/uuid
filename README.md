# `elide-uuid`

[![Maven Central](https://img.shields.io/maven-central/v/dev.elide/elide-uuid.svg?label=Maven%20Central)](https://search.maven.org/artifact/dev.elide/elide-uuid)
[![CI](https://github.com/elide-dev/uuid/actions/workflows/push.yml/badge.svg)](https://github.com/elide-dev/uuid/actions/workflows/push.yml)
[![Elide](https://elide.dev/shield)](https://elide.dev)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![Kotlin](https://img.shields.io/badge/kotlin-WASM-yellow.svg?logo=kotlin&logoColor=yellow)](http://kotlinlang.org)
[![Kotlin/JS. IR supported](https://img.shields.io/badge/kotlin-IR-yellow?logo=kotlin&logoColor=yellow)](https://kotl.in/jsirsupported)
[![Contributor Covenant](https://img.shields.io/badge/Contributor%20Covenant-v1.4-ff69b4.svg)](CODE_OF_CONDUCT.md)
[![Reliability](https://sonarcloud.io/api/project_badges/measure?project=elide-dev_uuid&metric=reliability_rating)](https://sonarcloud.io/summary/new_code?id=elide-dev_uuid)
[![Maintainability](https://sonarcloud.io/api/project_badges/measure?project=elide-dev_uuid&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=elide-dev_uuid)
[![Security](https://sonarcloud.io/api/project_badges/measure?project=elide-dev_uuid&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=elide-dev_uuid)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=elide-dev_uuid&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=elide-dev_uuid)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=elide-dev_uuid&metric=bugs)](https://sonarcloud.io/summary/new_code?id=elide-dev_uuid)
[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2Felide-dev%2Fuuid.svg?type=shield)](https://app.fossa.com/projects/git%2Bgithub.com%2Felide-dev%2Fuuid?ref=badge_shield)
[![OpenSSF Best Practices](https://bestpractices.coreinfrastructure.org/projects/7689/badge)](https://bestpractices.coreinfrastructure.org/projects/7689)

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
- Add `implementation "dev.elide:elide-uuid:<version>"` as a dependency in your `commonMain` `sourceSets`

This library publishes Gradle module metadata. If you're using Gradle prior to version 6, you should have `enableFeaturePreview("GRADLE_METADATA")` in your settings.gradle(.kts).

### Contributing

Contributions are welcome! Once you file a PR, a bot will as you to sign the CLA (_Contributor License Agreement_) and to sign off your commits to establish your DCO
(_Developer Certificate of Origin_).

Checks run automatically on each PR. Review is requested automatically once your PR goes from draft to open.

### Additional checks

[![Quality gate](https://sonarcloud.io/api/project_badges/quality_gate?project=elide-dev_uuid)](https://sonarcloud.io/summary/new_code?id=elide-dev_uuid)

[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2Felide-dev%2Fuuid.svg?type=large)](https://app.fossa.com/projects/git%2Bgithub.com%2Felide-dev%2Fuuid?ref=badge_large)

#### This is a fork

Ben Asher's original library is [here](https://github.com/benasher44/uuid.git).

#### About Elide

This library is part of a larger polyglot framework and runtime called [Elide](https://github.com/elide-dev). You can learn more about Elide at [`elide.dev`](https://elide.dev).
