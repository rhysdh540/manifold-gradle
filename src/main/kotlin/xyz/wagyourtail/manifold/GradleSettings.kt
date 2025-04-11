package xyz.wagyourtail.manifold

import org.gradle.api.Plugin
import org.gradle.api.Project
import xyz.wagyourtail.commons.core.logger.prefix.LoggingPrefix
import xyz.wagyourtail.commons.gradle.GradleLogWrapper
import xyz.wagyourtail.commonskt.properties.FinalizeOnRead
import kotlin.jvm.java

class GradlePlugin : Plugin<Project> {
    val version by FinalizeOnRead(GradlePlugin::class.java.`package`.implementationVersion ?: "unknown")

    override fun apply(project: Project) {
        val logger = GradleLogWrapper(LoggingPrefix.builder()
            .loggerName("Manifold")
            .build(),
            project.logger
        )

        logger.lifecycle("Loaded Manifold Plugin $version")
        project.extensions.create("manifold", ManifoldExtension::class.java)
    }

}