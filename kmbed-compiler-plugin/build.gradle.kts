plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    compileOnly(libs.kotlin.compiler)
    compileOnly(libs.autoService.annotations)
}