package ca.nodeengine.plugin

import ca.nodeengine.plugin.extension.NodePluginExtension
import ca.nodeengine.plugin.extension.NodePluginSubExtension
import ca.nodeengine.plugin.tasks.*
import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.ivy.IvyPublication
import org.gradle.api.publish.ivy.internal.publication.IvyModuleDescriptorSpecInternal
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.internal.publication.MavenPomInternal
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.*
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jreleaser.gradle.plugin.JReleaserExtension
import org.jreleaser.gradle.plugin.tasks.JReleaserDeployTask
import proguard.gradle.ProGuardTask
import java.io.File
import java.util.*
import kotlin.collections.forEach

/**
 * A gradle plugin which applies shared gradle logic to all the modules.
 *
 * @author FX
 */
class NodePlugin : Plugin<Project> {

    private fun createExtension(target: Project): NodePluginExtension {
        return target.extensions.create<NodePluginExtension>("nodePlugin").apply {
            rootArtifactId.convention(target.rootProject.name)
            defaultArtifactId.convention("|target|")
            annotatedPackages.convention("ca.nodeengine")
            javaVersion.convention(25)
            apiProjectSuffix.convention("api")
            excludedIncludedBuilds.convention(listOf("NodePlugin"))
            useProguard.convention(true)
            publishApi.convention(true)
            publishAll.convention(false)
            dryRun.convention(false)
        }
    }

    private fun createSubExtension(target: Project, rootExtension: NodePluginExtension): NodePluginSubExtension {
        val apiSuffix = rootExtension.apiProjectSuffix.get()
        val isApi = target.name.endsWith(apiSuffix)
        return target.extensions.create<NodePluginSubExtension>("subNodePlugin").apply {
            rootArtifactId.convention(rootExtension.rootArtifactId)
            defaultArtifactId.convention(rootExtension.defaultArtifactId)
            includeSources.convention(true)
            includeJavadoc.convention(isApi)
            includeLombok.convention(!isApi)
            includeAssets.convention(!isApi)
            useProguard.convention(rootExtension.useProguard.map { it && !isApi })
            shouldPublish.convention(if (isApi) rootExtension.publishApi else rootExtension.publishAll)
        }
    }

    override fun apply(target: Project) {
        val rootExtension = createExtension(target)

        // Repositories available at root for plugin resolution & common deps
        target.repositories.applyDefaultRepos()

        // Apply to all subprojects (not the root)
        target.subprojects.forEach { project ->
            val extension = createSubExtension(project, rootExtension)
            // Core plugins and repos
            project.pluginManager.apply("idea")
            project.pluginManager.apply("java-library")
            project.pluginManager.apply("net.ltgt.errorprone")

            project.afterEvaluate {

                if (extension.includeLombok.get()) {
                    project.pluginManager.apply("io.freefair.lombok")
                }

                project.repositories.applyDefaultRepos()

                // IDEA configuration
                val idea = project.extensions.getByType<IdeaModel>()
                idea.module.apply {
                    outputDir = project.file("build/classes/java/main")
                    testOutputDir = project.file("build/classes/java/test")
                }

                // Java toolchain and compatibility
                val javaExt = project.extensions.getByType<JavaPluginExtension>()

                val jv = rootExtension.javaVersion.get()
                javaExt.sourceCompatibility = JavaVersion.toVersion(jv)
                javaExt.targetCompatibility = JavaVersion.toVersion(jv)
                javaExt.toolchain.languageVersion.set(JavaLanguageVersion.of(jv))

                val dependencies = project.dependencies
                // Dependencies and versions
                if (extension.includeLombok.get() && target.providers.gradleProperty("lombokVersion").isPresent) {
                    val lombokVersion: String = target.providers.gradleProperty("lombokVersion").get()
                    dependencies.add("compileOnly", "org.projectlombok:lombok:$lombokVersion")
                    dependencies.add("annotationProcessor", "org.projectlombok:lombok:$lombokVersion")
                }

                val errorproneVersion: String = target.providers.gradleProperty("errorproneVersion").get()
                val nullawayVersion: String = target.providers.gradleProperty("nullawayVersion").get()
                dependencies.add("errorprone", "com.google.errorprone:error_prone_core:$errorproneVersion")
                dependencies.add("errorprone", "com.uber.nullaway:nullaway:$nullawayVersion")
                dependencies.add("implementation", "com.google.errorprone:error_prone_annotations:$errorproneVersion")
                dependencies.add("implementation", "com.uber.nullaway:nullaway-annotations:$nullawayVersion")
                dependencies.add("api", "org.jspecify:jspecify:1.0.0")

                dependencies.add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher:1.13.4")

                // JavaCompile tasks configuration incl. ErrorProne
                project.tasks.withType<JavaCompile>().configureEach {
                    options.isIncremental = true
                    options.encoding = "UTF-8"
                    options.errorprone {
                        disableWarningsInGeneratedCode = true
                        excludedPaths = ".*/build/generated/.*"
                        check("NullAway", CheckSeverity.ERROR)
                        option("NullAway:AnnotatedPackages", rootExtension.annotatedPackages.get())
                        option("NullAway:ExhaustiveOverride", true)
                        option("NullAway:CheckOptionalEmptiness", true)
                        option("NullAway:CheckContracts", true)
                        disable("UnnecessaryParentheses")
                    }
                    if (name.lowercase(Locale.ROOT).contains("test")) {
                        options.errorprone {
                            disable("NullAway")
                        }
                    }
                }

                // Jar base name
                project.tasks.withType<Jar>().configureEach {
                    archiveBaseName = extension.getArtifactId(project)
                    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                }

                // Tests use JUnit Platform
                project.tasks.withType<Test>().configureEach {
                    useJUnitPlatform()
                    this.testLogging.events("passed", "skipped", "failed")
                }

                if (extension.includeSources.get()) {
                    javaExt.withSourcesJar()
                }
                val sourceSets = project.extensions.getByType<SourceSetContainer>()
                if (extension.includeJavadoc.get()) {
                    javaExt.withJavadocJar()
                    val preprocess = project.tasks.register<Copy>("preprocessJavadocSources") {
                        from(sourceSets.getByName("main").allJava)
                        into(project.layout.buildDirectory.dir("javadoc-preprocessed"))
                        filter { line: String ->
                            val t = line.trim()
                            if (t.startsWith("*") || t.startsWith("/**") || t.startsWith("///")) {
                                line.replace(Regex("""&(?![a-zA-Z#0-9]+;)"""), "&amp;")
                            } else line
                        }
                    }
                    project.tasks.named<Javadoc>("javadoc").configure {
                        dependsOn(preprocess)
                        source = project.fileTree(preprocess.map { it.destinationDir })
                        classpath = sourceSets.getByName("main").compileClasspath
                    }
                }
                if (extension.includeAssets.get()) {
                    sourceSets.getByName("main").resources.srcDir("assets")
                }

                project.tasks.withType<Copy>().configureEach {
                    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                }

                project.tasks.withType<AbstractArchiveTask>().configureEach {
                    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                }

                // ProGuard obfuscation task (safe defaults)
                if (extension.useProguard.get() && project.tasks.findByName("proguardObfuscate") == null) {
                    val apiSuffix = rootExtension.apiProjectSuffix.get()
                    project.tasks.register<ProGuardTask>("proguardObfuscate") {
                        group = "build"
                        description = "Obfuscate the compiled JAR with ProGuard"

                        // Ensure classes are compiled and jar is built first
                        dependsOn(project.tasks.named("jar"))

                        // Also, ensure API module jars are built so we can add them as libraryjars
                        target.subprojects
                            .filter { it.name.endsWith(apiSuffix) && it.path != project.path }
                            .forEach { apiProj ->
                                dependsOn(apiProj.tasks.named("jar"))
                            }

                        // Configure at execution time to resolve providers lazily
                        doFirst {
                            val jarTask = project.tasks.named<Jar>("jar").get()
                            val inJar = jarTask.archiveFile.get().asFile
                            val outJar = project.layout.buildDirectory
                                .file("libs/${jarTask.archiveBaseName.get()}-${project.version}-obf.jar").get().asFile
                            val mappingFile = project.layout.buildDirectory
                                .file("outputs/proguard/mapping.txt").get().asFile
                            mappingFile.parentFile.mkdirs()

                            // In/out jars
                            injars(inJar)
                            outjars(outJar)
                            printmapping(mappingFile)

                            // Load configuration from file(s) if present; fall back to safe defaults
                            val candidateNames = listOf(
                                "proguard-rules.pro", "proguard.pro", "proguard.conf", "proguard.cfg"
                            )
                            val searchDirs = listOf(project.projectDir, project.rootProject.projectDir)
                            val configFiles = mutableListOf<File>()
                            for (dir in searchDirs) {
                                for (name in candidateNames) {
                                    val f = File(dir, name)
                                    if (f.exists()) {
                                        configFiles.add(f)
                                    }
                                }
                            }

                            if (configFiles.isNotEmpty()) {
                                configFiles.distinct().forEach { cfg ->
                                    // Include an external config file directly
                                    configuration(cfg)
                                }
                            } else {
                                throw IllegalStateException("Missing ProGuard configuration!")
                            }

                            // Only obfuscate by default to avoid hierarchy issues and empty outputs
                            // - Many modules don't have explicit entry points; shrinking can remove everything
                            // - Optimization can require full library hierarchies (e.g., optional Log4J2 in Netty)
                            dontshrink()
                            dontoptimize()

                            // Add JDK as library jars (Java 9+ via jmods, else rt.jar)
                            val javaHome = System.getProperty("java.home")
                            val jmodsDir = File(javaHome, "jmods")
                            if (jmodsDir.exists()) {
                                jmodsDir.listFiles { f -> f.isFile && f.name.endsWith(".jmod") }?.forEach { jmod ->
                                    libraryjars(jmod)
                                }
                            } else {
                                val rt = File(javaHome, "lib/rt.jar")
                                if (rt.exists()) {
                                    libraryjars(rt)
                                }
                            }

                            // Add project runtime classpath as library jars
                            val runtime = project.configurations.getByName("runtimeClasspath").resolve()
                            val libSet = mutableSetOf<String>()
                            runtime.forEach { dep ->
                                if (dep.exists() && libSet.add(dep.canonicalPath)) {
                                    libraryjars(dep)
                                }
                            }

                            // Additionally, include all API module jars from this build as libraryjars
                            target.subprojects
                                .filter { it.name.endsWith(apiSuffix) && it.path != project.path }
                                .forEach { apiProj ->
                                    val apiJarTask = apiProj.tasks.named<Jar>("jar").get()
                                    val apiJar = apiJarTask.archiveFile.get().asFile
                                    if (apiJar.exists() && libSet.add(apiJar.canonicalPath)) {
                                        libraryjars(apiJar)
                                    } else if (libSet.add(apiJar.absolutePath)) {
                                        // Add even if not yet existing;
                                        // The task dependency ensures it will exist by execution
                                        libraryjars(apiJar)
                                    }
                                }
                        }
                    }
                }

                // Publishing configuration
                if (extension.shouldPublish.get()) {
                    project.pluginManager.apply("maven-publish")
                    project.pluginManager.apply("org.jreleaser")
                    project.extensions.configure<PublishingExtension> {
                        repositories {
                            maven {
                                url = project.layout.buildDirectory.dir("staging-deploy").get().asFile.toURI()
                            }
                        }
                        publications {
                            afterEvaluate {
                                withType<MavenPublication>().configureEach {
                                    artifactId = extension.getArtifactId(project)
                                }
                            }
                        }
                    }

                    project.extensions.configure<JReleaserExtension> {
                        project {
                            setJReleaserDefaultsFromPublishing(project, this)
                        }
                        release {
                            github {
                                enabled = true
                            }
                        }
                        if (!project.providers.environmentVariable("CI").isPresent) {
                            environment {
                                variables = File(System.getProperty("user.home") + "/.jreleaser/config.toml")
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
                }
            }
        }

        // Root helper tasks
        target.afterEvaluate {
            val excludedIncludedBuilds = rootExtension.excludedIncludedBuilds.get()

            target.tasks.register<DependsOnAllTask>("obfuscateAll") {
                group = "other"
                description = "Run proguard obfuscation in all projects"
                taskName = "proguardObfuscate"
                excludedBuilds = excludedIncludedBuilds
            }

            target.tasks.register<DependsOnAllTask>("publishAllToMavenLocal") {
                group = "other"
                description = "Run publishToMavenLocal in all projects"
                taskName = "publishToMavenLocal"
                excludedBuilds = excludedIncludedBuilds
            }

            // Task to copy the top-level Gradle directory to all included builds recursively
            target.tasks.register<CopyGradleToIncludedBuildsTask>("copyGradleToIncludedBuilds") {
                excludedBuilds = excludedIncludedBuilds
            }

            // Task to publish & deploy all modules with JReleaser
            target.tasks.register<DeployModulesTask>("deployModules") {
                dryRun = rootExtension.dryRun.get()
            }

            target.subprojects.forEach { subproject ->
                subproject.tasks.register<ListExtensionsTask>("listNodePluginSettings")
            }

            target.tasks.register<ListExtensionsTask>("listNodePluginSettings")
        }
    }
}

private fun setJReleaserDefaultsFromPublishing(project: Project,
                                               jreleaserProject: org.jreleaser.gradle.plugin.dsl.project.Project) {
    project.extensions.getByType(PublishingExtension::class).publications.forEach { pub ->
        if (pub is MavenPublication) {
            if (!jreleaserProject.name.isPresent) {
                jreleaserProject.name.convention(pub.pom.name)
            }
            if (!jreleaserProject.description.isPresent) {
                jreleaserProject.description.convention(pub.pom.description)
            }
            val pom = pub.pom
            if (pom is MavenPomInternal) {
                val license = pom.licenses.first()
                if (!jreleaserProject.license.isPresent) {
                    jreleaserProject.license.convention(license.name)
                }
                if (!jreleaserProject.links.license.isPresent) {
                    jreleaserProject.links.license.convention(license.url)
                }
                if (!jreleaserProject.authors.isPresent) {
                    jreleaserProject.authors.convention(project.provider { pom.developers
                        .map { developer -> developer.name.get() } })
                }
                if (!jreleaserProject.links.vcsBrowser.isPresent) {
                    pom.scm?.let {
                        jreleaserProject.links.vcsBrowser.convention(it.url)
                    }
                }
                if (!jreleaserProject.links.bugTracker.isPresent) {
                    pom.issueManagement?.let {
                        jreleaserProject.links.bugTracker.convention(it.url)
                    }
                }
                if (!jreleaserProject.links.homepage.isPresent) {
                    pom.organization?.let {
                        jreleaserProject.links.homepage.convention(it.url)
                    }
                }
            }
            if (!jreleaserProject.inceptionYear.isPresent) {
                jreleaserProject.inceptionYear.convention(pub.pom.inceptionYear)
            }
        } else if (pub is IvyPublication) {
            if (!jreleaserProject.name.isPresent) {
                jreleaserProject.name.convention(pub.name)
            }
            val descriptor = pub.descriptor
            if (descriptor is IvyModuleDescriptorSpecInternal) {
                if (!jreleaserProject.authors.isPresent) {
                    jreleaserProject.authors.convention(project.provider { descriptor.authors
                        .map { developer -> developer.name.get() } })
                }
                val license = descriptor.licenses.first()
                if (!jreleaserProject.license.isPresent) {
                    jreleaserProject.license.convention(license.name)
                }
                if (!jreleaserProject.links.license.isPresent) {
                    jreleaserProject.links.license.convention(license.url)
                }
            }
        }
    }
}

private fun RepositoryHandler.applyDefaultRepos() {
    mavenCentral()
    gradlePluginPortal()
    mavenLocal()
    google()
}