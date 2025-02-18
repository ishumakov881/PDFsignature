pluginManagement {
    repositories {
        google {
//            content {
//                includeGroupByRegex("com\\.android.*")
//                includeGroupByRegex("com\\.google.*")
//                includeGroupByRegex("androidx.*")
//            }
        }
        mavenCentral()
        gradlePluginPortal()

        maven("https://jitpack.io")
        //noinspection JcenterRepositoryObsolete
        jcenter()
    }
}
dependencyResolutionManagement {
    //repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        //noinspection JcenterRepositoryObsolete
        jcenter()
    }
}

rootProject.name = "PDFsignature"
include(":app")
 