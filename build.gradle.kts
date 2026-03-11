import org.jreleaser.gradle.plugin.tasks.JReleaserDeployTask
import java.time.LocalDate

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `version-catalog`
    id("io.freefair.lombok") version "8.4"
    `maven-publish`
    id("org.jreleaser") version "1.23.0"
    id("com.gradle.plugin-publish") version "2.0.0"
}

group = "ca.nodeengine"
version = "1.1.2"

repositories {
    mavenCentral()
    gradlePluginPortal()
    mavenLocal()
    google()
}

val nullawayVersion: String by project

java {
    withJavadocJar()
    withSourcesJar()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(gradleApi())

    implementation("org.jetbrains.kotlin.jvm:org.jetbrains.kotlin.jvm.gradle.plugin:1.9.22")

    implementation("io.freefair.lombok:io.freefair.lombok.gradle.plugin:9.0.0-rc2")
    implementation("net.ltgt.gradle:gradle-errorprone-plugin:4.3.0")
    implementation("com.uber.nullaway:nullaway-annotations:$nullawayVersion")
    implementation("com.guardsquare:proguard-gradle:7.8.1")
    implementation("org.jreleaser:org.jreleaser.gradle.plugin:1.23.0")
    api("org.jspecify:jspecify:1.0.0")
}

gradlePlugin {
    website.set("https://github.com/NodeEngineHub/NodePlugin")
    vcsUrl.set("https://github.com/NodeEngineHub/NodePlugin")
    plugins {
        register("nodePlugin") {
            id = "ca.nodeengine.node-plugin"
            displayName = "NodePlugin"
            description = "A Gradle plugin for NodeEngine projects"
            implementationClass = "ca.nodeengine.plugin.NodePlugin"
            tags = listOf("nodeengine")
        }
    }
}

publishing {
    repositories {
        maven {
            url = layout.buildDirectory.dir("staging-deploy").get().asFile.toURI()
        }
    }
    publications {
        afterEvaluate {
            withType<MavenPublication>().configureEach {
                if (name == "pluginMaven") {
                    artifactId = "node-plugin"
                }
            }
        }
        withType<MavenPublication>().configureEach {
            pom {
                name = "NodePlugin"
                description = "A Gradle plugin for NodeEngine projects"
                inceptionYear = "2026"
                url = "https://github.com/NodeEngineHub/NodePlugin"
                organization {
                    name = "NodeEngine"
                    url = "https://NodeEngine.ca/"
                }
                licenses {
                    license {
                        name = "LGPL-3.0-only"
                        url = "https://github.com/NodeEngineHub/NodePlugin/blob/master/LICENSE"
                        distribution = "repo"
                    }
                }
                developers {
                    developer {
                        id = "fxmorin"
                        name = "FX Morin"
                        url = "https://github.com/FxMorin/"
                    }
                }
                scm {
                    url = "https://github.com/NodeEngineHub/NodePlugin/"
                    connection = "scm:git:git://github.com/NodeEngineHub/NodePlugin.git"
                    developerConnection = "scm:git:ssh://git@github.com/NodeEngineHub/NodePlugin.git"
                }
                issueManagement {
                    system = "GitHub"
                    url = "https://github.com/NodeEngineHub/NodePlugin/issues"
                }
            }
        }
    }
}

jreleaser {
    project {
        name = "NodePlugin"
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
        active = org.jreleaser.model.Active.ALWAYS
        armored = true
    }
    deploy {
        maven {
            mavenCentral {
                register("sonatype") {
                    active = org.jreleaser.model.Active.ALWAYS
                    url = "https://central.sonatype.com/api/v1/publisher"
                    stagingRepository("build/staging-deploy")
                    skipPublicationCheck = true
                }
            }
        }
    }
    release {
        github {
            name = rootProject.name
        }
    }
    if (!providers.environmentVariable("CI").isPresent) {
        environment {
            variables = file(System.getProperty("user.home") + "/.jreleaser/config.toml")
        }
    }
}

val createJReleaserOutputDir by tasks.registering {
    val outputDir = layout.buildDirectory.dir("jreleaser")
    outputs.dir(outputDir)
    doLast {
        val file = outputDir.get().asFile
        if (!file.exists()) {
            file.mkdirs()
        }
    }
}

tasks.withType<JReleaserDeployTask>().configureEach {
    dependsOn(createJReleaserOutputDir)
}