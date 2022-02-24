package com.akuleshov7.ktoml.source

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlConfig
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer
import okio.Source
import kotlin.native.concurrent.ThreadLocal

/**
 * This class can be used for reading [Source] in TOML format
 * @property serializersModule
 */
@OptIn(ExperimentalSerializationApi::class)
public open class TomlSourceReader(
    private val config: TomlConfig = TomlConfig(),
    override val serializersModule: SerializersModule = EmptySerializersModule
) : Toml(config, serializersModule) {

    /**
     * Simple deserializer of a source that contains toml.
     *
     * @param deserializer deserialization strategy
     * @param source source where toml is stored
     * @return deserialized object of type T
     */
    public fun <T> decodeFromSource(
        deserializer: DeserializationStrategy<T>,
        source: Source,
    ): T {
        return source.useLines { lines -> decodeFromString(deserializer, lines, config) }
    }

    /**
     * Simple deserializer of a source that contains toml.
     *
     * @param source source where toml is stored
     * @return deserialized object of type T
     */
    public inline fun <reified T> decodeFromSource(source: Source): T {
        return decodeFromSource(serializersModule.serializer(), source)
    }

    /**
     * Partial deserializer of a file that contains toml. Reading file with okio native library.
     * Will deserialize only the part presented under the tomlTableName table.
     * If such table is missing in he input - will throw an exception.
     *
     * (!) Useful when you would like to deserialize only ONE table
     * and you do not want to reproduce whole object structure in the code
     *
     * @param deserializer deserialization strategy
     * @param source source where toml is stored
     * @param tomlTableName fully qualified name of the toml table (it should be the full name -  a.b.c.d)
     * @return deserialized object of type T
     */
    public fun <T> partiallyDecodeFromSource(
        deserializer: DeserializationStrategy<T>,
        source: Source,
        tomlTableName: String,
    ): T {
        return source.useLines { lines ->
            partiallyDecodeFromLines(deserializer, lines, tomlTableName, config)
        }
    }

    /**
     * Partial deserializer of a file that contains toml. Reading file with okio native library.
     * Will deserialize only the part presented under the tomlTableName table.
     * If such table is missing in he input - will throw an exception.
     *
     * (!) Useful when you would like to deserialize only ONE table
     * and you do not want to reproduce whole object structure in the code
     *
     * @param source source where toml is stored
     * @param tomlTableName fully qualified name of the toml table (it should be the full name -  a.b.c.d)
     * @return deserialized object of type T
     */
    public inline fun <reified T> partiallyDecodeFromSource(
        source: Source,
        tomlTableName: String,
    ): T {
        return partiallyDecodeFromSource(serializersModule.serializer(), source, tomlTableName)
    }

    /**
     * The default instance of [TomlSourceReader] with the default configuration.
     * See [TomlConfig] for the list of the default options
     * ThreadLocal annotation is used here for caching.
     */
    @ThreadLocal
    public companion object Default : TomlSourceReader(TomlConfig())
}
