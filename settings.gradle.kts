rootProject.name = "kmbed"

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

include("kmbed-runtime")
include("kmbed-gradle-plugin")
include("kmbed-compiler-plugin")