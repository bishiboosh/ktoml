package com.akuleshov7.ktoml.tree.nodes.pairs.values

import com.akuleshov7.ktoml.TomlOutputConfig
import com.akuleshov7.ktoml.writers.TomlEmitter

/**
 * Toml AST Node for a representation of float types: key = 1.01.
 * Toml specification requires floating point numbers to be IEEE 754 binary64 values,
 * so it should be Kotlin Double (64 bits)
 * @property content
 */
public class TomlDouble
internal constructor(
    override var content: Any
) : TomlValue() {
    public constructor(content: String, lineNo: Int) : this(content.toDouble())

    public constructor(content: Double, lineNo: Int) : this(content)

    override fun write(
        emitter: TomlEmitter,
        config: TomlOutputConfig
    ) {
        emitter.emitValue(content as Double)
    }
}
