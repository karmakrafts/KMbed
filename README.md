# KMbed

KMbed is a Gradle/KMP plugin for embedding resources in Kotlin/Native executables.  
The runtime provides an API similar to Android's R-class, zlib compression and [kotlinx.io](https://github.com/Kotlin/kotlinx-io) integration.

### How to use it

Using KMbed is as simple as applying the KMbed Gradle Plugin and the Kotlin/Native runtime.

```kotlin
plugins {
    id("io.karma.kmbed.kmbed-gradle-plugin") version "<version>"
}

kotlin {
    sourceSets {
        nativeMain {
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