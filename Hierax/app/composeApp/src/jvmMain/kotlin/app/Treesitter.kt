package app

import androidx.compose.ui.graphics.Color
import org.treesitter.*

sealed class SyntaxColor(val color: Color = Color.Unspecified) {
    data object Keyword : SyntaxColor(Color(180, 70, 30))
    data object Function : SyntaxColor()
    data object Type : SyntaxColor()
    data object String : SyntaxColor(Color(30, 170, 70))
    data object Comment : SyntaxColor(Color.Gray)
    data object Number : SyntaxColor(Color(30, 180, 180))
    data object Punctuation : SyntaxColor()
    data object Operator : SyntaxColor()
    data object Annotation : SyntaxColor(Color(180, 180, 30))
    data object Other : SyntaxColor()
}

typealias HighlightList = List<Pair<IntRange, SyntaxColor>>

class KotlinSyntaxHighlighter {
    private val parser = TSParser()
    private val kotlinLanguage = TreeSitterKotlin()
    private val query: TSQuery

    init {
        parser.setLanguage(kotlinLanguage)
        query = TSQuery(kotlinLanguage, HIGHLIGHTS_QUERY)
    }

    /**
     * parses the given kotlin source code and returns a list of ranges with
     * their corresponding syntax color categories.
     *
     * @param sourceCode the kotlin code to highlight.
     * @return a list of pairs, where each pair contains an IntRange (byte offsets)
     *         and the determined SyntaxColor.
     */
    fun highlight(sourceCode: String): HighlightList {
        val highlights = mutableListOf<Pair<IntRange, SyntaxColor>>()

        val tree = parser.parseString(null, sourceCode)
            val rootNode = tree.rootNode
            val queryCursor = TSQueryCursor()
            val match = TSQueryMatch()

            queryCursor.exec(query, rootNode)

            while (queryCursor.nextMatch(match)) {
                for (capture in match.captures) {
                    val capturedNode = capture.node
                    val captureName = query.getCaptureNameForId(capture.index)

                    val color = mapCaptureNameToSyntaxColor(captureName)

                    val range = capturedNode.startByte until capturedNode.endByte

                    highlights.add(range to color)
                }
            }
        return highlights
    }

    private fun mapCaptureNameToSyntaxColor(captureName: String): SyntaxColor {
        return when (captureName) {
            "keyword", "keyword.operator", "keyword.function", "keyword.return" -> SyntaxColor.Keyword
            "function", "function.call", "function.method" -> SyntaxColor.Function
            "type", "type.builtin" -> SyntaxColor.Type
            "string", "string.escape", "character" -> SyntaxColor.String
            "comment" -> SyntaxColor.Comment
            "number", "float" -> SyntaxColor.Number
            "punctuation.bracket", "punctuation.delimiter", "punctuation.special" -> SyntaxColor.Punctuation
            "operator" -> SyntaxColor.Operator
            "annotation" -> SyntaxColor.Annotation
            else -> SyntaxColor.Other
        }
    }

    companion object {
        val HIGHLIGHTS_QUERY = """
        [(line_comment) (multiline_comment)] @comment

        ((string_literal) @string)
        ((character_literal) @character)

        (real_literal) @number
        (integer_literal) @number
        (long_literal) @number

        [
         "as"
         "break"
         "class"
         "continue"
         "do"
         "else"
         "false"
         "for"
         "fun"
         "if"
         "in"
         "interface"
         "is"
         "null"
         "object"
         "package"
         "return"
         "super"
         "this"
         "throw"
         "true"
         "try"
         "typealias"
         "val"
         "var"
         "when"
         "while"
         "by"
         "catch"
         "constructor"
         "delegate"
         "dynamic"
         "field"
         "file"
         "finally"
         "get"
         "import"
         "init"
         "param"
         "property"
         "receiver"
         "set"
         "setparam"
         "where"
         "actual"
         "abstract"
         "annotation"
         "companion"
         "crossinline"
         "data"
         "enum"
         "expect"
         "external"
         "final"
         "infix"
         "inline"
         "inner"
         "internal"
         "lateinit"
         "noinline"
         "open"
         "operator"
         "out"
         "override"
         "private"
         "protected"
         "public"
         "sealed"
         "suspend"
         "tailrec"
         "vararg"
        ] @keyword

        ("return" @keyword.return)
        ("as" "is" "in" @keyword.operator)
        ("fun" @keyword.function)
        
        (call_expression (simple_identifier) @function.call)
        (function_declaration (simple_identifier) @function)

        [
         (simple_identifier)
         (type_identifier)
        ] @variable
        
        (navigation_expression
          (simple_identifier) @variable.property)
          
        (class_declaration (type_identifier) @type)
        (user_type) @type
        
        [
          (annotation (user_type))
        ] @annotation

        [
         "&&"
         "||"
         "=="
         "!="
         "<"
         ">"
         "<="
         ">="
         "+"
         "-"
         "*"
         "/"
         "%"
        ] @operator

        [
         "."
         ","
         ":"
        ] @punctuation.delimiter

        [
         "("
         ")"
         "["
         "]"
         "{"
         "}"
        ] @punctuation.bracket
        """.trimIndent().trim()
    }
}

fun main() {
    mainAstDumper()
    val highlighter = KotlinSyntaxHighlighter()

    val kotlinCode = """
    package com.example.demo

    import java.util.Date

    data class User(val name: String, val age: Int)

    fun getGreeting(): String {
        val user = User("Alex", 30)
        val currentTime = Date().time
        if (user.age > 21) {
            return "Hello, ${'$'}{user.name}! Time is ${'$'}currentTime"
        } else {
            return "Hi."
        }
    }
    """.trimIndent()

    println(kotlinCode)

    val highlights: HighlightList = highlighter.highlight(kotlinCode)
    val codeBytes = kotlinCode.toByteArray(Charsets.UTF_8)

    highlights.forEach { (range, color) ->
        val text = codeBytes.sliceArray(range).toString(Charsets.UTF_8)
        println("range: $range, color: $color, text: \"$text\"")
    }
}

fun printTree(node: TSNode, sourceCode: String, level: Int = 0) {
    val indent = "  ".repeat(level)
    val nodeType = node.type
    val range = node.startByte until node.endByte
    val text = sourceCode.substring(node.startByte, node.endByte).replace("\n", "\\n")

    println("$indent$nodeType $range '$text'")

    for (i in 0 until node.childCount) {
        printTree(node.getChild(i), sourceCode, level + 1)
    }
}

fun mainAstDumper() {
    println(KotlinSyntaxHighlighter.HIGHLIGHTS_QUERY.slice(1262.. 1283))
    val parser = TSParser()
    parser.setLanguage(TreeSitterKotlin())

    val codeToInspect = """
    @Use
    class MyUser(val name: String)

    val user: MyUser = MyUser("Alex")
        /*
        some comment here
        */
    val text = 3.70f
    fun main() {}
    """.trimIndent()

    println(codeToInspect)

    val tree = parser.parseString(null, codeToInspect)
        printTree(tree.rootNode, codeToInspect)
}
