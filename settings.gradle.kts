// settings.gradle.kts

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
        // সমাধান: Kotlin DSL-এর জন্য আধুনিক এবং স্পষ্ট `url = uri("...")` সিনট্যাক্স ব্যবহার করা হয়েছে।
        maven {
            url = uri("https://jitpack.io")
        }
        maven {
            url = uri("https://repo.clojars.org")
        }
    }
}

rootProject.name = "BongoTube"
include(":app")