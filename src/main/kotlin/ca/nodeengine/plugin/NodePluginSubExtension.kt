package ca.nodeengine.plugin

import org.gradle.api.provider.Property

/**
 * This is an extension for subprojects, where a [NodePluginExtension] is already provided in the root project.<br>
 * It allows you to override settings and usages per subproject.
 *
 * @author FX
 */
abstract class NodePluginSubExtension {

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
}