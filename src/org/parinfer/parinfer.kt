//--------------------------------------------------------------------------------------------------
// parinfer-jvm
// TODO: about, license, name, etc
//--------------------------------------------------------------------------------------------------

package org.parinfer

//--------------------------------------------------------------------------------------------------
// Constants / Predicates
//--------------------------------------------------------------------------------------------------

val INDENT_MODE = "INDENT_MODE"
val PAREN_MODE = "PAREN_MODE"

val BACKSLASH = "\\"
val BLANK_SPACE = " "
val DOUBLE_SPACE = "  "
val DOUBLE_QUOTE = "\""
val NEWLINE = "\n"
val SEMICOLON = ";"
val TAB = "\t"

// TODO: LINE_ENDING regex

val PARENS = hashMapOf(
    "{" to "}",
    "}" to "{",
    "[" to "]",
    "]" to "[",
    "(" to ")",
    ")" to "(")

fun isOpenParen(c: String) : Boolean {
    return c == "(" || c == "{" || c == "["
}

fun isCloseParen(c: String) : Boolean {
    return c == ")" || c == "}" || c == "]"
}

//--------------------------------------------------------------------------------------------------
// Result Structure
//--------------------------------------------------------------------------------------------------

final class StackItm(lineNo: Int, x: Int, ch: Char, indentDelta: Int) {
    public val lineNo: Int = lineNo
    public val x: Int = x
    public val ch: Char = ch
    public val indentDelta: Int = indentDelta
}

final class Result(text: String, mode: String) {
    public val mode: String = mode

    public val origText: String = text
    //public var origLines: String = ...

    public var lines: Array<String> = arrayOf<String>()
    public var lineNo: Int = -1
    public var ch: String = ""
    public var x: Int = 0

    public var parenStack: Array<StackItm> = arrayOf<StackItm>()

    public var parenTrailLineNo: Int? = null
    public var parenTrailStartX: Int? = null
    public var parenTrailendX: Int? = null
    public var parenTrailOpeners: Array<StackItm> = arrayOf<StackItm>()

    public var cursorX: Int? = null
    public var cursorLine: Int? = null
    public var cursorDx: Int? = null

    public var isInCode: Boolean = true
    public var isEscaping: Boolean = false
    public var isInStr: Boolean = false
    public var isInComment: Boolean = false
    public var commentX: Int? = null

    public var quoteDanger: Boolean = false
    public var trackingIndent: Boolean = false
    public var skipChar: Boolean = false
    public var success: Boolean = false

    public var maxIndet: Int? = null
    public var indentDelta: Int = 0

    // TODO: create an Error object
    //public var errorName: String? = null
    //public var errorMessage: String? = null
    //public var errorLineNo: Int? = null
    //public var errorX: Int? = null

    //public var errorPosCache
}

//--------------------------------------------------------------------------------------------------
// Errors
//--------------------------------------------------------------------------------------------------

val ERROR_QUOTE_DANGER = "quote-danger"
val ERROR_EOL_BACKSLASH = "eol-backslash"
val ERROR_UNCLOSED_QUOTE = "unclosed-quote"
val ERROR_UNCLOSED_PAREN = "unclosed-paren"
val ERROR_UNHANDLED = "unhandled"

val errorMessages = hashMapOf(
    ERROR_QUOTE_DANGER to "Quotes must balanced inside comment blocks.",
    ERROR_EOL_BACKSLASH to "Line cannot end in a hanging backslash.",
    ERROR_UNCLOSED_QUOTE to "String is missing a closing quote.",
    ERROR_UNCLOSED_PAREN to "Unmatched open-paren.")

//--------------------------------------------------------------------------------------------------
// String Operations
//--------------------------------------------------------------------------------------------------

// TODO: write me

//--------------------------------------------------------------------------------------------------
// Line Operations
//--------------------------------------------------------------------------------------------------

// TODO: write me

//--------------------------------------------------------------------------------------------------
// Misc Util
//--------------------------------------------------------------------------------------------------

// TODO: write me

//--------------------------------------------------------------------------------------------------
// Character functions
//--------------------------------------------------------------------------------------------------

// TODO: write me

//--------------------------------------------------------------------------------------------------
// Cursor functions
//--------------------------------------------------------------------------------------------------

// TODO: write me

//--------------------------------------------------------------------------------------------------
// Paren Trail functions
//--------------------------------------------------------------------------------------------------

// TODO: write me

//--------------------------------------------------------------------------------------------------
// Indentation functions
//--------------------------------------------------------------------------------------------------

// TODO: write me

//--------------------------------------------------------------------------------------------------
// High-level processing functions
//--------------------------------------------------------------------------------------------------

// TODO: write me

//--------------------------------------------------------------------------------------------------
// Public API
//--------------------------------------------------------------------------------------------------

fun main(args: Array<String>) = //println("hello parinfer!")
    println( PARENS.get("{") )
