package xyz.wagyourtail.manifold.plugin

import xyz.wagyourtail.commonskt.collection.defaultedMapOf
import xyz.wagyourtail.commonskt.collection.finalizable.FinalizableMap
import xyz.wagyourtail.commonskt.collection.finalizable.finalizableMapOf

class PreprocessorConfig {

    private val _propertySets = FinalizableMap<String?, FinalizableMap<String, Any>>(backing = defaultedMapOf { finalizableMapOf() })

    val propertySet = object : Map<String, Map<String, Any>> by _propertySets {}

    fun default(config: Map<String, Any>) {
        _propertySets[null]!!.putAll(config)
    }

    fun String.invoke(name: String, config: Map<String, Any>) {
        _propertySets[name]!!.putAll(config)
    }

    fun apply() {
        // ensure default set exists
        default(emptyMap())

        _propertySets.finalize()
        for (map in _propertySets.values) {
            map.finalize()
        }

        _propertySets.forEach(::applyPropertySet)
    }

    private fun applyPropertySet(name: String?, config: Map<String, Any>) {

    }

}