plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `version-catalog`
    id("io.freefair.lombok") version "8.4"
    id("com.vanniktech.maven.publish") version "0.35.0"
}

group = "ca.nodeengine.nodeplugin"
version = "1.0.0"

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

mavenPublishing {
    coordinates("ca.nodeengine", "NodePlugin", "1.0.0")

    pom {
        name.set("NodePlugin")
        description.set("A Gradle plugin for NodeEngine projects")
        inceptionYear.set("2026")
        url.set("https://github.com/NodeEngineHub/NodePlugin")
        licenses {
            license {
                name.set("GNU Lesser General Public License v3.0")
                url.set("https://github.com/NodeEngineHub/NodePlugin/blob/master/LICENSE")
            }
        }
        developers {
            developer {
                id.set("fxmorin")
                name.set("FX Morin")
                url.set("https://github.com/fxmorin/")
            }
        }
        scm {
            url.set("https://github.com/NodeEngineHub/NodePlugin/")
            connection.set("scm:git:git://github.com/NodeEngineHub/NodePlugin.git")
            developerConnection.set("scm:git:ssh://git@github.com/NodeEngineHub/NodePlugin.git")
        }
    }
    publishToMavenCentral()
    signAllPublications()
}