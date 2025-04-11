plugins {
    kotlin("jvm") version "2.1.10"
    alias(libs.plugins.commons.gradle)
    `java-gradle-plugin`
    `maven-publish`
}

group = "xyz.wagyourtail"
version = if (project.hasProperty("version_snapshot")) project.properties["version"] as String + "-SNAPSHOT" else project.properties["version"] as String

kotlin {
    jvmToolchain(8)
}

repositories {
    maven("https://maven.wagyourtail.xyz/releases")
    mavenCentral()
}

dependencies {
    implementation(gradleApi())
    implementation(libs.commons.gradle)
    implementation(libs.commons.kt)

    testImplementation(kotlin("test"))
}

gradlePlugin {
    plugins {
        create("simplePlugin") {
            id = "xyz.wagyourtail.manifold"
            description = project.description
            implementationClass = "xyz.wagyourtail.manifold.GradlePlugin"
            version = project.version as String
        }
        create("simpleSettingsPlugin") {
            id = "xyz.wagyourtail.manifold-settings"
            description = project.description
            implementationClass = "xyz.wagyourtail.manifold.GradleSettings"
            version = project.version as String
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    repositories {
        maven {
            name = "WagYourMaven"
            url = if (project.hasProperty("version_snapshot")) {
                uri("https://maven.wagyourtail.xyz/snapshots/")
            } else {
                uri("https://maven.wagyourtail.xyz/releases/")
            }
            credentials {
                username = project.findProperty("mvn.user") as String? ?: System.getenv("USERNAME")
                password = project.findProperty("mvn.key") as String? ?: System.getenv("TOKEN")
            }
        }
    }
}
