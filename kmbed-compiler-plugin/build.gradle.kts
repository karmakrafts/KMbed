plugins {
    alias(libs.plugins.kotlin.jvm)
    `maven-publish`
}

dependencies {
    compileOnly(libs.kotlin.compiler)
    compileOnly(libs.autoService.annotations)
}