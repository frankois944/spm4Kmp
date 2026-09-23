# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

`spmForKmp` is a Gradle plugin (ID `io.github.frankois944.spmForKmp`) that integrates Swift Package Manager dependencies into Kotlin Multiplatform projects targeting Apple platforms, and bridges user Swift code to Kotlin via cinterop. It replaces the deprecated CocoaPods plugin. Docs: https://spmforkmp.eu/

Building and testing requires **macOS with Xcode** (the plugin shells out to `swift`/`xcodebuild`). CI runs on `macos-latest` with Xcode 26 and JDK 21; bytecode targets Java 17.

## Repository layout

Gradle **composite build**:

- `plugin-build/` — included build; the plugin itself is `plugin-build/plugin/`. Plugin metadata (ID, `VERSION`, implementation class) lives in `plugin-build/gradle.properties` — bump `VERSION` there for releases. Both builds share `gradle/libs.versions.toml`.
- `example/` — KMP consumer (`:example` module of the root build) used as an integration test bed. It uses the real `firebase-ios-sdk`, binary/local/registry packages, `spmWorkingPath = example/SPM`, and `spmforkmp.enableTracing=true`.
- Root build only aggregates `preMerge` / `reformatAll` and lint config.
- `docs/` — documentation site sources (zensical, `zensical.toml`); `site/` is generated output. `docs/references/` documents the DSL — update it when the DSL changes.
- `BinaryPackageSource/` — Xcode project + `build.sh` that produce the dummy xcframeworks used by tests.

Plugin sources are in `plugin-build/plugin/src/main/java/` but are **Kotlin**. `explicitApi()` is enabled: every public declaration needs an explicit visibility modifier, and anything not part of the user DSL should be `internal`.

## Commands

From the repo root:

```bash
./gradlew preMerge                                   # everything CI gates on: example:check, plugin:check, validatePlugins
./gradlew :plugin-build:plugin:functionalTest        # plugin functional tests (Gradle TestKit, slow)
./gradlew :plugin-build:plugin:functionalTest --tests "*.BasicPackageTest"   # single test class
./gradlew :example:iosSimulatorArm64Test --tests "com.example.IosTest"       # example integration test
./gradlew :plugin-build:plugin:ktlintCheck :plugin-build:plugin:detektMain   # lint, as CI runs it
./gradlew reformatAll                                # ktlintFormat on root + plugin
```

- `check` on the plugin runs `detektMain` (Detekt with type resolution), not plain `detekt`. `maxIssues: 0` — any Detekt finding fails the build. ktlint has `ignoreFailures = true`, so run `reformatAll` rather than relying on it to fail. Config: `config/detekt/detekt.yml`.
- Functional tests run under JUnit 5 with JaCoCo instrumentation of the plugin-under-test classpath (workaround for gradle/gradle#1465, see `plugin-build/plugin/build.gradle.kts`). Pass `-PdisableFix=true` to skip it.
- Snapshot publishing: `./gradlew publishSnapshot -Psnapshot` (needs `GRADLE_PUBLISH_KEY`/`GRADLE_PUBLISH_SECRET`). Release publishing is triggered by pushing a git tag.

## Architecture

Entry point: `SpmForKmpPlugin.kt`. It registers `swiftPackageConfig`, a `NamedDomainObjectContainer<PackageRootDefinitionExtension>`, and wires everything in `afterEvaluate`.

Two DSL entry points feed the same container:

- **Current**: `KotlinNativeTarget.swiftPackageConfig(cinteropName) { … }` in `ExtensionHelper.kt` — sets `useExtension = true`; the plugin creates the missing cinterop itself.
- **Legacy**: `swiftPackageConfig { create("name") { … } }` — still supported but logs a migration warning.

Entries are merged by `internalName` (the cinterop name), so several targets can share one Swift package.

### Task graph (`tasks/ConfigAppleTargets.kt`, names in the `TASK_*` constants)

Per package:
1. `generateSwiftPackage` — writes `Package.swift` from the DSL (`manifest/`)
2. `generateRegistryFilePackage` — Swift package registry config
3. `resolveSwiftPackage` — `swift package resolve`; owns `scratch/artifacts` and `scratch/checkouts`

Per Apple target:
4. `compileSwiftPackage` — `swift build` for one triple; owns only `scratch/<triple>/<mode>`
5. `CopyPackageResources` — bundles resources into the app
6. `generateCInteropDefinition` — writes `.def` files consumed by Kotlin's `CInteropProcess`

Plus `generateExportableSwiftPackage` (per package, `mustRunAfter` the compile tasks): the manifest of dependencies the Xcode consumer project must add.

The plugin only configures cinterop tasks it owns (by name or `cinterop<InternalName>` prefix); foreign cinterops are left untouched (`ForeignCinteropTest`). On non-macOS hosts, cinterops get a fake `.def` file instead of the task chain.

### Key packages (`io.github.frankois944.spmForKmp`)

- `definition/` — user-facing DSL (`PackageRootDefinitionExtension`, dependency / product / exported / packageSetting / packageRegistry sub-DSLs)
- `tasks/apple/<task>/` — one directory per task: the task class plus a `ConfigureTask.kt` extension that wires its inputs
- `tasks/utils/` — task naming, `publishSafe` handling, tracing (`TaskTracer`), Gradle properties (`ExternalProperties.kt`)
- `operations/` — shelling out to `swift` / `xcodebuild` / the registry
- `manifest/` — `Package.swift` generation
- `config/` — target mapping (`AppleCompileTarget`), directory config, experimental Kotlin interop flags
- `utils/` — `@ExperimentalSpmForKmpFeature` opt-in annotation, plist/manifest parsing, checksums

User-facing Gradle properties: `spmforkmp.enableTracing` (HTML report under `spmForKmpTrace/`), `spmforkmp.hideLocalPackageMessage`, `spmforkmp.disableStartupFile`.

## Caching invariants — do not regress

Build cache and configuration cache support are deliberate (see issue #329 and `BuildCacheTest` / `ConfigurationCacheTest`):

- `CompileSwiftPackageTask` **must stay `@CacheableTask`**. Making it non-cacheable is not an acceptable fix for anything.
- No overlapping outputs between tasks. Dependency downloads belong to `ResolveSwiftPackageTask` (`@DisableCachingByDefault`), never to the per-target compile tasks. `Package.resolved` is an input, not an output.
- After `swift build`, symlinks in the build directory are dereferenced (broken ones deleted) so the cache entry can be packed.
- Keep configuration lazy: `tasks.register` + `configure {}`, no `.get()` on task providers at configuration time; predict output paths from config instead of realizing tasks. No `Project` access in task actions.
- TestKit + `--configuration-cache` on task **execution** fails with `NoClassDefFoundError` because of `withPluginClasspath()`, not the plugin. Execution-phase CC coverage comes from `:example`. Don't add CC execution tests in TestKit.

## Functional tests

`plugin-build/plugin/src/functionalTest/kotlin/`. Tests extend `utils/BaseTest` and build a synthetic KMP project with the fixture builder `fixture/SmpKMPTestFixture` (`KotlinSource` / `SwiftSource` / `withRawDependencies`), then run it with Gradle TestKit (`GradleBuilder.runner(...)`) and assert with TestKit Truth (`assertThat(result).task(":library:build").succeeded()`). Helpers for reading the generated manifest / `Package.resolved` are in `utils/TestHelpers.kt`.

- Fixture resources (dummy xcframeworks, local packages) are in `src/functionalTest/resources/`.
- Generated projects are kept under `plugin-build/plugin/build/functionalTest/` for inspection; `OpenFolderOnFailureExtension` opens them in Finder on failure.
- These tests resolve and compile real Swift packages — prefer running a single class while iterating.
