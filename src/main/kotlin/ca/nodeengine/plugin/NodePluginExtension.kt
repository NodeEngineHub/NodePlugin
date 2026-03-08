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
     * The root artifact ID for this project.<br>
     * By default, it will be the root project name.
     */
    abstract val rootArtifactId: Property<String>

    /**
     * The default artifact ID format for this project.<br>
     * Use `|root|` for the root name, and `|target|` for the target name.<br>
     * By default, it will be `|root|-|target|`<br>
     * If the sub extension sets an artifact id, it will override this default.
     */
    abstract val defaultArtifactId: Property<String>

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
     * If this project should use proguard obfuscation.<br>
     * Default: `true`, except api projects.
     */
    abstract val useProguard: Property<Boolean>

    /**
     * List of plugin IDs to exclude from copying Gradle files in copyGradleToIncludedBuilds.<br>
     * Default: `["NodePlugin"]`
     */
    abstract val excludedIncludedBuilds: ListProperty<String>

    /**
     * If this project should publish its API modules.<br>
     * Default: `true`
     */
    abstract val publishApi: Property<Boolean>

    /**
     * If this project should publish its None-API modules.<br>
     * Default: `false`
     */
    abstract val publishAll: Property<Boolean>

    /**
     * If deployModules should use dryRun.<br>
     * Default: `false`
     */
    abstract val dryRun: Property<Boolean>
}