package ca.nodeengine.plugin

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

/**
 * Configuration extension for [NodePlugin].
 *
 * @author FX
 */
abstract class NodePluginExtension {
    /**
     * The packages to be annotated for NullAway.<br>
     * Default: `"ca.nodeengine"`
     */
    abstract val annotatedPackages: Property<String>

    /**
     * The Java version to use for toolchain and compatibility.<br>
     * Default: `25`
     */
    abstract val javaVersion: Property<Int>

    /**
     * The suffix used to identify API projects.<br>
     * Default: `"api"`
     */
    abstract val apiProjectSuffix: Property<String>

    /**
     * The suffix used to identify Core projects.<br>
     * Default: `"core"`
     */
    abstract val coreProjectSuffix: Property<String>

    /**
     * Whether this project should use proguard obfuscation.<br>
     * Default: `true`, except api projects.
     */
    abstract val useProguard: Property<Boolean>

    /**
     * List of plugin IDs to exclude from copying Gradle files in copyGradleToIncludedBuilds.<br>
     * Default: `["NodePlugin"]`
     */
    abstract val excludedIncludedBuilds: ListProperty<String>

    /**
     * Whether this project should publish its API modules.<br>
     * Default: `false`
     */
    abstract val publishApi: Property<Boolean>

    /**
     * Whether this project should publish its Core modules.<br>
     * Default: `false`
     */
    abstract val publishCore: Property<Boolean>
}