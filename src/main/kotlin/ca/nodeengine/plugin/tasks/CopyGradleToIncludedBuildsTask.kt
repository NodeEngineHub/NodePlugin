package ca.nodeengine.plugin.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Task to copy the top-level Gradle directory to all included builds recursively
 *
 * @author FX
 */
abstract class CopyGradleToIncludedBuildsTask : DefaultTask() {

    @Input
    var excludedBuilds: List<String> = emptyList()

    init {
        group = "other"
        description = "Copy the top-level Gradle directory to all included builds recursively"
        // Recurse into included builds so they copy into their own included builds
        project.gradle.includedBuilds.forEach { included ->
            if (included.name !in excludedBuilds) {
                dependsOn(included.task(":copyGradleToIncludedBuilds"))
            }
        }
    }

    @TaskAction
    fun action() {
        // Perform copy to each directly included build
        doLast {
            val sourceDir = project.rootProject.file("gradle")
            if (sourceDir.exists()) {
                project.gradle.includedBuilds.forEach { included ->
                    if (included.name !in excludedBuilds) {
                        val destDir = File(included.projectDir, "gradle")
                        project.copy {
                            from(sourceDir)
                            into(destDir)
                        }
                    }
                }
            } else {
                project.logger.lifecycle("No top-level 'gradle' directory found at: ${sourceDir.absolutePath}")
            }
            val gradlewFile = project.rootProject.file("gradlew")
            if (gradlewFile.exists()) {
                project.gradle.includedBuilds.forEach { included ->
                    if (included.name !in excludedBuilds) {
                        project.copy {
                            from(gradlewFile)
                            into(included.projectDir)
                        }
                    }
                }
            } else {
                project.logger.lifecycle("No top-level 'gradlew' file found at: ${gradlewFile.absolutePath}")
            }
            val gradlewBatFile = project.rootProject.file("gradlew.bat")
            if (gradlewBatFile.exists()) {
                project.gradle.includedBuilds.forEach { included ->
                    if (included.name !in excludedBuilds) {
                        project.copy {
                            from(gradlewBatFile)
                            into(included.projectDir)
                        }
                    }
                }
            } else {
                project.logger.lifecycle("No top-level 'gradlew' file found at: ${gradlewBatFile.absolutePath}")
            }
        }
    }
}