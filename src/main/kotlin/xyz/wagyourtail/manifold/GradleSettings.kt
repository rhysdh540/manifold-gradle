package xyz.wagyourtail.manifold

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import xyz.wagyourtail.commons.core.logger.SimpleLogger
import xyz.wagyourtail.commons.core.logger.prefix.LoggingPrefix
import xyz.wagyourtail.commonskt.properties.FinalizeOnRead
import xyz.wagyourtail.manifold.settings.ManifoldSettingsExtension
import kotlin.jvm.java

class GradleSettings : Plugin<Settings> {
    val version by FinalizeOnRead(GradlePlugin::class.java.`package`.implementationVersion ?: "unknown")

    override fun apply(project: Settings) {
        val logger = SimpleLogger.builder().prefix(
            LoggingPrefix.builder()
                .loggerName("Manifold")
                .build()
        ).build()

        logger.lifecycle("Loaded Manifold Settings Plugin $version")
        project.extensions.create("manifold", ManifoldSettingsExtension::class.java, project)
    }

}