package ca.nodeengine.plugin.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

/**
 * This task acts as a DependsOn across all projects, subprojects, and included builds.<br>
 * Mainly used so we only need to run a single task for common functionality.<br>
 * Aggregate task across all subprojects and included builds
 *
 * @author FX
 */
abstract class DependsOnAllTask : DefaultTask() {

    @Input
    var excludedBuilds: List<String> = emptyList()

    @get:Input
    abstract var taskName: String // The task we will be calling

    @TaskAction
    fun action() {
        project.subprojects.forEach { sp ->
            sp.tasks.findByName(taskName)?.let {
                dependsOn(it)
            }
        }
        project.gradle.includedBuilds.forEach { included ->
            if (included.name !in excludedBuilds) {
                dependsOn(included.task(":${name}"))
            }
        }
    }
}