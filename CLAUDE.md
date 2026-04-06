# spm4Kmp

A Gradle plugin providing Swift Package Manager (SPM) integration for Kotlin Multiplatform (KMP) projects — an alternative to the deprecated CocoaPods plugin.

Plugin ID: `io.github.frankois944.spmForKmp`

## Project Structure

```
spm4Kmp/
├── plugin-build/plugin/src/main/java/io/github/frankois944/spmForKmp/
│   ├── SpmForKmpPlugin.kt          # Main plugin entry point
│   ├── config/                     # Plugin configuration classes
│   ├── definition/                 # Package definitions & dependency types
│   ├── tasks/apple/                # Gradle tasks (manifest, compile, cinterop, etc.)
│   ├── manifest/                   # Swift manifest handling
│   └── operations/                 # Core operations
├── plugin-build/plugin/src/functionalTest/  # Unit tests of the plugin (Gradle TestKit)
├── example/                        # Sample KMP project that directly uses the plugin-build source
│   ├── src/swift/                  # Custom Swift code
│   ├── SPM/                        # SPM working directory
│   ├── xcframework/                # Pre-built .xcframework binaries
│   └── iosApp/                     # Example iOS app
├── config/detekt/detekt.yml        # Detekt static analysis config
├── docs/                           # MkDocs full plugin documentation
│   ├── index.md                    # Overview and features
│   ├── setup.md                    # Getting started / requirements
│   ├── bridge.md                   # Swift↔Kotlin bridge guide
│   ├── exportingDependencies.md    # Exporting SPM deps to Kotlin
│   ├── usages/                     # Usage guides (multi-target, registry, etc.)
│   ├── references/                 # Config reference (swiftPackageConfig, dependency, etc.)
│   ├── migration/                  # Migration guides (from CocoaPods, 0.x)
│   └── section-help/               # FAQ, tips, known issues
└── gradle/libs.versions.toml       # Version catalog
```

## Build & Test Commands

```bash
./gradlew preMerge                                        # Full verification (tests + lint + validation)
./gradlew :plugin-build:plugin:check                      # Plugin tests + lint + detekt
./gradlew :plugin-build:plugin:functionalTest             # Run unit tests only
./gradlew :plugin-build:plugin:validatePlugins            # Validate plugin before publishing
./gradlew reformatAll                                     # Reformat all Kotlin code
```

## Key Versions

- Kotlin: 2.1.21
- Gradle: 8.x
- Java target: JVM 17 (CI uses JDK 21)
- Plugin version: see `plugin-build/gradle.properties`
- Xcode: 16.4 (CI)

## Code Style

- `kotlin.code.style=official`
- Indent: 4 spaces (Kotlin), 2 spaces (YAML)
- **ktlint** + **Detekt** enforced
- `explicitApi()` required — all public declarations need explicit visibility modifiers
- Run `./gradlew reformatAll` before committing

## Example Project

`example/` is a real KMP project that consumes the plugin directly from `plugin-build/` source (not from the published artifact). It demonstrates real-world usage including Firebase, binary frameworks, local packages, registry packages, and custom Swift bridges.

The example is also exercised in CI: `:example:iosSimulatorArm64Test` runs as part of `preMerge`.

## Documentation

Full plugin docs are in `docs/` (MkDocs).

**Core guides:**
- `docs/setup.md` — requirements (macOS, Xcode 16+, Kotlin 2.1.21+, Gradle 8.12+) and initial config
- `docs/bridge.md` — creating a Swift↔Kotlin native API bridge; bridge code lives in `src/swift/[cinteropName]/`
- `docs/bridgeWithDependencies.md` — using external SPM dependencies inside the bridge; handling linker errors
- `docs/exportingDependencies.md` — exporting ObjC-compatible Swift packages to Kotlin via `exportToKotlin = true`

**References (`docs/references/`):**
- `swiftPackageConfig.md` — all `swiftPackageConfig` properties (platform versions, paths, build flags, cinterop options, registry auth)
- `exportedPackageConfig.md` — `exportedPackageSettings` block (`includeProduct`, `isStatic`, `name`)
- `bridgeSettingsConfig.md` — per-target build settings (C, C++, Swift, linker)
- `dependency/dependencyConfig.md` — all dependency types: `remotePackageVersion`, `remotePackageBranch`, `remotePackageCommit`, `localPackage`, `localBinary`, `remoteBinary`, `registryPackage`
- `dependency/productName.md` — `ProductName` data class (`name`, `alias`)
- `dependency/productPackageConfig.md` — `add()` overloads with `exportToKotlin` flag
- `targetSettingsConfigs/` — `CSettingConfig`, `CxxSettingConfig`, `SwiftSettingConfig`, `LinkerSettingConfig`

**Usage guides (`docs/usages/`):**
- `multiTarget.md` — per-target configurations (iOS vs macOS, multiple `cinteropName`)
- `packageRegistry.md` — Swift Package Registry support (since v1.2.0) with token auth
- `importCLangFramework.md` — importing C-language xcFrameworks (experimental, `isCLang = true`)
- `largebridge.md` — structuring large bridges as local Swift packages
- `distribution.md` — distributing KMP libraries that depend on native SPM packages

**Migration (`docs/migration/`):**
- `migrateFromCocoapods.md` — step-by-step migration from the KMP CocoaPods plugin:
  1. **Gradle** — swap `kotlinCocoapods` for `spmForKmp` in `libs.versions.toml` and both `build.gradle.kts` files; replace the `cocoapods {}` block with `swiftPackageConfig {}` using `remotePackageVersion` dependencies
  2. **Xcode** — run `pod deintegrate`, add a new Run Script Phase at the top of Build Phases (`./gradlew :shared:embedAndSignAppleFrameworkForXcode`), update Other Linker Flags (`-framework shared`) and Framework Search Paths
  3. **Kotlin imports** — remove the `cocoapods.` prefix from all imports (recoverable via `packageDependencyPrefix` if needed)
- `migration_from_0.x.md` — breaking changes from versions < 1.0:
  - `copyDependenciesToApp` deleted — now the default behavior
  - `isIncludedInExportedPackage` deleted — replaced by `exportedPackageSettings { includeProduct }`
  - `linkerOpts`/`compilerOpts` removed from per-dependency config — only available at the root `swiftPackageConfig` level
  - Legacy `SwiftDependency` removed from public API

**Help (`docs/section-help/`):**
- `faq.md` — Pure Swift packages, ObjC compatibility, `module.modulemap` inspection
- `issues.md` — known issues: "Undefined symbol", Gradle cache failures, Xcode 16.3+ module error, Swift concurrency linker issues
- `tips.md` — build time reduction, CI caching, `swiftBinPath`/`swiftly`, iOS test concurrency workarounds

## Migration

### From CocoaPods (`docs/migration/migrateFromCocoapods.md`)

**1. `libs.versions.toml`**
- Under `[versions]`: add `spmForKmp = "x.y.z"`
- Under `[plugins]`: remove `kotlinCocoapods` entry, add `spmForKmp = { id = "io.github.frankois944.spmForKmp", version.ref = "spmForKmp" }`

**2. Root `build.gradle.kts`**
- Replace `alias(libs.plugins.kotlinCocoapods).apply(false)` with `alias(libs.plugins.spmForKmp).apply(false)`

**3. Shared module `build.gradle.kts`**
- Replace `alias(libs.plugins.kotlinCocoapods)` with `alias(libs.plugins.spmForKmp)`
- Remove the entire `cocoapods { }` block
- Wrap targets in `listOf(...).forEach { }` and add `it.swiftPackageConfig(cinteropName = "nativeBridge") { }` with `remotePackageVersion` dependencies replacing each `pod()`

**4. Xcode**
- Run `pod deintegrate` from the iOS app folder
- In Build Phases: add a **New Run Script Phase** at the very top containing:
  ```
  cd "$SRCROOT/../../"
  ./gradlew :shared:embedAndSignAppleFrameworkForXcode
  ```
- In Build Settings:
  - **Other Linker Flags**: add `-framework shared`
  - **Framework Search Paths**: add `$(SRCROOT)/../build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)`

**5. Kotlin imports**
- Remove the `cocoapods.` prefix: `import cocoapods.FirebaseCore.FIRApp` → `import FirebaseCore.FIRApp`
- To keep the old prefix, set `packageDependencyPrefix` in `swiftPackageConfig`

---

### From versions < 1.0 (`docs/migration/migration_from_0.x.md`)

| Removed | Replacement |
|---|---|
| `copyDependenciesToApp` | Now the default behavior — remove it |
| `isIncludedInExportedPackage` | Use `exportedPackageSettings { includeProduct = listOf(...) }` |
| `linkerOpts`/`compilerOpts` on dependency | Move to root `swiftPackageConfig { linkerOpts / compilerOpts }` |
| `SwiftDependency` (public API) | Removed — use typed dependency DSL (`remotePackageVersion`, etc.) |

## Testing

Plugin unit tests use Gradle TestKit and run real Gradle builds. Tests live in `plugin-build/plugin/src/functionalTest/` and cover scenarios like binary packages, local packages, remote packages, registry packages, error cases, etc.

**`IOSAppTest`** is the most important test — it is the end-to-end validation that everything works. It runs `xcodebuild` against the `example/iosApp` project (piped through `xcbeautify`) to build and test the full iOS app on the iPhone SE (3rd gen) simulator. It requires `GITEA_TOKEN` to be set (skipped otherwise) and has a 20-minute timeout.

CI runs on `macos-latest` (Xcode required for Swift compilation).

## CI/CD

GitHub Actions workflows:
- `pre-merge.yaml` — triggered on push to `main` and PRs; runs lint, tests, SonarQube
- `publish-plugin.yaml` — triggered on tag; publishes to Gradle Plugin Portal

Required secrets: `GRADLE_PUBLISH_KEY`, `GRADLE_PUBLISH_SECRET`, `SONAR_TOKEN`, `CODECOV_TOKEN`, `GITEA_TOKEN`
