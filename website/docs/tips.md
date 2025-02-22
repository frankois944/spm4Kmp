# Tips

## Reduce Build Time

- **Since the version 0.4.0**

[spmWorkingPath](references/swiftPackageConfig.md#spmworkingpath) has been added to change the path to Swift Package working file.

By setting [spmWorkingPath](https://github.com/frankois944/spm4Kmp/blob/cf80e65b3076d9e0bcd94a847e1209d4b9b91141/example/build.gradle.kts#L108C1-L108C104) outside the build folder, the working files won't be removed if you clean the project.

Swift Package Manager has its own cache, so it's fine to detach it from the Kotlin build folder.

### CI/CD Caching

Add to your cache the content of the `build/spmKmpPlugin` folder or the `spmWorkingDir` value if set.

Also, check my [GitHub action workflow](https://github.com/frankois944/spm4Kmp/blob/main/.github/workflows/pre-merge.yaml) where I build the example app with cached built files.

## Firebase

An [full example](https://github.com/frankois944/FirebaseKmpDemo) of how to implement Firebase with the plugin
