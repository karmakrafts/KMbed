# kMbed

[![](https://git.karmakrafts.dev/kk/kmbed/badges/master/pipeline.svg)](https://git.karmakrafts.dev/kk/kmbed/-/pipelines)
[![](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Frepo.maven.apache.org%2Fmaven2%2Fdev%2Fkarmakrafts%2Fkmbed%2Fkmbed-runtime%2Fmaven-metadata.xml
)](https://git.karmakrafts.dev/kk/kmbed/-/packages)
[![](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Fcentral.sonatype.com%2Frepository%2Fmaven-snapshots%2Fdev%2Fkarmakrafts%2Fkmbed%2Fkmbed-runtime%2Fmaven-metadata.xml
)](https://git.karmakrafts.dev/kk/kmbed/-/packages)
[![](https://img.shields.io/badge/2.3.0-blue?logo=kotlin&label=kotlin)](https://kotlinlang.org/)
[![](https://img.shields.io/badge/documentation-black?logo=kotlin)](https://docs.karmakrafts.dev/kmbed-runtime)

kMbed is a Gradle/KMP plugin for embedding resources in KMP modules.  
The runtime provides an API similar to Android's R-class, zlib compression and [kotlinx.io](https://github.com/Kotlin/kotlinx-io) integration.

### How to use it

First, add the official Maven Central repository to your settings.gradle.kts:

```kotlin
pluginManagement {
    repositories {
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
```

Then add a dependency on the plugin in your root buildscript:

```kotlin
plugins {
    id("dev.karmakrafts.kmbed.kmbed-gradle-plugin") version "<version>"
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation("dev.karmakrafts.kmbed:kmbed-runtime:<version>")
            }
        }
    }
}
```