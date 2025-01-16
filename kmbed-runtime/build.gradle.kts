plugins {
    alias(libs.plugins.kotlin.multiplatform)
    `maven-publish`
}

kotlin {
    mingwX64()
    linuxX64()
    linuxArm64()
    macosX64()
    macosArm64()
    androidNativeX64()
    androidNativeArm64()
    androidNativeArm32()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    applyDefaultHierarchyTemplate()
    sourceSets {
        nativeMain {
            dependencies {
                implementation(libs.kotlinx.io.bytestring)
                implementation(libs.kotlinx.io.core)
                implementation(libs.multiplatform.mman)
            }
        }
    }
}