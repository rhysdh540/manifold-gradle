package xyz.wagyourtail.manifold.plugin

import org.gradle.api.Project
import xyz.wagyourtail.commons.gradle.sourceSets
import java.io.File

class SubprojectPreprocessorConfig(val project: Project) {

    val subprojects: MutableSet<Project> = mutableSetOf()

    val sourceSets: MutableMap<String, MutableSet<File>> = mutableMapOf()

    var ideActiveSubproject: String? = null
        get() {
            if (field != null) return field
            ideActiveSubproject = project.extensions.extraProperties.properties["manifold.ideActiveSubproject"] as String?
            return field
        }
        set(value) {
            val active = project.extensions.extraProperties.properties["manifold.ideActiveSubproject"] as String?
            if (active == null) {
                field = subprojects.first().path
                return
            } else {
                for (sp in subprojects) {
                    if (sp.path.equals(active)) {
                        field = active
                        return
                    }
                }
                val trimmed = if (project.path.endsWith(":")) {
                    project.path
                } else {
                    "${project.path}:"
                }
                for (sp in subprojects) {
                    if (sp.path.equals(trimmed + active)) {
                        field = trimmed + active
                        return
                    }
                }
            }
            error("Could not find subproject for $value, expected ${subprojects.joinToString(", ")}")
        }

    fun sourceSet(name: String, addedRoot: File) {
        sourceSets.getOrPut(name) { mutableSetOf() }.add(addedRoot)
    }

    fun sourceSet(name: String, addedRoots: List<File>) {
        sourceSets.getOrPut(name) { mutableSetOf() }.addAll(addedRoots)
    }

    fun subproject(subproject: String, config: PreprocessorConfigList.PreprocessorConfig.() -> Unit) {
        subproject(project.project(subproject), config)
    }

    fun subproject(subproject: Project, config: PreprocessorConfigList.PreprocessorConfig.() -> Unit) {
        subprojects.add(subproject)

        subproject.apply(mapOf("plugin" to "xyz.wagyourtail.manifold"))

        subproject.manifold.preprocessor {
            sourceSets.clear()
            sourceSets.addAll(this@SubprojectPreprocessorConfig.sourceSets.keys)
            default(config)
        }
    }

    fun subprojects(subprojects: Map<String, PreprocessorConfigList.PreprocessorConfig.() -> Unit>) {
        for ((subproject, config) in subprojects) {
            subproject(subproject, config)
        }
    }

    init {
        project.afterEvaluate {
            apply()
        }
    }

    fun apply() {
        for (it in subprojects)  {

            val projectPath = if (project.path.endsWith(":")) {
                project.path
            } else {
                "${project.path}:"
            }

            // trick idea sync into not thinking the folder is shared by multiple projects
            if (isIdeaSync && ideActiveSubproject != it.path) {
                continue
            } else if (isIdeaSync) {
                project.logger.info("[Manifold] Setting active subproject to $ideActiveSubproject")
            }

            it.afterEvaluate {
                for (set in it.sourceSets) {
                    if (set.name in sourceSets) {
                        for (root in sourceSets[set.name]!!) {
                            set.java.srcDir(root.resolve("java"))
                            set.resources.srcDir(root.resolve("resources"))
                        }
                    }

                    // ensure writeBuildProperties is called *after* adding the directories
                    if (ideActiveSubproject == it.path) {
                        it.manifold.preprocessor {
                            writeBuildProperties(set, ideActiveConfig, resolvedConfigs[ideActiveConfig]!!)
                        }
                    }
                }
            }
        }
    }

    val isIdeaSync: Boolean
        get() = System.getProperty("idea.sync.active", "false").toBoolean()

}