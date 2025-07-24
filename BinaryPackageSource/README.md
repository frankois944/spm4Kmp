# Dummy XCFramework for Testing Local Binary Import
#### This repository contains a dummy XCFramework for testing local binary imports in your project. The framework is pre-built and available in the repository for immediate use in tests.

🛠 Usage
Requirements
Xcode command-line tools (xcodebuild)

macOS (for building XCFrameworks)

1. Using the Pre-Built XCFramework
   The XCFramework is already included in the repository and can be found at:

```text
../plugin-build/plugin/src/functionalTest/resources/
```
Simply reference it in your tests.

---
2. Updating the XCFramework
   If you need to modify the source code and regenerate the framework:

Update the source code in the relevant files.

Run the build script:

```sh
 ./build.sh
```
This generates the XCFramework in:

```text
../plugin-build/plugin/src/functionalTest/resources/
```
Update the [packageVersion](/build.sh) in the script to avoid caching issues.
(If you don’t change it, you might get a cached version.)

Clear build caches (if needed)
If you encounter caching problems, delete temporary build directories and rerun ./build.sh.

---
3. Updating Checksums (For Remote Binary Tests)
   When testing remote binary imports, update the checksum in the test after running:

```sh
 ./build.sh
```
(Not required for local framework updates.)
