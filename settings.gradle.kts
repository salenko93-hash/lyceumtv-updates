pluginManagement {
    repositories {
        google()
        maven {
            name = "JetBrainsMavenCentralCache"
            url = uri("https://cache-redirector.jetbrains.com/maven-central")
        }
    }

    resolutionStrategy {
        eachPlugin {
            when (requested.id.id) {
                "com.android.application" ->
                    useModule("com.android.tools.build:gradle:${requested.version}")
                "org.jetbrains.kotlin.android" ->
                    useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:${requested.version}")
                "org.jetbrains.kotlin.plugin.compose" ->
                    useModule("org.jetbrains.kotlin:compose-compiler-gradle-plugin:${requested.version}")
            }
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        maven {
            name = "JetBrainsMavenCentralCache"
            url = uri("https://cache-redirector.jetbrains.com/maven-central")
        }
    }
}

rootProject.name = "LyceumMobileAndroid"
include(":app")
