import java.time.LocalDate

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `version-catalog`
    id("io.freefair.lombok") version "8.4"
    `maven-publish`
    id("org.jreleaser") version "1.16.0"
}

group = "ca.nodeengine"
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

publishing {
    publications {
        register<MavenPublication>("maven") {
            from(components["java"])
            groupId = "${project.group}"
            artifactId = "${rootProject.name}"
            version ="${project.version}"

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
        }
    }
    repositories {
        maven {
            url = layout.buildDirectory.dir("staging-deploy").get().asFile.toURI()
        }
    }
}

jreleaser {
    project {
        description = "A Gradle plugin for NodeEngine projects"
        authors = listOf("Fx Morin")
        license = "LGPL 3.0"
        links {
            homepage = "https://github.com/NodeEngineHub/NodePlugin/"
            bugTracker = "https://github.com/NodeEngineHub/NodePlugin/issues"
            contact = "https://nodeengine.ca/"
        }
        inceptionYear = "2026"
        vendor = "FXCO Ltd."
        copyright = "Copyright (c) ${LocalDate.now().year} FXCO Ltd."
    }
    signing {
        active.set(org.jreleaser.model.Active.ALWAYS)
        armored.set(true)
    }
    deploy {
        maven {
            mavenCentral {
                register("sonatype") {
                    active.set(org.jreleaser.model.Active.ALWAYS)
                    url.set("https://central.sonatype.com/api/v1/publisher")
                    stagingRepository("build/staging-deploy")
                }
            }
        }
    }
}