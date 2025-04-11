pluginManagement {
    repositories {
        maven("https://maven.wagyourtail.xyz/releases")
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "manifold-gradle"
