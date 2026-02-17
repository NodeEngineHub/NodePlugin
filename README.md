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
    coreProjectSuffix.set("core")

    // If proguard tasks should be used
    // useProguard.set(false)

    // Included builds to exclude from root helper tasks (obfuscateAll, etc.)
    excludedIncludedBuilds.set(listOf("NodePlugin"))
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
    coreProjectSuffix = 'core'

    // If proguard tasks should be used
    // useProguard = false

    // Included builds to exclude from root helper tasks (obfuscateAll, etc.)
    excludedIncludedBuilds = ['NodePlugin']
}
```

### Configuration Properties

| Property                 | Type | Default Value     | Description                                                                                                                 |
|:-------------------------| :--- |:------------------|:----------------------------------------------------------------------------------------------------------------------------|
| `annotatedPackages`      | `Property<String>` | `"ca.nodeengine"` | The package prefix(es) that NullAway should analyze for nullability.                                                        |
| `javaVersion`            | `Property<Int>` | `25`              | The Java version used for the Gradle toolchain and Java compatibility levels.                                               |
| `apiProjectSuffix`       | `Property<String>` | `"api"`           | Suffix used to identify "API" projects. API projects get Javadoc preprocessing and Javadoc JARs.                            |
| `coreProjectSuffix`      | `Property<String>` | `"core"`          | Suffix used to identify "Core" projects. Core projects include an `assets` folder in their resources.                       |
| `useProguard`            | `Property<Boolean>` | `true`              | If `true`, ProGuard tasks are enabled. It's `false` by default for api projects.                                             |
| `excludedIncludedBuilds` | `ListProperty<String>` | `["NodePlugin"]`  | List of included build names to exclude when running root helper tasks like `obfuscateAll` or `copyGradleToIncludedBuilds`. |


## Tasks

The plugin registers several tasks, some at the root project level and others in subprojects.

### Root Project Tasks

- `obfuscateAll`: Aggregates and runs `proguardObfuscate` for all subprojects and included builds (unless excluded).
- `publishAllToMavenLocal`: Publishes all subprojects and included builds to the local Maven repository.
- `copyGradleToIncludedBuilds`: Copies the root `gradle/` wrapper and `gradlew` scripts to all included builds.

### Subproject Tasks

- `proguardObfuscate`: (Non-API modules only) Obfuscates the project's JAR using ProGuard. Requires a `proguard-rules.pro` (or similar) file in the project or root directory.
- `preprocessJavadocSources`: (API modules only) Preprocesses Javadoc sources to fix common HTML issues (e.g., escaping `&` characters in Javadoc comments).
- `mavenJava`: A standard Maven publication is automatically configured for all subprojects.
