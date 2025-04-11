package xyz.wagyourtail.manifold.settings

import groovy.lang.Closure
import groovy.lang.DelegatesTo
import org.gradle.api.initialization.ProjectDescriptor
import org.gradle.api.initialization.Settings

open class ManifoldSettingsExtension(val settings: Settings) {

    val subprojectPreprocessor = mutableMapOf<String, SubprojectPreprocessorSettingsConfigList>()

    fun subprojectPreprocessor(root: String, config: SubprojectPreprocessorSettingsConfigList.() -> Unit) {
        val projectDir = settings.rootDir.resolve(root)
        settings.include(projectDir.relativeTo(settings.rootDir).toString())
        subprojectPreprocessor(settings.project(projectDir) ?: error("Could not find project $root"), config)
    }

    fun subprojectPreprocessor(
        root: String,
        @DelegatesTo(
            strategy = Closure.DELEGATE_FIRST,
            value = SubprojectPreprocessorSettingsConfigList::class
        )
        action: Closure<*>
    ) {
        subprojectPreprocessor(root) {
            action.delegate = this
            action.resolveStrategy = Closure.DELEGATE_FIRST
            action.call(this)
        }
    }

    fun subprojectPreprocessor(root: ProjectDescriptor, config: SubprojectPreprocessorSettingsConfigList.() -> Unit) {
        subprojectPreprocessor[root.path] = SubprojectPreprocessorSettingsConfigList(settings, root).apply(config)
    }

    fun subprojectPreprocessor(
        root: ProjectDescriptor,
        @DelegatesTo(
            strategy = Closure.DELEGATE_FIRST,
            value = SubprojectPreprocessorSettingsConfigList::class
        )
        action: Closure<*>
    ) {
        subprojectPreprocessor(root) {
            action.delegate = this
            action.resolveStrategy = Closure.DELEGATE_FIRST
            action.call(this)
        }
    }

    fun subprojectPreprocessor(config: SubprojectPreprocessorSettingsConfigList.() -> Unit) {
        subprojectPreprocessor(settings.rootProject, config)
    }

    fun subprojectPreprocessor(
        @DelegatesTo(
            strategy = Closure.DELEGATE_FIRST,
            value = SubprojectPreprocessorSettingsConfigList::class
        )
        action: Closure<*>
    ) {
        subprojectPreprocessor {
            action.delegate = this
            action.resolveStrategy = Closure.DELEGATE_FIRST
            action.call(this)
        }
    }

    init {
        settings.gradle.beforeProject { project ->
            project.apply(mapOf("plugin" to "xyz.wagyourtail.manifold"))

            if (project.path in subprojectPreprocessor) {
                subprojectPreprocessor[project.path]?.apply(project)
            } else if (project.parent?.path in subprojectPreprocessor) {
                subprojectPreprocessor[project.parent?.path]?.apply(project)
            }
        }

//        settings.gradle.afterProject { project ->
//            if (project.path in subprojectPreprocessor) {
//                project.manifold
//            }
//
//        }
    }

}