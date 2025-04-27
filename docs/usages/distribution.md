# Distribute Kotlin Library

## Release A Library Using The Plugin

We can distribute to other users a Kotlin library using native or third-party dependency, some requirements are necessary for Apple targets.

### Requirement

The user must :

- add the same native dependency used by the Kotlin library to his Xcode project.
- use the same version as the library use.

It will fix issues with linking, missing resources, App Store compliance, and more.

!!! note

    The user **doesn't have access** to the Kotlin library source code, which is great!

## Example

A Compose Multiplatform Component library using a native video player.

``` Kotlin title="commonMain/kotlin/KmpPlayer.kt"
@Composable
public expect fun KmpPlayer(modifier: Modifier = Modifier, url: String)
```

### Android
For Android, it uses [Exoplayer](https://github.com/google/ExoPlayer).

#### Gradle

``` Kotlin title="library/build.gradle.kts"
androidMain.dependencies {
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.dash)
    implementation(libs.media3.ui)
}
```

#### AndroidMain

``` Kotlin title="androidMain/kotlin/KmpPlayer.kt"
@Composable
public actual fun KmpPlayer(modifier: Modifier, url: String) {

    val context = LocalContext.current

    // Initialize ExoPlayer
    val exoPlayer = ExoPlayer.Builder(context).build()

    val mediaSource = remember(url) {
        MediaItem.fromUri(url)
    }


    LaunchedEffect(url) {
        exoPlayer.setMediaItem(mediaSource)
        exoPlayer.prepare()
    }

    // Manage lifecycle events
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
            }
        },
        modifier = modifier
    )
}
```


### iOS

For iOS, it uses [KSPlayer](https://github.com/kingslay/KSPlayer), it's a pure Swift library.

#### Gradle

``` Kotlin title="library/build.gradle.kts"
swiftPackageConfig {
    create("appleDeps") {
        minIos = "13.0"
        minMacos = "10.15"
        minTvos = "13.0"
        minWatchos = "2.0"
        dependency {
            remotePackageBranch(
                url = URI("https://github.com/kingslay/KSPlayer"),
                products = {
                    add("KSPlayer")
                },
                branch = "main"
            )
        }
    }
}
```

#### Bridge

Some tips [here](../tips.md#working-with-objcnamesclasses-types).

``` Swift title="src/swift/appleDeps/MEPlayerController.swift"
import Foundation
import KSPlayer

@objcMembers public class MEPlayerController: NSObject {
     private let player = IOSVideoPlayerView()

    override init() {
       super.init()
       KSOptions.secondPlayerType = KSMEPlayer.self
       player.delegate = self
       player.autoresizingMask = [.flexibleWidth, .flexibleHeight]
       player.contentMode = .scaleAspectFill
    }

    public func setMediaItem(videoUrl: URL) {
       player.set(
          url: videoUrl,
          options: KSOptions()
      )
    }

    public var playerView: NSObject {
       player
    }

    public func releasePlayer() {
       player.resetPlayer()
       player.removeFromSuperview()
    }
}
```

#### IOSMain

``` Kotlin title="iosMain/kotlin/KmpPlayer.kt"
import appleDeps.MEPlayerController

@Composable
public actual fun KmpPlayer(modifier: Modifier, url: String) {

    val playerController = MEPlayerController()

    val mediaSource = remember(url) {
        NSURL.URLWithString(url)
    }

    LaunchedEffect(url) {
        if (mediaSource == null) {
            throw Exception("Bad input URL")
        }
        playerController.setMediaItemWithVideoUrl(videoUrl = mediaSource)
    }

    DisposableEffect(Unit) {
        onDispose {
            playerController.releasePlayer()
        }
    }

    UIKitView(
        factory = {
            playerController.playerView() as UIView
        },
        modifier = modifier,
        update = {
        }
    )
}
```

#### Requirement

The setup guide of your library must contain the rule that [KSPlayer](https://github.com/kingslay/KSPlayer) package must be added to his Xcode project with the explicit version.

### Sample

The sample is [available](https://github.com/frankois944/spmForKmpWithDistribution).

- Run the command `./gradlew publishToMavenLocal --no-configuration-cache` on the repository root.
- Open the project `sampleApp` and test the application
