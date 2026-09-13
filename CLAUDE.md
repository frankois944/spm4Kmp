# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

`spmForKmp` is a Gradle plugin (ID `io.github.frankois944.spmForKmp`) that integrates Swift Package Manager dependencies into Kotlin Multiplatform projects targeting Apple platforms, and bridges user Swift code to Kotlin via cinterop. It replaces the deprecated CocoaPods plugin. Docs: https://spmforkmp.eu/

Building and testing requires **macOS with Xcode** (the plugin shells out to `swift`/`xcodebuild`). CI uses JDK 21; bytecode targets Java 17.

## Repository layout

This is a Gradle **composite build**:

- `plugin-build/` — included build containing the actual plugin in `plugin-build/plugin/`. Plugin metadata (ID, VERSION, implementation class) lives in `plugin-build/gradle.properties`.
- `example/` — KMP consumer project used as an integration test bed (`:example` module of the root build).
- Root build only aggregates lint/format/preMerge tasks.
- `docs/` — documentation site sources (built with zensical, `zensical.toml`); `site/` is generated output.

Plugin sources are in `plugin-build/plugin/src/main/java/` but are Kotlin (`explicitApi()` is enabled — all public API needs explicit visibility modifiers).

## Commands

From the repo root:

```bash
./gradlew preMerge                                   # all tests + validation (example:check, plugin:check, validatePlugins)
./gradlew :plugin-build:plugin:functionalTest        # plugin functional tests (Gradle TestKit)
./gradlew :plugin-build:plugin:functionalTest --tests "*.BasicPackageTest"   # single test class
./gradlew :example:iosSimulatorArm64Test --tests "com.example.IosTest"       # example integration test
./gradlew :plugin-build:plugin:ktlintCheck :plugin-build:plugin:detektMain   # lint (what CI runs)
./gradlew reformatAll                                # ktlintFormat on root + plugin
```

Notes:
- `check` on the plugin depends on `detektMain` (Detekt with type resolution), not plain `detekt`. Detekt config: `config/detekt/detekt.yml`.
- Functional tests run under JUnit 5 with JaCoCo instrumentation of the plugin-under-test classpath (workaround for gradle/gradle#1465, see `plugin-build/plugin/build.gradle.kts`). Pass `-PdisableFix=true` to skip that instrumentation.
- Snapshot publishing: `./gradlew publishSnapshot -Psnapshot` (needs `GRADLE_PUBLISH_KEY`/`GRADLE_PUBLISH_SECRET`).

## Architecture

Entry point: `SpmForKmpPlugin` (`plugin-build/plugin/src/main/java/io/github/frankois944/spmForKmp/SpmForKmpPlugin.kt`). It registers a `swiftPackageConfig` extension — a `NamedDomainObjectContainer<PackageRootDefinitionExtension>` — and wires everything in `afterEvaluate`.

Per configured cinterop and Apple target, a task chain is created (see `tasks/ConfigAppleTargets.kt` and the `TASK_*` constants in the plugin class):

1. `generateSwiftPackage` — generates a `Package.swift` manifest from the DSL (`manifest/`)
2. `compileSwiftPackage` — compiles the local Swift package with SPM per target
3. `generateCInteropDefinition` — produces `.def` files consumed by Kotlin's `CInteropProcess` tasks
4. Auxiliary tasks: `generateExportableSwiftPackage` (dependencies exported to the Xcode consumer project), `generateRegistryFilePackage` (Swift package registry support), `CopyPackageResources` (bundle resources into the app)

Key packages under `io.github.frankois944.spmForKmp`:

- `definition/` — the user-facing DSL (`PackageRootDefinitionExtension`, dependency/product/packageSetting/packageRegistry sub-DSLs)
- `tasks/apple/` — task implementations, one directory per task
- `operations/` — shell interaction with `swift`/`xcodebuild` toolchain
- `manifest/` — `Package.swift` generation
- `config/` — target mapping (`AppleCompileTarget`) and internal config models

## Functional tests

Located in `plugin-build/plugin/src/functionalTest/`. Tests build synthetic KMP projects via the fixture builder `fixture/SmpKMPTestFixture.kt` (with `KotlinSource`/`SwiftSource`) and run them with Gradle TestKit. `utils/BaseTest.kt` is the common base; `OpenFolderOnFailureExtension` opens the generated test project on failure. These tests are slow (they resolve and compile real Swift packages).
