package ca.nodeengine.plugin.tasks

import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

/**
 * This task publishes and deploys all the modules using JReleaser
 *
 * @author FX
 */
abstract class DeployModulesTask : Exec() {

    @Input
    val dryRun = false

    init {
        group = "publishing"
        description = "Publish and deploys all enabled API/Core modules using JReleaser"
    }

    @TaskAction
    fun action() {
        project.subprojects.forEach { sp ->
            sp.tasks.findByName("publishToMavenLocal")?.let {
                dependsOn(it)
            }
        }
        workingDir(project.rootDir)
        if (System.getProperty("os.name").lowercase().contains("windows")) {
            args = mutableListOf("cmd", "/c", "gradlew.bat")
        } else {
            args = mutableListOf("./gradlew")
        }
        args.add("jreleaserDeploy")
        if (dryRun) {
            args.add("--dryrun")
        }
        commandLine(args)
    }
}