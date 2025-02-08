# KMbed

KMbed is a Gradle/KMP plugin for embedding resources in KMP modules.  
The runtime provides an API similar to Android's R-class, zlib compression and [kotlinx.io](https://github.com/Kotlin/kotlinx-io) integration.

### Multiplatform support

The following KMP targets are currently supported:
* JVM
* JS
* Windows x64
* Linux x64
* Linux arm64
* macOS x64
* macOS arm64
* iOS x64
* iOS arm64

### How to use it

Using KMbed is as simple as applying the KMbed Gradle Plugin and the runtime.

```kotlin
plugins {
    id("io.karma.kmbed.kmbed-gradle-plugin") version "<version>"
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation("io.karma.kmbed:kmbed-runtime:<version>")
            }
        }
    }
}
```

### How to configure it

The following configuration properties are avaiable through the kmbed project extension:

```kotlin
kmbed {
    // Allows changing the namespace (package name) of the generated resources for the current module
    resourceNamespace = "com.example.foo"
    // Allows force-disabling resource compression in the resource compiler
    compression = false
    // Allows adjusting the threshold at which resources are compressed (in bytes)
    compressionThreshold = 256
}
```