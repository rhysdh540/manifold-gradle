package xyz.wagyourtail.manifold.settings

import org.gradle.api.Project
import org.gradle.api.initialization.ProjectDescriptor
import org.gradle.api.initialization.Settings
import xyz.wagyourtail.commonskt.properties.FinalizeOnRead
import xyz.wagyourtail.commonskt.properties.MustSet
import xyz.wagyourtail.manifold.plugin.manifold
import java.io.File

class SubprojectPreprocessorConfigList(val settings: Settings, val root: ProjectDescriptor) {

    var buildFile: File by FinalizeOnRead(MustSet())

    val projects = mutableListOf<String>()

    fun addProject(project: String) {
        addProject(settings.project("${root.path}:$project"))
    }

    fun addProject(project: ProjectDescriptor) {
        if (!project.path.startsWith(root.path)) {
            throw IllegalArgumentException("Project $project must be a subproject of $root")
        }

        project.buildFileName = buildFile.relativeTo(project.projectDir).toString()

        projects.add(project.path)
    }

    fun apply(project: Project) {
        if (project.path == root.path) {
            project.manifold.subprojectPreprocessor.set(true)
        } else if (project.path in projects) {
            //
        }
    }

}