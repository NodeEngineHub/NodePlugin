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
        val rootExt = project.extensions.findByType<NodePluginExtension>()
        val subExt = project.extensions.findByType<NodePluginSubExtension>()

        if (rootExt == null && subExt == null) return
        if (rootExt != null) {
            println("\nRoot NodePlugin Settings for ${project.displayName}")
        }

        // Root extension (NodePluginExtension)
        if (rootExt != null) {
            println("  [Root Extension Settings]")
            rootExt.rootArtifactId.orNull?.let { println("    rootArtifactId: $it") }
            rootExt.defaultArtifactId.orNull?.let { println("    defaultArtifactId: $it") }
            rootExt.annotatedPackages.orNull?.let { println("    annotatedPackages: $it") }
            rootExt.javaVersion.orNull?.let { println("    javaVersion: $it") }
            rootExt.apiProjectSuffix.orNull?.let { println("    apiProjectSuffix: $it") }
            rootExt.useProguard.orNull?.let { println("    useProguard: $it") }
            rootExt.excludedIncludedBuilds.orNull?.let { if (it.isNotEmpty()) println("    excludedIncludedBuilds: $it") }
            rootExt.publishApi.orNull?.let { println("    publishApi: $it") }
            rootExt.publishAll.orNull?.let { println("    publishAll: $it") }
            rootExt.dryRun.orNull?.let { println("    dryRun: $it") }
        }

        // Sub extension (NodePluginSubExtension)
        if (subExt != null) {
            println("  [Sub Extension Settings] (${project.displayName})")
            subExt.rootArtifactId.orNull?.let { println("    rootArtifactId: $it") }
            println("    artifactId: ${subExt.getArtifactId(project)}")
            subExt.includeSources.orNull?.let { println("    includeSources: $it") }
            subExt.includeJavadoc.orNull?.let { println("    includeJavadoc: $it") }
            subExt.includeLombok.orNull?.let { println("    includeLombok: $it") }
            subExt.includeAssets.orNull?.let { println("    includeAssets: $it") }
            subExt.useProguard.orNull?.let { println("    useProguard: $it") }
            subExt.shouldPublish.orNull?.let { println("    shouldPublish: $it") }
        }
    }
}
