# NodePlugin

`NodePlugin` is a Gradle plugin that provides shared build logic, including toolchain configuration, NullAway (ErrorProne) setup, Javadoc preprocessing, and ProGuard obfuscation across all modules.

## Configuration

The plugin can be configured using the `nodePlugin` extension in your `build.gradle.kts` (or `build.gradle`) file. If you use all the default values, you can omit the `nodePlugin` block entirely.

### Example (Kotlin DSL)

```kotlin
plugins {
    id("NodePlugin")
}

nodePlugin {
    // Packages to be annotated for NullAway (Nullability checks)
    annotatedPackages.set("ca.nodeengine")

    // Java version for toolchain, source and target compatibility
    javaVersion.set(25)

    // Suffixes used to identify API and Core projects for specific logic
    apiProjectSuffix.set("api")

    // If proguard tasks should be used globally
    useProguard.set(true)

    // Included builds to exclude from root helper tasks (obfuscateAll, etc.)
    excludedIncludedBuilds.set(listOf("NodePlugin"))

    // The root artifact ID for this project. Default: root project name.
    rootArtifactId.set("quantum")

    // The default artifact ID format. |root| = root name, |target| = target name.
    defaultArtifactId.set("|target|")

    // If this project should publish its API modules. Default: true
    publishApi.set(true)

    // If this project should publish its Non-API modules. Default: false
    publishAll.set(false)

    // If deployModules should use dryRun. Default: false
    dryRun.set(false)

    // If the root project should be included as a node project. Default: false
    includeRootProject.set(false)
}
```

### Subproject Configuration (Kotlin DSL)

You can also configure individual subprojects using the `subNodePlugin` extension within a subproject's `build.gradle.kts`.

```kotlin
nodePlugin {
    // The artifact ID for this project. Default: derived from defaultArtifactId.
    artifactId.set("my-custom-artifact-id")

    // Determines if this project should include a Sources Jar. Default: true
    includeSources.set(true)

    // Determines if this project should include a Javadoc Jar. Default: true (for API projects)
    includeJavadoc.set(true)

    // If lombok should be included as a dependency. Default: true (for Non-API projects)
    includeLombok.set(true)

    // If assets folder should be included in resources. Default: true (for Non-API projects)
    includeAssets.set(true)

    // If this project should use proguard obfuscation. Default: true (for Non-API projects)
    useProguard.set(true)

    // If this project should be published. Default: matches publishApi/publishAll.
    shouldPublish.set(true)
}
```

### Example (Groovy DSL)

```groovy
plugins {
    id 'NodePlugin'
}

nodePlugin {
    // Packages to be annotated for NullAway (Nullability checks)
    annotatedPackages = 'ca.nodeengine'

    // Java version for toolchain, source and target compatibility
    javaVersion = 25

    // Suffixes used to identify API and Core projects for specific logic
    apiProjectSuffix = 'api'

    // If proguard tasks should be used globally
    useProguard = true

    // Included builds to exclude from root helper tasks (obfuscateAll, etc.)
    excludedIncludedBuilds = ['NodePlugin']

    // The root artifact ID for this project. Default: root project name.
    rootArtifactId = 'quantum'

    // The default artifact ID format. |root| = root name, |target| = target name.
    defaultArtifactId = '|target|'

    // If this project should publish its API modules. Default: true
    publishApi = true

    // If this project should publish its Non-API modules. Default: false
    publishAll = false

    // If deployModules should use dryRun. Default: false
    dryRun = false

    // If the root project should be included as a node project. Default: false
    includeRootProject = false
}
```

### Configuration Properties

#### Root Project Configuration

| Property | Type | Default Value | Description |
|:---| :--- |:---|:---|
| `annotatedPackages` | `Property<String>` | `"ca.nodeengine"` | The package prefix(es) that NullAway should analyze for nullability. |
| `javaVersion` | `Property<Int>` | `25` | The Java version used for the Gradle toolchain and Java compatibility levels. |
| `apiProjectSuffix` | `Property<String>` | `"api"` | Suffix used to identify "API" projects. API projects get Javadoc preprocessing and Javadoc JARs by default. |
| `useProguard` | `Property<Boolean>` | `true` | If `true`, ProGuard tasks are enabled globally. |
| `excludedIncludedBuilds` | `ListProperty<String>` | `["NodePlugin"]` | List of included build names to exclude when running root helper tasks. |
| `rootArtifactId` | `Property<String>` | `rootProject.name` | The root artifact ID for this project. |
| `defaultArtifactId` | `Property<String>` | `"|target|"` | The default artifact ID format. Use `|root|` for the root name, and `|target|` for the target name. |
| `publishApi` | `Property<Boolean>` | `true` | If `true`, API modules will be published by default. |
| `publishAll` | `Property<Boolean>` | `false` | If `true`, Non-API modules will be published by default. |
| `dryRun` | `Property<Boolean>` | `false` | If `true`, `deployModules` task will use dry run mode. |
| `includeRootProject` | `Property<Boolean>` | `false` | If `true`, the root project itself will be configured as a node project. |

#### Subproject Configuration

| Property | Type | Default Value | Description |
|:---| :--- |:---|:---|
| `artifactId` | `Property<String>` | (derived) | The artifact ID for this project. If not set, it's generated from `defaultArtifactId`. |
| `includeSources` | `Property<Boolean>` | `true` | Whether to include a Sources JAR. |
| `includeJavadoc` | `Property<Boolean>` | `isApi` | Whether to include a Javadoc JAR. |
| `includeLombok` | `Property<Boolean>` | `!isApi` | Whether to include Lombok as a dependency. |
| `includeAssets` | `Property<Boolean>` | `!isApi` | Whether to include the `assets` folder in resources. |
| `useProguard` | `Property<Boolean>` | `root.useProguard && !isApi` | Whether to use ProGuard for this project. |
| `shouldPublish` | `Property<Boolean>` | (derived) | Whether this project should be published. Defaults to `publishApi` for API projects and `publishAll` for others. |


## Tasks

The plugin registers several tasks, some at the root project level and others in subprojects.

### Root Project Tasks

- `obfuscateAll`: Aggregates and runs `proguardObfuscate` for all subprojects and included builds (unless excluded).
- `publishAllToMavenLocal`: Publishes all subprojects and included builds to the local Maven repository.
- `copyGradleToIncludedBuilds`: Copies the root `gradle/` wrapper and `gradlew` scripts to all included builds.
- `deployModules`: Publishes & deploys all modules using JReleaser. Respects the `dryRun` setting.
- `listNodePluginSettings`: Lists the current `NodePlugin` settings for the project.

### Subproject Tasks

- `proguardObfuscate`: (Non-API modules only) Obfuscates the project's JAR using ProGuard. Requires a `proguard-rules.pro` (or similar) file in the project or root directory.
- `preprocessJavadocSources`: (API modules only) Preprocesses Javadoc sources to fix common HTML issues (e.g., escaping `&` characters in Javadoc comments).
- `mavenJava`: A standard Maven publication is automatically configured for all subprojects.
- `listNodePluginSettings`: Lists the current `NodePlugin` settings for this subproject.
