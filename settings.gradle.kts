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
rootProject.name = "Seriee"
include(":app")

// Local build cache: task outputs are reused after `clean`, branch switches and
// daemon restarts. Stored in ~/.gradle/caches/build-cache-1 by default.
buildCache {
    local {
        isEnabled = true
    }
}
