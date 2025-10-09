# Import Apple Clang xcFramework

The plugin supports the import of binary xcFrameworks based on the C language, usually created from Xcode.

!!! warning

    This feature is experimental and not really tested on all kinds of xcframeworks based on the C language.

    If you encounter any problems, please [open an issue](https://github.com/frankois944/spm4Kmp/issues).

The following example is an import of [HevSocks5Tunnel](https://github.com/heiher/hev-socks5-tunnel).

<details>
<summary>Headers embedded by HevSocks5Tunnel</summary>
```cpp
/*
============================================================================
Name        : hev-main.h
Author      : hev <r@hev.cc>
Copyright   : Copyright (c) 2019 - 2023 hev
Description : Main
============================================================================
*/

#ifndef __HEV_MAIN_H__
#define __HEV_MAIN_H__

#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

/**
* hev_socks5_tunnel_main:
* @config_path: config file path
* @tun_fd: tunnel file descriptor
*
* Start and run the socks5 tunnel, this function will blocks until the
* hev_socks5_tunnel_quit is called or an error occurs.
*
* Returns: returns zero on successful, otherwise returns -1.
*
* Since: 2.4.6
*/
int hev_socks5_tunnel_main (const char *config_path, int tun_fd);

/**
* hev_socks5_tunnel_main_from_file:
* @config_path: config file path
* @tun_fd: tunnel file descriptor
*
* Start and run the socks5 tunnel, this function will blocks until the
* hev_socks5_tunnel_quit is called or an error occurs.
*
* Returns: returns zero on successful, otherwise returns -1.
*
* Since: 2.6.7
*/
int hev_socks5_tunnel_main_from_file (const char *config_path, int tun_fd);

/**
* hev_socks5_tunnel_main_from_str:
* @config_str: string config
* @config_len: the byte length of string config
* @tun_fd: tunnel file descriptor
*
* Start and run the socks5 tunnel, this function will blocks until the
* hev_socks5_tunnel_quit is called or an error occurs.
*
* Returns: returns zero on successful, otherwise returns -1.
*
* Since: 2.6.7
*/
int hev_socks5_tunnel_main_from_str (const unsigned char *config_str,
unsigned int config_len, int tun_fd);

/**
* hev_socks5_tunnel_quit:
*
* Stop the socks5 tunnel.
*
* Since: 2.4.6
*/
void hev_socks5_tunnel_quit (void);

/**
* hev_socks5_tunnel_stats:
* @tx_packets (out): transmitted packets
* @tx_bytes (out): transmitted bytes
* @rx_packets (out): received packets
* @rx_bytes (out): received bytes
*
* Retrieve tunnel interface traffic statistics.
*
* Since: 2.6.5
*/
void hev_socks5_tunnel_stats (size_t *tx_packets, size_t *tx_bytes,
size_t *rx_packets, size_t *rx_bytes);

#ifdef __cplusplus
}
#endif

#endif /* __HEV_MAIN_H__ */
```
</details>

## Gradle

```kotlin title="build.gradle.kts"
remoteBinary(
    url = uri("https://github.com/wanliyunyan/HevSocks5Tunnel/releases/download/2.10.0/HevSocks5Tunnel.xcframework.zip"),
    packageName = "HevSocks5Tunnel",
    exportToKotlin = true,
    checksum = "f66fc314edbdb7611c5e8522bc50ee62e7930f37f80631b8d08b2a40c81a631a",
    isCLang = true, // Required
)
```

## Kotlin

```kotlin title="kotlin.iosMain.kt"
import HevSocks5Tunnel.hev_socks5_tunnel_quit

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
fun test() {
    hev_socks5_tunnel_quit()
}
```

## Swift (Optional)

You can also import the framework in Swift.

```swift title="MyBridge.swift"
import HevSocks5Tunnel
@objc public class MySwiftClass: NSObject {
    public func cMethod() {
        hev_socks5_tunnel_quit()
    }
}
```
