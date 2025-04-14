package xyz.wagyourtail.manifold.plugin

import org.gradle.api.Project
import xyz.wagyourtail.commons.gradle.isIdeaSync
import xyz.wagyourtail.commons.gradle.sourceSets
import java.io.File

class SubprojectPreprocessorConfig(val project: Project) {

    val subprojects: MutableSet<Project> = mutableSetOf()

    val sourceSets: MutableMap<String, MutableSet<File>> = mutableMapOf()

    var ideActiveSubproject: String? = null
        set(active) {
            project.logger.info("[Manifold] setting ideActiveSubproject to ${active}")
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
            error("Could not find subproject for $active, expected ${subprojects.joinToString(", ")}")
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

    fun apply() {
        if (ideActiveSubproject == null) {
            ideActiveSubproject = project.extensions.extraProperties.properties["manifold.ideActiveSubproject"] as String?
        }

        for (it in subprojects)  {

            // trick idea sync into not thinking the folder is shared by multiple projects
            if (isIdeaSync && ideActiveSubproject != it.path) {
                continue
            } else if (isIdeaSync) {
                project.logger.lifecycle("[Manifold] Setting active subproject to $ideActiveSubproject")
            }

            it.apply(mapOf("plugin" to "java"))
            it.apply(mapOf("plugin" to "xyz.wagyourtail.manifold"))

            it.afterEvaluate {

                for (set in it.sourceSets) {
                    if (set.name in sourceSets) {
                        project.logger.info("[Manifold] adding paths for ${it.path} / ${set.name}")

                        for (root in sourceSets[set.name]!!) {
                            set.java.srcDir(root.resolve("java"))
                            set.resources.srcDir(root.resolve("resources"))
                        }
                    }

                    // ensure writeBuildProperties is called *after* adding the directories
                    if (ideActiveSubproject == it.path) {
                        it.manifold.preprocessor {
                            writeBuildProperties(set, ideActiveConfig, resolvedConfigs.getValue(ideActiveConfig))
                        }
                    }
                }

            }
        }
    }
}