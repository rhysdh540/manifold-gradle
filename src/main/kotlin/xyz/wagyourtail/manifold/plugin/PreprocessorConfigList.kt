package xyz.wagyourtail.manifold.plugin

import groovy.lang.Closure
import groovy.lang.DelegatesTo
import groovy.xml.dom.DOMCategory.name
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.language.jvm.tasks.ProcessResources
import xyz.wagyourtail.commons.gradle.sourceSets
import xyz.wagyourtail.commons.gradle.withSourceSet
import xyz.wagyourtail.commonskt.properties.LazyMutable
import java.io.File
import java.util.Locale
import java.util.Properties
import kotlin.jvm.java

class PreprocessorConfigList(val project: Project, val manifold: ManifoldExtension) {

    val configSets = mutableMapOf<String, PreprocessorConfig.() -> Unit>()
    val sourceSets = mutableListOf<String>("main")

    /**
     * set the active config that is used by the IDE
     * (aka, write to the build.properties file)
     */
    var ideActiveConfig: String by LazyMutable {
        project.extensions.extraProperties.properties["manifold.ideActiveConfig"] as String? ?: ""
    }

    fun sourceSet(sourceSet: String) {
        this.sourceSets.add(sourceSet)
    }

    fun sourceSet(sourceSet: SourceSet) {
        this.sourceSets.add(sourceSet.name)
    }

    fun sourceSet(sourceSet: NamedDomainObjectProvider<SourceSet>) {
        this.sourceSets.add(sourceSet.name)
    }

    fun sourceSets(sourceSets: List<Any>) {
        for (sourceSet in sourceSets) {
            when (sourceSet) {
                is String -> this.sourceSets.add(sourceSet)
                is SourceSet -> this.sourceSets.add(sourceSet.name)
                is NamedDomainObjectProvider<*> -> this.sourceSets.add(sourceSet.name)
                else -> throw IllegalArgumentException("sourceSet must be a String, SourceSet, or NamedDomainObjectProvider<SourceSet>")
            }
        }
    }

    fun default(config: PreprocessorConfig.() -> Unit) {
        configSets.compute("") { key, value ->
            { it: PreprocessorConfig ->
                value?.invoke(it)
                config(it)
            }
        }
    }

    fun config(config: PreprocessorConfig.() -> Unit) = config("", config)

    fun config(
        @DelegatesTo(
            strategy = Closure.DELEGATE_FIRST,
            value = PreprocessorConfig::class
        )
        action: Closure<*>
    ) {
        config {
            action.delegate = this
            action.resolveStrategy = Closure.DELEGATE_FIRST
            action.call(this)
        }
    }

    fun String.invoke(config: PreprocessorConfig.() -> Unit) = config(this, config)

    fun config(name: String, config: PreprocessorConfig.() -> Unit) {
        configSets.compute(name) { key, value ->
            { it: PreprocessorConfig ->
                value?.invoke(it)
                config(it)
            }
        }
    }

    fun config(
        name: String,
        @DelegatesTo(
            strategy = Closure.DELEGATE_FIRST,
            value = PreprocessorConfig::class
        )
        action: Closure<*>
    ) {
        config(name) {
            action.delegate = this
            action.resolveStrategy = Closure.DELEGATE_FIRST
            action.call(this)
        }
    }

    init {
        project.afterEvaluate {
            apply()
        }
    }

    val resolvedConfigs by lazy {
        val configs = mutableMapOf<String, PreprocessorConfig>()

        for ((name, config) in configSets) {
            configs[name] = PreprocessorConfig().apply {
                if (name != "") configSets[""]!!.invoke(this)
                config(this)
            }
        }

        configs
    }

    fun apply() {
        // ensure default set exists
        default { }

        val sourceSetProvider = project.sourceSets

        for (sourceSet in sourceSets) {
            val resolved = sourceSetProvider.findByName(sourceSet) ?: continue

            project.dependencies.add("annotationProcessor".withSourceSet(resolved), manifold("preprocessor"))

            for ((name, config) in resolvedConfigs) {
                applyConfig(resolved, name, config)
            }
        }
    }

    fun applyConfig(sourceSet: SourceSet, name: String, config: PreprocessorConfig) {
        // create tasks
        val processResources = "processResources${name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}"
            .withSourceSet(sourceSet)
        val compileJava = "compileJava${name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}"
            .withSourceSet(sourceSet)
        val classes = "classes${name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}"
            .withSourceSet(sourceSet)
        val jar = "jar${name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}"
            .withSourceSet(sourceSet)

        val processResourcesTask = project.tasks.maybeCreate(processResources, ProcessResources::class.java).apply {
            for (pr in config.processResources) {
                pr()
            }
        }

        val compileJavaTask = project.tasks.maybeCreate(compileJava, JavaCompile::class.java).apply {
            if ("-Xplugin:Manifold" !in options.compilerArgs) {
                options.compilerArgs.add("-Xplugin:Manifold")
            }
            for (cj in config.compileJava) {
                cj()
            }
        }

        val classesTask = project.tasks.maybeCreate(classes).apply {
            dependsOn(processResourcesTask, compileJavaTask)
            outputs.files(processResourcesTask, compileJavaTask)
        }

        if (config.jar.isNotEmpty()) {
            val jarTask = project.tasks.maybeCreate(jar, Jar::class.java).apply {
                dependsOn(classesTask)
                from(classesTask)

                for (j in config.jar) {
                    j()
                }
            }
        }

        if (name == ideActiveConfig) {
            writeBuildProperties(sourceSet, name, config)
        }
    }

    val subprojectHolder: SubprojectPreprocessorConfig? by lazy {
        var parent = project.parent
        while (parent != null) {
            val sub = parent.manifoldMaybe?.subprojectPreprocessorConfig?.subprojects?.map { it.path }?.toSet()
            if (sub != null && project.path in sub) {
                return@lazy parent.manifold.subprojectPreprocessorConfig
            }
            parent = parent.parent
        }
        null
    }

    val activeSubproject: Boolean by lazy {
        subprojectHolder?.ideActiveSubproject == project.path
    }

    fun writeBuildProperties(sourceSet: SourceSet, name: String, config: PreprocessorConfig) {
        val subprojectDirs = if (!activeSubproject) {
            subprojectHolder?.sourceSets[sourceSet.name] ?: emptySet()
        } else {
            emptySet()
        }
        for (file in sourceSet.allJava.sourceDirectories) {
            if (!file.exists()) continue
            if (file in subprojectDirs) continue
            project.logger.info("[Manifold] Writing build.properties to $file")

            val buildProperties = File(file, "build.properties")
            val properties = Properties()
            properties.putAll(config.properties.mapValues { it.value.toString() })
            buildProperties.writer().use {
                properties.store(it, "Configured by Manifold Gradle.\n$name\n")
            }
        }
    }

    class PreprocessorConfig {

        val properties = mutableMapOf<String, Any?>()

        var processResources = mutableListOf<ProcessResources.() -> Unit>({
            inputs.properties(properties)
        })

        val compileJava = mutableListOf<JavaCompile.() -> Unit>({
            options.compilerArgs.addAll(properties.map { "-A${it.key}=${it.value}" })
        })

        var jar = mutableListOf<Jar.() -> Unit>()

        @JvmOverloads
        fun property(name: String, value: Any? = "") {
            properties[name] = value
        }

        fun properties(config: Map<String, Any?>) {
            properties.putAll(config)
        }

        fun processResources(files: List<String>) {
            processResources.add {
                for (file in files) {
                    from(file) {
                        expand(properties)
                    }
                }
            }
        }

        fun processResources(config: ProcessResources.() -> Unit) {
            processResources.add(config)
        }

        fun compileJava(config: JavaCompile.() -> Unit) {
            compileJava.add(config)
        }

        fun jar(config: Jar.() -> Unit) {
            jar.add(config)
        }

    }

}