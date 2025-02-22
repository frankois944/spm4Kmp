# Tips

## Reduce Build Time

- **Since the version 0.4.0**

[spmWorkingPath](references/swiftPackageConfig.md#spmworkingpath) has been added to change the path to Swift Package working file.

By settings [spmWorkingPath](references/swiftPackageConfig.md#spmworkingpath) outside of the build folder, the working files won't be removed if you clean the project.


### CI/CD Caching

Add to your cache the content of `build/spmKmpPlugin` folder or the `spmWorkingDir` value if set.

Also, check my [GitHub action workflow](https://github.com/frankois944/spm4Kmp/blob/main/.github/workflows/pre-merge.yaml) where I build the example app with cached built files.

