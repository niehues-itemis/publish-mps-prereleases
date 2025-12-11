pluginManagement {
    repositories {
        maven("https://artifacts.itemis.cloud/repository/gradle-plugins/") {
            val userName = providers.gradleProperty("artifacts.itemis.cloud.user").orNull
            if (userName != null) {
                credentials {
                    username = userName
                    password = providers.gradleProperty("artifacts.itemis.cloud.pw").orNull
                }
            }
        }
        gradlePluginPortal()
    }
}

rootProject.name = "publish-mps-prereleases"

include(":find-latest-version")
include(":repackage-and-publish")
