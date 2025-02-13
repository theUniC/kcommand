package io.github.theunic.kcommand.core

import kotlinx.serialization.KSerializer
import kotlin.reflect.KClass

class MessageRegistry<M : Any> {
    private val classToEntry = mutableMapOf<KClass<out M>, Pair<String, KSerializer<out M>>>()
    private val nameToSerializer = mutableMapOf<String, KSerializer<out M>>()

    fun <T : M> register(kclass: KClass<T>, serializer: KSerializer<T>) {
        val typeName = kclass.qualifiedName.toString()
        classToEntry[kclass] = typeName to serializer
        nameToSerializer[typeName] = serializer
    }

    fun typeNameAndSerializer(kclass: KClass<out M>): Pair<String, KSerializer<out M>>? {
        return classToEntry[kclass]
    }

    fun serializerFor(typeName: String): KSerializer<out M>? {
        return nameToSerializer[typeName]
    }
}
