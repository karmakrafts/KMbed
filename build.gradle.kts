import java.util.Properties
import kotlin.io.path.div
import kotlin.io.path.inputStream

val baseVersion: String = libs.versions.kmbed.get()

allprojects {
    group = "net.folivo"
    version = withVersionSuffix(baseVersion)

    repositories {
        mavenLocal()
        mavenCentral()
        google()
    }
}