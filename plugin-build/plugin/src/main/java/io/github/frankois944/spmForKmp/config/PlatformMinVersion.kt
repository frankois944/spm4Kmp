package io.github.frankois944.spmForKmp.config

// Minimum deployment targets used when the DSL does not set one.
//
// They track the oldest version the current Apple SDKs still accept: the `swiftbuild` build
// system validates the deployment target against that range and fails the build when it is
// lower, where the deprecated `native` one silently accepted anything the compiler could emit.
//
// A project that needs to deploy lower can still set the value explicitly; the build then warns
// when the installed SDK no longer supports it, see `ExecOperations.getSdkDeploymentFloor`.

/**
 * DEFAULT_MIN_IOS_VERSION: 15.0
 */
internal const val DEFAULT_MIN_IOS_VERSION = "15.0"

/**
 * DEFAULT_MIN_TV_OS_VERSION: 15.0
 */
internal const val DEFAULT_MIN_TV_OS_VERSION = "15.0"

/**
 * DEFAULT_MIN_WATCH_OS_VERSION: 4.0
 *
 * Deliberately left below the floor the watchOS SDK reports (9.0), because raising it would drop
 * `watchosArm32` (armv7k) on the toolchains where it still works: armv7k needs a deployment
 * target below 9.0.
 *
 * On Xcode 27 the point is moot — its watchOS SDK cannot emit armv7k at any deployment target,
 * on either build system, and fails with "watchOS 9.0.0 and above does not support emitting
 * binaries or IR for armv7k" (the version in that message is the SDK's, not the target's).
 * So `watchosArm32` needs an older Xcode, and this default keeps it buildable there.
 */
internal const val DEFAULT_MIN_WATCH_OS_VERSION = "4.0"

/**
 * DEFAULT_MIN_MAC_OS_VERSION: 12.0
 */
internal const val DEFAULT_MIN_MAC_OS_VERSION = "12.0"

/**
 * DEFAULT_TOOL_VERSION: 5.9
 */
internal const val DEFAULT_TOOL_VERSION = "5.9"
