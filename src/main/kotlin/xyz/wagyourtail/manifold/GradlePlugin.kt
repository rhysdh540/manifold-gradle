package xyz.wagyourtail.manifold

import org.gradle.api.JavaVersion
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

    companion object {
        fun JavaCompile.addManifoldArgs() {
            if ("-Xplugin:Manifold" !in options.compilerArgs) {
                options.compilerArgs.add("-Xplugin:Manifold")

                val javaVersion = options.release.orNull?.let { JavaVersion.toVersion(it) }
                    ?: javaCompiler.orNull?.metadata?.jvmVersion?.let { JavaVersion.toVersion(it) }
                    ?: JavaVersion.current()

                if (javaVersion > JavaVersion.VERSION_1_8 && inputs.files.any { it.name == "module-info.java" }) {
                    options.compilerArgs.addAll(listOf("--module-path", classpath.asPath))
                }
            }
        }
    }

    override fun apply(project: Project) {
        val logger = GradleLogWrapper(LoggingPrefix.builder()
            .loggerName("Manifold")
            .includeThreadName(false)
            .build(),
            project.logger
        )

        logger.lifecycle("Loaded Manifold Plugin $pluginVersion")

        project.extensions.create("manifold", ManifoldExtension::class.java)

        project.afterEvaluate {
            project.tasks.withType(JavaCompile::class.java).configureEach {
                it.addManifoldArgs()
            }
        }

    }

}