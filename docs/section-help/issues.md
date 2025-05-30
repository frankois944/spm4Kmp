# Known Issues

## Failed to store cache entry

The error message `Failed to store cache entry` is a Gradle cache issue that [can't be fixed](https://github.com/frankois944/spm4Kmp/issues/89), but can only be avoided by disabling the cache.

```
org.gradle.caching=false
org.gradle.configuration-cache=false
```
