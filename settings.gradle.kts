@file:Suppress("UnstableApiUsage")

include(":plugin-system")


enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
  includeBuild("build-logic")
  repositories {
    gradlePluginPortal()
    google()
    mavenCentral()
  }
}

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
    maven { url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/") }
    maven { url = uri("https://jitpack.io") }
  }
}

rootProject.name = "VCSpace"

include(
    ":app",
    ":common",
    ":common-res",
    ":editor",
    ":editor-textmate",
    ":eventbus-events",
    ":emulatorview",
    ":models",
    ":subprojects:tm4e")
