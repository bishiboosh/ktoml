package com.akuleshov7.ktoml.tree

import com.akuleshov7.ktoml.TomlConfig
import com.akuleshov7.ktoml.exceptions.ParseException
import com.akuleshov7.ktoml.parsers.parseTomlKeyValue
import com.akuleshov7.ktoml.parsers.trimCurlyBraces

/**
 * Class for parsing and representing of inline tables: inline = { a = 5, b = 6 , c = 7 }
 * @property lineNo line number
 * @property keyValuePair parsed keyValue
 * @property config toml configuration
 */
public class TomlInlineTable(
    private val keyValuePair: Pair<String, String>,
    lineNo: Int,
    config: TomlConfig = TomlConfig(),
) : TomlNode(
    "${keyValuePair.first} = ${keyValuePair.second}",
    lineNo,
    config
) {
    override val name: String = keyValuePair.first
    private val tomlKeyValues: List<TomlNode>

    init {
        tomlKeyValues = keyValuePair.second.parseInlineTableValue()
    }

    private fun String.parseInlineTableValue(): List<TomlNode> {
        val parsedList = this
            .trimCurlyBraces()
            .trim()
            .also {
                if (it.endsWith(",")) {
                    throw ParseException(
                        "Trailing commas are not permitted in inline tables: [${keyValuePair.second}] ", lineNo
                    )
                }
            }
            .split(",")
            .map { it.parseTomlKeyValue(lineNo, config) }

        return parsedList
    }

    public fun returnTable(tomlFileHead: TomlFile, currentParentalNode: TomlNode): TomlTable {
        val tomlTable = TomlTablePrimitive(
            "[${if (currentParentalNode is TomlTable) "${currentParentalNode.fullTableName}." else ""}${keyValuePair.first}]",
            lineNo,
            config
        )

        // FixMe: this code duplication can be unified with the logic in TomlParser
        tomlKeyValues.forEach { keyValue ->
            when {
                keyValue is TomlKeyValue && keyValue.key.isDotted -> {
                    // in case parser has faced dot-separated complex key (a.b.c) it should create proper table [a.b],
                    // because table is the same as dotted key
                    val newTableSection = keyValue.createTomlTableFromDottedKey(tomlTable, config)

                    tomlFileHead
                        .insertTableToTree(newTableSection)
                        .appendChild(keyValue)
                }

                keyValue is TomlInlineTable -> tomlFileHead.insertTableToTree(
                    keyValue.returnTable(tomlFileHead, tomlTable)
                )

                // otherwise, it should simply append the keyValue to the parent
                else -> tomlTable.appendChild(keyValue)

            }
        }
        return tomlTable
    }
}
