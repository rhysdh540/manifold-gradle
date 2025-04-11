package xyz.wagyourtail.manifold

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile
import xyz.wagyourtail.commons.core.logger.prefix.LoggingPrefix
import xyz.wagyourtail.commons.gradle.GradleLogWrapper
import xyz.wagyourtail.commonskt.properties.FinalizeOnRead
import xyz.wagyourtail.manifold.plugin.ManifoldExtension
import kotlin.jvm.java

val pluginVersion by FinalizeOnRead(GradlePlugin::class.java.`package`.implementationVersion ?: "unknown")

class GradlePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val logger = GradleLogWrapper(LoggingPrefix.builder()
            .loggerName("Manifold")
            .build(),
            project.logger
        )

        logger.lifecycle("Loaded Manifold Plugin $pluginVersion")

        project.extensions.create("manifold", ManifoldExtension::class.java)

        project.afterEvaluate {
            project.tasks.withType(JavaCompile::class.java).configureEach {
                if ("-Xplugin:Manifold" !in it.options.compilerArgs) {
                    it.options.compilerArgs.add("-Xplugin:Manifold")
                }
            }
        }

    }

}