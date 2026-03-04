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

    @Input
    var onlyBuilds: Boolean = false

    @get:Input
    abstract var taskName: String // The task we will be calling

    init {
        // Use a lazy provider for dependencies to avoid "after task has started execution" error
        dependsOn(project.provider {
            val deps = mutableListOf<Any>()
            if (!onlyBuilds) {
                project.subprojects.forEach { sp ->
                    sp.tasks.findByName(taskName)?.let {
                        deps.add(it)
                    }
                }
            }
            project.gradle.includedBuilds.forEach { included ->
                if (included.name !in excludedBuilds) {
                    deps.add(included.task(":${name}"))
                }
            }
            deps
        })
    }
}