package ca.nodeengine.plugin.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input

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
        val allProjects = mutableListOf<String>()
        dependsOn(project.provider {
            val deps = mutableListOf<Any>()
            if (!onlyBuilds) {
                project.subprojects.forEach { sp ->
                    if (!allProjects.contains(sp.name)) {
                        sp.tasks.findByName(taskName)?.let {
                            deps.add(it)
                        }
                        allProjects.add(sp.name)
                    }
                }
            }
            project.gradle.includedBuilds.forEach { included ->
                // Check if the included build is a child of the current project based on its path
                val isChild = try {
                    val projectPath = project.projectDir.toPath().toAbsolutePath().normalize()
                    val includedPath = included.projectDir.toPath().toAbsolutePath().normalize()
                    includedPath.startsWith(projectPath) && includedPath != projectPath
                } catch (_: Exception) {
                    false
                }

                if (isChild && !allProjects.contains(included.name) && included.name !in excludedBuilds) {
                    deps.add(included.task(":${taskName}"))
                    allProjects.add(included.name)
                }
            }
            project.tasks.findByName(taskName)?.let {
                deps.add(it)
            }
            deps
        })
    }
}