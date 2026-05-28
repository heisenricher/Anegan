pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Anegan"
include(":app")
include(":core:designsystem")
include(":core:database")
include(":core:conversion-engine")
include(":feature:dashboard")
include(":feature:conversion-flow")
include(":feature:history")
include(":feature:notes")
include(":feature:vault")
include(":feature:file-manager")
include(":feature:document-reader")
include(":feature:wifi-transfer")
include(":feature:apk-tools")
include(":feature:saver")
include(":feature:smb-share")

