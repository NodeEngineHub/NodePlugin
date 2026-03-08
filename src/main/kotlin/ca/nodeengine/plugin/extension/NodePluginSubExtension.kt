package ca.nodeengine.plugin.extension

import org.gradle.api.Project
import org.gradle.api.provider.Property
import kotlin.text.replace

/**
 * This is an extension for subprojects, where a [NodePluginExtension] is already provided in the root project.<br>
 * It allows you to override settings and usages per subproject.
 *
 * @author FX
 */
abstract class NodePluginSubExtension {

    /**
     * The artifact id of the root parent.
     */
    abstract val rootArtifactId: Property<String>

    /**
     * The default artifact ID format provided by the root project.<br>
     * Use `|root|` for the root name, and `|target|` for the target name.<br>
     * By default, it will be `|root|-|target|`<br>
     * If the artifact id is set, it will override this default.
     */
    abstract val defaultArtifactId: Property<String>

    /**
     * Determines if this project should include a Sources Jar<br>
     * Default: `true`
     */
    abstract val includeSources: Property<Boolean>

    /**
     * Determines if this project should include a Javadoc Jar
     */
    abstract val includeJavadoc: Property<Boolean>

    /**
     * If lombok should be included as a dependency
     */
    abstract val includeLombok: Property<Boolean>

    /**
     * If lombok should be included as a dependency
     */
    abstract val includeAssets: Property<Boolean>

    /**
     * If this project should use proguard obfuscation.
     */
    abstract val useProguard: Property<Boolean>

    /**
     * If this project should be published.<br>
     * Adds publishing tasks with JRelease
     */
    abstract val shouldPublish: Property<Boolean>

    /**
     * The artifact ID for this project.<br>
     * Default: `project.name`
     */
    abstract val artifactId: Property<String>

    fun getArtifactId(target: Project): String {
        if (artifactId.isPresent) {
            return artifactId.get()
        }
        var defaultArtifactId = defaultArtifactId.get()
        defaultArtifactId = defaultArtifactId.replace("|root|", rootArtifactId.get())
        defaultArtifactId = defaultArtifactId.replace("|target|", target.name)
        return defaultArtifactId
    }
}