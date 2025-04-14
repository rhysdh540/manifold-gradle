package xyz.wagyourtail.manifold.settings

import groovy.lang.Closure
import groovy.lang.DelegatesTo
import org.gradle.api.Project
import org.gradle.api.initialization.ProjectDescriptor
import org.gradle.api.initialization.Settings
import xyz.wagyourtail.commonskt.collection.defaultedMapOf
import xyz.wagyourtail.commonskt.properties.FinalizeOnRead
import xyz.wagyourtail.commonskt.properties.MustSet
import xyz.wagyourtail.manifold.plugin.PreprocessorConfigList
import xyz.wagyourtail.manifold.plugin.manifold
import java.io.File

class SubprojectPreprocessorSettingsConfigList(val settings: Settings, val root: ProjectDescriptor) {

    var buildFile: File by FinalizeOnRead(MustSet())

    val projects = mutableMapOf<String, PreprocessorConfigList.PreprocessorConfig.() -> Unit>()
    val sourceSets = defaultedMapOf<String, MutableList<String>> { mutableListOf() }.also {
        it["main"] = mutableListOf("manifold/main")
    }

    var ideActiveSubproject: String? = null


    fun buildFile(buildFile: String) {
        this.buildFile = root.projectDir.resolve(buildFile)
    }

    @JvmOverloads
    fun project(project: String, buildFile: File = this.buildFile, config: PreprocessorConfigList.PreprocessorConfig.() -> Unit = {}) {
        val projectDir = root.projectDir.resolve(project)
        settings.include(projectDir.relativeTo(settings.rootDir).toString())
        project(settings.project(projectDir), buildFile, config)
    }

    @JvmOverloads
    fun project(
        project: String,
        buildFile: File = this.buildFile,
        @DelegatesTo(
            strategy = Closure.DELEGATE_FIRST,
            value = PreprocessorConfigList.PreprocessorConfig::class
        )
        action: Closure<*>
   ) {
        project(project, buildFile) {
            action.delegate = this
            action.resolveStrategy = Closure.DELEGATE_FIRST
            action.call(this)
        }
    }

    @JvmOverloads
    fun project(project: ProjectDescriptor, buildFile: File = this.buildFile, config: PreprocessorConfigList.PreprocessorConfig.() -> Unit = {}) {
        if (!project.path.startsWith(root.path)) {
            throw IllegalArgumentException("Project $project must be a subproject of $root")
        }

        project.buildFileName = buildFile.relativeTo(project.projectDir).toString()

        projects.put(project.path, config)
    }

    @JvmOverloads
    fun project(
        project: ProjectDescriptor,
        buildFile: File = this.buildFile,
        @DelegatesTo(
            strategy = Closure.DELEGATE_FIRST,
            value = PreprocessorConfigList.PreprocessorConfig::class
        )
        action: Closure<*>
    ) {
        project(project, buildFile) {
            action.delegate = this
            action.resolveStrategy = Closure.DELEGATE_FIRST
            action.call(this)
        }
    }

    @JvmOverloads
    fun sourceSet(name: String, addedRoot: String = "manifold/$name") {
        sourceSets[name].add(addedRoot)
    }

    fun sourceSets(vararg sourceSets: Pair<String, String>) {
        for (ss in sourceSets) {
            sourceSet(ss.first, ss.second)
        }
    }

    fun sourceSets(vararg sourceSets: String) {
        for (ss in sourceSets) {
            sourceSet(ss)
        }
    }

    fun apply(project: Project) {
        if (project.path == root.path) {
            project.manifold.subprojectPreprocessor {

                for (sourceSet in this@SubprojectPreprocessorSettingsConfigList.sourceSets) {
                    sourceSet(sourceSet.key, sourceSet.value.map { project.file(it) })
                }
                subprojects(projects)

                if (this@SubprojectPreprocessorSettingsConfigList.ideActiveSubproject != null) {
                    ideActiveSubproject = this@SubprojectPreprocessorSettingsConfigList.ideActiveSubproject!!
                }
            }
        }
    }

}