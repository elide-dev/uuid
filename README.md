# `elide-uuid`

[![Maven Central](https://img.shields.io/maven-central/v/dev.elide/elide-uuid.svg?label=Maven%20Central)](https://search.maven.org/artifact/dev.elide/elide-uuid)
[![CI](https://github.com/elide-dev/uuid/actions/workflows/push.yml/badge.svg)](https://github.com/elide-dev/uuid/actions/workflows/push.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![Kotlin](https://img.shields.io/badge/kotlin-WASM-yellow.svg?logo=kotlin&logoColor=yellow)](http://kotlinlang.org)
[![Kotlin/JS. IR supported](https://img.shields.io/badge/kotlin-K2-yellow?logo=kotlin&logoColor=yellow)](https://kotl.in/jsirsupported)
[![Contributor Covenant](https://img.shields.io/badge/Contributor%20Covenant-v1.4-ff69b4.svg)](CODE_OF_CONDUCT.md)
[![Reliability](https://sonarcloud.io/api/project_badges/measure?project=elide-dev_uuid&metric=reliability_rating)](https://sonarcloud.io/summary/new_code?id=elide-dev_uuid)
[![Maintainability](https://sonarcloud.io/api/project_badges/measure?project=elide-dev_uuid&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=elide-dev_uuid)
[![Security](https://sonarcloud.io/api/project_badges/measure?project=elide-dev_uuid&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=elide-dev_uuid)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=elide-dev_uuid&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=elide-dev_uuid)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=elide-dev_uuid&metric=bugs)](https://sonarcloud.io/summary/new_code?id=elide-dev_uuid)
[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2Felide-dev%2Fuuid.svg?type=shield)](https://app.fossa.com/projects/git%2Bgithub.com%2Felide-dev%2Fuuid?ref=badge_shield)
[![OpenSSF Best Practices](https://bestpractices.coreinfrastructure.org/projects/7689/badge)](https://bestpractices.coreinfrastructure.org/projects/7689)

K/N UUID. This brings a UUID that matches UUIDs on various platforms:

- iOS/Mac: `NSUUID`
- Java: `java.util.UUID`
- Native: Source implementation

### Features

- Adheres to RFC4122
- Support for `uuid3`, `uuid4`, and `uuid5`
- Frozen and thread-safe (thread-safe randomness in native)
- Tested on all platforms, and against macOS/iOS UUID to verify correctness
- Aggressively optimized for performance and safety
- Updated for new versions of Kotlin
- Zero dependencies (only `stdlib`)
- 🆕 All Kotlin targets, including WASM/WASI and Android/Native
- 🆕 Serializable on JVM
- 🆕 Serializable with KotlinX
- 🆕 Zero-overhead model

### Setup

**In your `build.gradle(.kts)`:**

```kotlin
dependencies {
  implementation("dev.elide:elide-uuid:<version>")
}
```

**From an Elide application:**
```kotlin
dependencies {
  implementation(elide.uuid)
}
```

**To enable KotlinX Serialization support:**

```kotlin
dependencies {
  implementation("dev.elide:elide-uuid-kotlinx:<version>")
}
```

### Usage

From any Kotlin source set:
```kotlin
val uuid = uuid4()
println(uuid.toString())
// 1b4e28ba-2fa1-11d2-883f-0016d3cca427
```

When KotlinX Serialization support is included:
```kotlin
@Serializable data class Sample(
  @Serializable(with = UUIDSerializer::class) val uuid: Uuid,
)

val sample = Sample(uuid = uuid4())
println(Json.encodeToString(Sample.serializer(), uuid.toString()))
// {"uuid": "1b4e28ba-2fa1-11d2-883f-0016d3cca427"}
```

#### "Zero-overhead" abstraction

This library is designed to offer a compile-time option for dealing with UUIDs; internally, UUIDs are stored as a simple
`Pair<Long, Long>` to minimize allocations, and the main `Uuid` class is a `@JvmInline value class`.

Thus, your UUIDs are strictly typed and validated but also remain very lightweight.

The main UUID library uses exactly zero dependencies, and even omits a `stdlib` dependency by default. You must bring
your own before shipping a target with this library.

#### Serialization

Both JVM serialization and KotlinX serialization are supported out of the box.

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
