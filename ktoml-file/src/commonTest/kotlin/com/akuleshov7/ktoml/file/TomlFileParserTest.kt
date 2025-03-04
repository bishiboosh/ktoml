package com.akuleshov7.ktoml.file

import com.akuleshov7.ktoml.*
import com.akuleshov7.ktoml.parsers.TomlParser
import com.akuleshov7.ktoml.tree.TomlTablePrimitive
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TomlFileParserTest {
    @Serializable
    data class TestClass(
        val title: String,
        val owner: Owner,
        val database: Database
    )

    @Serializable
    data class Owner(
        val name: String,
        val dob: String,
        val mytest: MyTest
    )

    @Serializable
    data class Database(
        val server: String,
    )

    @Serializable
    data class MyTest(
        val myserver: String,
        val myotherserver: String
    )

    @Test
    fun readParseAndDecodeFile() {
        val expected = TestClass(
            "TOML \"Example\"",
            Owner(
                "Tom Preston-Werner",
                "1979-05-27T07:32:00-08:00",
                MyTest("test", "this is my \\ special \" [ value \" / ")
            ),
            Database(
                "192.168.1.1"
            )
        )
        assertEquals(
            expected,
            TomlFileReader().decodeFromFile(serializer(), "src/commonTest/resources/simple_example.toml")
        )
    }

    // ================
    @Serializable
    data class MyTableTest(
        val a: A,
        val d: D
    )

    @Serializable
    data class A(val b: Ab, val d: InnerTest)

    @Serializable
    data class Ab(val c: InnerTest)

    @Serializable
    data class D(val a: InnerTest)

    @Serializable
    data class InnerTest(val str: String = "Undefined")

    @Test
    @ExperimentalSerializationApi
    fun testTableDiscovery() {
        val file = "src/commonTest/resources/complex_toml_tables.toml"
        // ==== reading from file
        val test = MyTableTest(A(Ab(InnerTest("Undefined")), InnerTest("Undefined")), D(InnerTest("Undefined")))
        assertEquals(test, TomlFileReader.decodeFromFile(serializer(), file))
        // ==== checking how table discovery works
        val lines = readAndParseFile(file)
        val parsedResult = TomlParser(TomlConfig()).parseStringsToTomlTree(lines, TomlConfig())
        assertEquals(listOf("a", "a.b.c", "a.d", "d", "d.a"), parsedResult.getRealTomlTables().map { it.fullTableName })
    }

    @Serializable
    data class RegressionTest(val a: Long?, val b: Long, val c: Long, val d: Long?)

    @ExperimentalSerializationApi
    @Test
    fun regressionCast2Test() {
        val file = "src/commonTest/resources/class_cast_regression2.toml"
        val parsedResult = TomlFileReader.decodeFromFile<RegressionTest>(serializer(), file)
        assertEquals(RegressionTest(null, 1, 2, null), parsedResult)
    }

    @ExperimentalSerializationApi
    @Test
    fun regressionPartialTest() {
        val file = "src/commonTest/resources/class_cast_regression2.toml"
        val parsedResult = TomlFileReader.decodeFromFile<RegressionTest>(serializer(), file)
        assertEquals(RegressionTest(null, 1, 2, null), parsedResult)
    }


    @Serializable
    data class TestRegression(
        val list1: List<Double>,
        val general: GeneralConfig,
        val list2: List<Long>,
        val warn: WarnConfig,
        val list3: List<String>
    )

    @Serializable
    data class GeneralConfig(
        val execCmd: String? = null,
        val tags: List<String>? = null,
        val description: String? = null,
        val suiteName: String? = null,
        val excludedTests: List<String>? = null,
        val includedTests: List<String>? = null,
        val ignoreSaveComments: Boolean? = null
    )

    @Serializable
    data class WarnConfig(
        val list: List<String>
    )

    @ExperimentalSerializationApi
    @Test
    fun regressionInvalidIndex() {
        val file = "src/commonTest/resources/partial_parser_regression.toml"
        assertEquals(
            GeneralConfig(
                execCmd = "echo hello world",
                tags = listOf("Tag", "Other tag"),
                description = "My description",
                suiteName = "// DocsCheck",
                excludedTests = null,
                includedTests = null,
                ignoreSaveComments = null
            ),
            TomlFileReader.partiallyDecodeFromFile(serializer(), file, "general")
        )
        assertEquals(
            TestRegression(
                list1 = listOf(1.0, 2.0),
                general = GeneralConfig(
                    execCmd = "echo hello world",
                    tags = listOf("Tag", "Other tag"),
                    description = "My description",
                    suiteName = "// DocsCheck",
                    excludedTests = null,
                    includedTests = null,
                    ignoreSaveComments = null
                ),
                list2 = listOf(1, 3, 5),
                warn = WarnConfig(list = listOf("12a", "12f")),
                list3 = listOf("mystr", "2", "3")
            ),
            TomlFileReader.decodeFromFile(serializer(), file)
        )
    }

    @Serializable
    data class Table1(val a: Long, val b: Long)

    @Serializable
    data class Table2(val c: Long, val e: Long, val d: Long)

    @Serializable
    data class TwoTomlTables(val table1: Table1, val table2: Table2)

    @Test
    fun testPartialFileDecoding() {
        val file = "src/commonTest/resources/partial_decoder.toml"
        val test = TwoTomlTables(Table1(1, 2), Table2(1, 2, 3))
        assertEquals(
            test.table1,
            TomlFileReader.partiallyDecodeFromFile(
                serializer(), file, "table1"
            )
        )
    }

    @Test
    fun readTopLevelTables() {
        val file = "src/commonTest/resources/simple_example.toml"
        val lines = readAndParseFile(file)
        assertEquals(
            listOf("owner", "database"),
            TomlParser(TomlConfig())
                .parseStringsToTomlTree(lines, TomlConfig())
                .children
                .filterIsInstance<TomlTablePrimitive>()
                .filter { !it.isSynthetic }
                .map { it.fullTableName }
        )
    }

    @Test
    fun invalidFile() {
        val file = "src/commonTest/resources/simple_example.wrongext"
        assertFailsWith<IllegalStateException> {
            readAndParseFile(file)
        }
    }
}
