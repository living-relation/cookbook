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

rootProject.name = "MarkdownNotes"
include(":app")
include(":core-markdown")
include(":core-files")
include(":feature-reader")
include(":feature-editor")
include(":feature-table")
