package xyz.wagyourtail.manifold

import groovy.lang.Closure
import groovy.lang.DelegatesTo
import org.gradle.api.Project
import org.gradle.api.provider.Property
import java.util.concurrent.Callable
import javax.inject.Inject

abstract class ManifoldExtension @Inject constructor(val project: Project) {

    abstract val version: Property<String>

    init {
        version.finalizeValueOnRead()
    }

    @JvmName("module")
    operator fun invoke(name: String) =
        project.dependencies.create("systems.manifold:manifold-$name:${version.get()}")


    private val preprocessor by lazy {
        PreprocessorConfig()
    }

    fun preprocessor(config: PreprocessorConfig.() -> Unit) {
        preprocessor.apply(config)
        preprocessor.apply()
    }

    fun preprocessor(
        @DelegatesTo(
            strategy = Closure.DELEGATE_FIRST,
            value = PreprocessorConfig::class
        )
        action: Closure<PreprocessorConfig>
    ) {
        preprocessor {
            action.delegate = this
            action.resolveStrategy = Closure.DELEGATE_FIRST
            action.call(this)
        }
    }


}