package ca.nodeengine.plugin.tasks

import ca.nodeengine.plugin.NodePluginExtension
import ca.nodeengine.plugin.NodePluginSubExtension
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.findByType

/**
 * Task that lists all projects and their [NodePluginExtension] or [NodePluginSubExtension] settings.
 *
 * @author FX
 */
abstract class ListExtensionsTask : DefaultTask() {

    init {
        group = "help"
        description = "Lists all projects and their NodePlugin extension settings."
    }

    @TaskAction
    fun action() {
        val rootProject = project.rootProject
        println("\n--- NodePlugin Extension Settings for ${project.rootProject.displayName} ---")

        rootProject.allprojects.forEach { p ->
            println("\nProject: ${p.path}")
            
            // Try to find the root extension (NodePluginExtension)
            val rootExt = p.extensions.findByType<NodePluginExtension>()
            if (rootExt != null) {
                println("  [Root Extension Settings]")
                println("    annotatedPackages: ${rootExt.annotatedPackages.orNull}")
                println("    javaVersion: ${rootExt.javaVersion.orNull}")
                println("    apiProjectSuffix: ${rootExt.apiProjectSuffix.orNull}")
                println("    useProguard: ${rootExt.useProguard.orNull}")
                println("    excludedIncludedBuilds: ${rootExt.excludedIncludedBuilds.orNull}")
                println("    publishApi: ${rootExt.publishApi.orNull}")
                println("    publishAll: ${rootExt.publishAll.orNull}")
            }

            // Try to find the sub extension (NodePluginSubExtension)
            val subExt = p.extensions.findByType<NodePluginSubExtension>()
            if (subExt != null) {
                println("  [Sub Extension Settings]")
                println("    artifactId: ${subExt.artifactId.orNull}")
                println("    includeSources: ${subExt.includeSources.orNull}")
                println("    includeJavadoc: ${subExt.includeJavadoc.orNull}")
                println("    includeLombok: ${subExt.includeLombok.orNull}")
                println("    includeAssets: ${subExt.includeAssets.orNull}")
                println("    useProguard: ${subExt.useProguard.orNull}")
                println("    shouldPublish: ${subExt.shouldPublish.orNull}")
            }

            if (rootExt == null && subExt == null) {
                println("  No NodePlugin extension found.")
            }
        }
    }
}
