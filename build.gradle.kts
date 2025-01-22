val baseVersion: String = libs.versions.kmbed.get()

allprojects {
    group = "io.karma.kmbed"
    // @formatter:off
    version = System.getenv("CI_COMMIT_TAG")?.let { baseVersion }
        ?: "$baseVersion.${System.getenv("CI_PIPELINE_IID") ?: 0}"
    // @formatter:on
    repositories {
        mavenLocal()
        mavenCentral()
        google()
        maven("https://git.karmakrafts.dev/api/v4/projects/332/packages/maven") // multiplatform-mman
    }
}