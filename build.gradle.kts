plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `version-catalog`
    id("io.freefair.lombok") version "8.4"
}

group = "ca.nodeengine.build_logic"

repositories {
    mavenCentral()
    gradlePluginPortal()
    mavenLocal()
    google()
}

val nullawayVersion: String by project

dependencies {
    implementation(kotlin("stdlib"))
    implementation(gradleApi())

    implementation("org.jetbrains.kotlin.jvm:org.jetbrains.kotlin.jvm.gradle.plugin:1.9.22")

    implementation("io.freefair.lombok:io.freefair.lombok.gradle.plugin:9.0.0-rc2")
    implementation("net.ltgt.gradle:gradle-errorprone-plugin:4.3.0")
    implementation("com.uber.nullaway:nullaway-annotations:$nullawayVersion")
    implementation("com.guardsquare:proguard-gradle:7.8.1")
    api("org.jspecify:jspecify:1.0.0")
}

gradlePlugin {
    plugins {
        register("nodePlugin") {
            id = "NodePlugin"
            implementationClass = "ca.nodeengine.plugin.NodePlugin"
        }
    }
}