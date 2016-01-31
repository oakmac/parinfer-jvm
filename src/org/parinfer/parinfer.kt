//--------------------------------------------------------------------------------------------------
// parinfer-jvm
// TODO: about, license, name, etc
//--------------------------------------------------------------------------------------------------

package org.parinfer

import java.lang.Math
import java.util.ArrayList

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

final class StackItm(lineNo: Int, x: Int, ch: String, indentDelta: Int) {
    public val lineNo: Int = lineNo
    public val x: Int = x
    public val ch: String = ch
    public val indentDelta: Int = indentDelta
}

final class Result(text: String, mode: String) {
    public val mode: String = mode

    public val origText: String = text
    //public var origLines: String = ...

    public var lines: ArrayList<String> = arrayListOf<String>()
    public var lineNo: Int = -1
    public var ch: String = ""
    public var x: Int = 0

    public var parenStack: ArrayList<StackItm> = arrayListOf<StackItm>()

    public var parenTrailLineNo: Int? = null
    public var parenTrailStartX: Int? = null
    public var parenTrailendX: Int? = null
    public var parenTrailOpeners: ArrayList<StackItm> = arrayListOf<StackItm>()

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

// TODO: finish the error functions

//--------------------------------------------------------------------------------------------------
// String Operations
//--------------------------------------------------------------------------------------------------

fun insertWithinString(orig: String, idx: Int, insert: String) : String {
    return orig.substring(0, idx) + insert + orig.substring(idx)
}

fun replaceWithinString(orig: String, start: Int, end: Int, replace: String) : String {
    return orig.substring(0, start) + replace + orig.substring(end)
}

fun removeWithinString(orig: String, start: Int, end: Int) : String {
    return orig.substring(0, start) + orig.substring(end)
}

fun repeatString(text: String, n: Int) : String {
    return text.repeat(n)
}

fun getLineEnding(text: String) : String {
    // TODO: write me
    return "\n"
}

//--------------------------------------------------------------------------------------------------
// Line operations
//--------------------------------------------------------------------------------------------------

fun insertWithinLine(result: Result, lineNo: Int, idx: Int, insert: String) {
    val line = result.lines[lineNo]
    result.lines[lineNo] = insertWithinString(line, idx, insert)
}

fun replaceWithinLine(result: Result, lineNo: Int, start: Int, end: Int, replace: String) {
    val line = result.lines[lineNo]
    result.lines[lineNo] = replaceWithinString(line, start, end, replace)
}

fun removeWithinLine(result: Result, lineNo: Int, start: Int, end: Int) {
    val line = result.lines[lineNo]
    result.lines[lineNo] = removeWithinString(line, start, end)
}

fun initLine(result: Result, line: String) {
    result.x = 0
    result.lineNo = result.lineNo + 1
    result.lines.add(line)

    // reset line-specific state
    result.commentX = null
    result.indentDelta = 0
}

// if the current character has changed, commit its change to the current line.
fun commitChar(result: Result, origCh: String) {
    val ch = result.ch
    if (origCh != ch) {
        replaceWithinLine(result, result.lineNo, result.x, result.x + origCh.length, ch)
    }
    result.x = result.x + ch.length
}

//--------------------------------------------------------------------------------------------------
// Misc Util
//--------------------------------------------------------------------------------------------------

fun clamp(valN: Int, minN: Int?, maxN: Int?) : Int {
    var returnN = valN
    if (minN != null) {
        returnN = Math.max(minN, valN)
    }
    if (maxN != null) {
        returnN = Math.min(maxN, valN)
    }
    return returnN
}

// NOTE: the parinfer.js "peek" function is not really possible in a statically typed language

//--------------------------------------------------------------------------------------------------
// Character functions
//--------------------------------------------------------------------------------------------------

fun isValidCloseParen(parenStack: ArrayList<StackItm>, ch: String) : Boolean {
    val parenStackSize = parenStack.size()
    if (parenStackSize == 0) {
        return false
    }

    val lastStackItm = parenStack.get(parenStackSize - 1)
    return lastStackItm.ch == PARENS.get(ch)
}

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

fun main(args: Array<String>) {
    println( repeatString("YaY", 4) )
}
