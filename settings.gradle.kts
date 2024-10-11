pluginManagement {
  includeBuild("build-logic")
  repositories {
    gradlePluginPortal()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://plugins.gradle.org/m2/")
  }
}

plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "vectrix-item-components"
