pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
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

rootProject.name = "AutoTrans"

include(":app")
include(":domain")
include(":data")
include(":feature:capture")
include(":feature:ocr")
include(":feature:translator")
include(":feature:overlay")
include(":feature:settings")
include(":core:common")
include(":core:ui")
include(":core:testing")
