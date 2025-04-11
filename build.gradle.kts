plugins {
    kotlin("jvm") version "2.1.10"
    alias(libs.plugins.commons.gradle)
    `java-gradle-plugin`
}

group = "xyz.wagyourtail"
version = "1.0-SNAPSHOT"

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
