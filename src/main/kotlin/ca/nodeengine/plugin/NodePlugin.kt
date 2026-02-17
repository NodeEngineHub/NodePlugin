package ca.nodeengine.plugin

import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.*
import org.gradle.kotlin.dsl.get
import org.gradle.plugins.ide.idea.model.IdeaModel
import java.util.*
import proguard.gradle.ProGuardTask
import java.io.File
import org.gradle.api.provider.Property
import org.gradle.api.provider.ListProperty

/**
 * Configuration extension for [NodePlugin].
 *
 * @author FX
 */
abstract class NodePluginExtension {
    /**
     * The packages to be annotated for NullAway.
     * Default: "ca.nodeengine"
     */
    abstract val annotatedPackages: Property<String>

    /**
     * The Java version to use for toolchain and compatibility.
     * Default: 25
     */
    abstract val javaVersion: Property<Int>

    /**
     * The suffix used to identify API projects.
     * Default: "api"
     */
    abstract val apiProjectSuffix: Property<String>

    /**
     * The suffix used to identify Core projects.
     * Default: "core"
     */
    abstract val coreProjectSuffix: Property<String>

    /**
     * Whether this project should use proguard obfuscation.
     * Default: true, except api projects.
     */
    abstract val useProguard: Property<Boolean>

    /**
     * List of plugin IDs to exclude from copying Gradle files in copyGradleToIncludedBuilds.
     * Default: ["node-plugin"]
     */
    abstract val excludedIncludedBuilds: ListProperty<String>
}

/**
 * A gradle plugin which applies shared gradle logic to all the modules.<br>
 * TODO: We will need to throw this into a maven in order for the individual systems to work on their own.
 *
 * @author FX
 */
class NodePlugin : Plugin<Project> {

    override fun apply(target: Project) {
        val extension = target.extensions.create<NodePluginExtension>("nodePlugin").apply {
            annotatedPackages.convention("ca.nodeengine")
            javaVersion.convention(25)
            apiProjectSuffix.convention("api")
            coreProjectSuffix.convention("core")
            excludedIncludedBuilds.convention(listOf("NodePlugin"))
            useProguard.convention(true)
        }

        // Repositories available at root for plugin resolution & common deps
        target.repositories.applyDefaultRepos()

        // Apply to all subprojects (not the root)
        target.subprojects.forEach { project ->
            // Core plugins and repos
            project.pluginManager.apply("idea")
            project.pluginManager.apply("java-library")
            project.pluginManager.apply("maven-publish")
            project.pluginManager.apply("io.freefair.lombok")
            project.pluginManager.apply("net.ltgt.errorprone")

            project.repositories.applyDefaultRepos()

            // IDEA configuration
            val idea = project.extensions.getByType<IdeaModel>()
            idea.module.apply {
                outputDir = project.file("build/classes/java/main")
                testOutputDir = project.file("build/classes/java/test")
            }

            // Java toolchain and compatibility
            val javaExt = project.extensions.getByType<JavaPluginExtension>()

            val jv = extension.javaVersion.get()
            javaExt.sourceCompatibility = JavaVersion.toVersion(jv)
            javaExt.targetCompatibility = JavaVersion.toVersion(jv)
            javaExt.toolchain.languageVersion.set(JavaLanguageVersion.of(jv))

            // Sources and Javadoc jars + resources for core
            val apiSuffix = extension.apiProjectSuffix.get()
            val coreSuffix = extension.coreProjectSuffix.get()
            val isApi = project.name.contains(apiSuffix)
            val isCore = project.name.contains(coreSuffix)

            val dependencies = project.dependencies
            // Dependencies and versions
            if (!isApi && target.providers.gradleProperty("lombokVersion").isPresent) {
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
                    disableWarningsInGeneratedCode.set(true)
                    excludedPaths.set(".*/build/generated/.*")
                    check("NullAway", CheckSeverity.ERROR)
                    option("NullAway:AnnotatedPackages", extension.annotatedPackages.get())
                    option("NullAway:ExhaustiveOverride", true)
                    option("NullAway:CheckOptionalEmptiness", true)
                    option("NullAway:CheckContracts", true)
                    disable("UnnecessaryParentheses")
                }
                val n = name.lowercase(Locale.ROOT)
                if (n.contains("test")) {
                    options.errorprone { disable("NullAway") }
                }
            }

            // Jar base name
            project.tasks.withType<Jar>().configureEach {
                archiveBaseName.set("${target.name}-${project.name}")
                duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            }

            // Tests use JUnit Platform
            project.tasks.withType<Test>().configureEach {
                useJUnitPlatform()
                this.testLogging.events("passed", "skipped", "failed")
            }

            javaExt.withSourcesJar()
            if (isApi) {
                javaExt.withJavadocJar()
            }

            val sourceSets = project.extensions.getByType<SourceSetContainer>()
            if (isApi) {
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
                    setSource(project.fileTree(preprocess.map { it.destinationDir }))
                    classpath = sourceSets.getByName("main").compileClasspath
                }
            } else if (isCore) {
                sourceSets.getByName("main").resources.srcDir("assets")
            }

            project.tasks.withType<Copy>().configureEach {
                duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            }

            project.tasks.withType<AbstractArchiveTask>().configureEach {
                duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            }

            // ProGuard obfuscation task (safe defaults)
            val apiSuffix_pg = extension.apiProjectSuffix.get()
            val isApiModule = project.path.endsWith(":$apiSuffix_pg")
            if (!isApi && !extension.useProguard.get()) {
                project.tasks.register<ProGuardTask>("proguardObfuscate") {
                    group = "build"
                    description = "Obfuscate the compiled JAR with ProGuard"

                    // Ensure classes are compiled and jar is built first
                    dependsOn(project.tasks.named("jar"))

                    // Also, ensure API module jars are built so we can add them as libraryjars
                    target.subprojects
                        .filter { it.path.endsWith(":$apiSuffix_pg") && it.path != project.path }
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
                            .filter { it.path.endsWith(":$apiSuffix_pg") && it.path != project.path }
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

            // Additional subproject repositories
            project.repositories.mavenCentral()
            project.repositories.mavenLocal()

            // Publishing configuration
            project.afterEvaluate {
                project.extensions.configure<PublishingExtension> {
                    publications {
                        if (findByName("mavenJava") == null) {
                            create<MavenPublication>("mavenJava") {
                                from(project.components["java"])
                                groupId = project.group.toString()
                                artifactId = project.name
                                version = project.version.toString()
                            }
                        }
                    }
                }
            }
        }

        // Root helper tasks
        target.afterEvaluate {
            val excludedBuilds = extension.excludedIncludedBuilds.get()

            // Aggregate obfuscation task across all subprojects and included builds
            target.tasks.register("obfuscateAll") {
                target.subprojects.forEach { sp ->
                    sp.tasks.findByName("proguardObfuscate")?.let { dependsOn(it) }
                }
                target.gradle.includedBuilds.forEach { included ->
                    if (included.name !in excludedBuilds) {
                        dependsOn(included.task(":obfuscateAll"))
                    }
                }
            }

            target.tasks.register("publishAllToMavenLocal") {
                target.subprojects.forEach { sp -> sp.tasks.named("publishToMavenLocal").let { t -> dependsOn(t) } }
                target.gradle.includedBuilds.forEach { included ->
                    if (included.name !in excludedBuilds) {
                        dependsOn(included.task(":publishAllToMavenLocal"))
                    }
                }
            }

            // Task to copy the top-level Gradle directory to all included builds recursively
            target.tasks.register("copyGradleToIncludedBuilds") {
                // Perform copy to each directly included build
                doLast {
                    val sourceDir = target.rootProject.file("gradle")
                    if (sourceDir.exists()) {
                        target.gradle.includedBuilds.forEach { included ->
                            if (included.name !in excludedBuilds) {
                                val destDir = File(included.projectDir, "gradle")
                                target.copy {
                                    from(sourceDir)
                                    into(destDir)
                                }
                            }
                        }
                    } else {
                        target.logger.lifecycle("No top-level 'gradle' directory found at: ${sourceDir.absolutePath}")
                    }
                    val gradlewFile = target.rootProject.file("gradlew")
                    if (gradlewFile.exists()) {
                        target.gradle.includedBuilds.forEach { included ->
                            if (included.name !in excludedBuilds) {
                                target.copy {
                                    from(gradlewFile)
                                    into(included.projectDir)
                                }
                            }
                        }
                    } else {
                        target.logger.lifecycle("No top-level 'gradlew' file found at: ${gradlewFile.absolutePath}")
                    }
                    val gradlewBatFile = target.rootProject.file("gradlew.bat")
                    if (gradlewBatFile.exists()) {
                        target.gradle.includedBuilds.forEach { included ->
                            if (included.name !in excludedBuilds) {
                                target.copy {
                                    from(gradlewBatFile)
                                    into(included.projectDir)
                                }
                            }
                        }
                    } else {
                        target.logger.lifecycle("No top-level 'gradlew' file found at: ${gradlewBatFile.absolutePath}")
                    }
                }
                // Recurse into included builds so they copy into their own included builds
                target.gradle.includedBuilds.forEach { included ->
                    if (included.name !in excludedBuilds) {
                        dependsOn(included.task(":copyGradleToIncludedBuilds"))
                    }
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