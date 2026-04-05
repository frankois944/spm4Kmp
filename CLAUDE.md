# spm4Kmp — Claude Code Instructions

## Project Overview

**spm4Kmp** is a Gradle plugin that integrates Swift Package Manager (SPM) into Kotlin Multiplatform (KMP) projects targeting Apple platforms (iOS, macOS, tvOS, watchOS). It replaces the deprecated CocoaPods plugin.

- **Plugin ID:** `io.github.frankois944.spmForKmp`
- **Group:** `io.github.frankois944`
- **Current Version:** defined in `gradle.properties`
- **Documentation:** https://spmforkmp.eu/

## Project Structure

```
spm4Kmp/
├── plugin-build/           # Gradle plugin implementation (included build)
│   └── plugin/src/
│       ├── main/java/      # Plugin Kotlin source (io.github.frankois944.spmForKmp)
│       └── functionalTest/ # Gradle TestKit functional tests
├── example/                # Example KMP project using the plugin
│   ├── src/                # Kotlin Multiplatform sources
│   ├── iosApp/             # Swift/iOS app
│   └── SPM/                # Swift Package Manager directory
├── docs/                   # MkDocs documentation sources
├── config/detekt/          # Detekt static analysis configuration
└── gradle/                 # Gradle version catalog (libs.versions.toml)
```

## Build & Test Commands

Run from the **root** of the project:

```bash
# Build the plugin
./gradlew :plugin-build:plugin:build

# Run all checks (linting + tests) — run before every PR
./gradlew :preMerge

# Run functional tests only
./gradlew :plugin-build:plugin:functionalTest

# Run example project tests
./gradlew :example:iosSimulatorArm64Test
```

## Code Quality

```bash
# Lint check (ktlint)
./gradlew :plugin-build:plugin:ktlintCheck

# Auto-format Kotlin code
./gradlew ktlintFormat
./gradlew reformatAll

# Static analysis (Detekt)
./gradlew :plugin-build:plugin:detektMain

# Code coverage report (JaCoCo)
./gradlew :plugin-build:plugin:jacocoTestReport
```

## Key Source Locations

| Path | Description |
|------|-------------|
| `plugin-build/plugin/src/main/java/.../SpmForKmpPlugin.kt` | Plugin entry point |
| `plugin-build/plugin/src/main/java/.../tasks/` | Gradle task implementations |
| `plugin-build/plugin/src/main/java/.../definition/` | DSL extension definitions |
| `plugin-build/plugin/src/functionalTest/` | Functional tests (Gradle TestKit) |
| `example/build.gradle.kts` | Example plugin configuration |
| `gradle/libs.versions.toml` | Centralized dependency versions |

## Technology Stack

- **JVM:** Java 17+ (target compatibility)
- **Kotlin:** 2.1.21
- **Gradle:** 8.12.1
- **Testing:** JUnit 6, Gradle TestKit
- **Linting:** ktlint 14.2.0, Detekt 1.23.8
- **CI:** GitHub Actions (macOS-latest, Xcode 16.4, JDK 21)

## Plugin Architecture & Key Concepts

### How the Plugin Works

1. User declares SPM dependencies via the `swiftPackageConfig {}` DSL on each Apple Kotlin target
2. Plugin generates a `Package.swift` manifest from the declared dependencies
3. Plugin compiles the Swift package using `swift build`
4. Plugin generates C-interop definition files so Kotlin can call Swift/ObjC APIs
5. Plugin optionally generates a local exportable Swift package for Xcode consumption

### Swift–Kotlin Bridge

- Bridge source lives in `src/swift/[cinteropName]/` (or `src/swift/[targetName]/`)
- Bridge code must use `@objc`/`@objcMembers` + `public` visibility to be exported to Kotlin
- Pure Swift types are **not** directly usable from Kotlin — they require an ObjC bridge wrapper

### Dependency Types

| DSL method | Description |
|---|---|
| `remotePackageVersion()` | Remote SPM package pinned to a version |
| `remotePackageBranch()` | Remote SPM package on a branch |
| `remotePackageCommit()` | Remote SPM package at a commit hash |
| `localPackage()` | Local Swift package folder |
| `localBinary()` | Local `.xcframework` |
| `remoteBinary()` | Remote zipped `.xcframework` |
| `registryPackage()` | Package from a Swift package registry |

### Exporting to Kotlin

- Only ObjC-compatible products can be exported directly
- Set `exportToKotlin = true` on the product to generate a C-interop binding
- Pure Swift packages must have an ObjC bridge written in `src/swift/[cinteropName]/`

### Key DSL Options (`swiftPackageConfig`)

- `cinteropName` / `groupName` — name of the cinterop module
- `customPackageSourcePath` — custom Swift source directory (default: `src/swift`)
- `minIos`, `minMacos`, `minTvos`, `minWatchos` — platform minimum versions (set to `null` to skip from manifest)
- `toolsVersion` — Swift tools version (default: 5.9)
- `debug` — build in debug mode (default: false)
- `packageDependencyPrefix` — prefix applied to all package dependency names
- `linkerOpts`, `compilerOpts` — custom linker/compiler flags for C-interop export
- `spmWorkingPath` — SPM build artifacts directory (set outside `build/` to avoid clean rebuilds)
- `sharedCachePath`, `sharedConfigPath`, `sharedSecurityPath` — override default SPM shared directories
- `exportedPackageSettings` — configure the generated local package for Xcode
- `bridgeSettings` — C/C++/Swift/linker build settings for the bridge target
- `swiftBinPath` — custom Swift binary path (e.g. to target a specific Xcode or toolchain)
- `toolchain` — custom Swift toolchain identifier
- `useXcodeBuild` — use `xcodebuild` instead of `swift build` (default: false)
- `registry()` — configure a private Swift package registry with credentials
- `strictEnums`, `nonStrictEnums` — control Kotlin enum generation for ObjC enums
- `foreignExceptionMode` — wrap ObjC exceptions into Kotlin `ForeignException`
- `disableDesignatedInitializerChecks` — allow calling non-designated ObjC initializers as `super()`
- `userSetupHint` — custom message appended to linker error output
- `newPublicationInteroperability` — experimental Kotlin 2.3.20+ interop mode

## Development Conventions

- All plugin source code is in `plugin-build/` as an included build
- Functional tests use real Gradle projects — test resources live in `plugin-build/plugin/src/functionalTest/resources/`
- Kotlin code style follows ktlint rules with experimental rules enabled
- Run `./gradlew :preMerge` before submitting any PR — this runs linting and all tests
- Never skip detekt or ktlint checks; fix violations instead

## Known Issues & Gotchas

- **Kotlin ≤ 2.1.20 + Xcode ≥ 16.3** — incompatible; use Xcode 16.2 or upgrade to Kotlin 2.1.21+
- **"Undefined symbol" at build or runtime** — the native library needs to be added to the Xcode project
- **Gradle caching issues** — disable with `org.gradle.caching=false` and `org.gradle.configuration-cache=false`
- **Swift concurrency** — requires iOS 15+ minimum deployment target; iOS 16+ for tests
- **"SWIFT_TYPEDEFS only" in Kotlin** — the exported product is not ObjC-compatible; inspect `module.modulemap` under `build/spmKmpPlugin/[cinteropName]/scratch/`
- **Reducing build times** — set `spmWorkingPath` to a path outside `build/` so SPM artifacts survive `./gradlew clean`

## Publishing

```bash
# Publish to Gradle Plugin Portal
./gradlew :plugin-build:plugin:publishPlugins

# Publish snapshot
./gradlew publishSnapshot
```
