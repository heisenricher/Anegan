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
include(":core:ai-engine")
include(":feature:dashboard")
include(":feature:conversion-flow")
include(":feature:history")
include(":feature:ai-assistant")
