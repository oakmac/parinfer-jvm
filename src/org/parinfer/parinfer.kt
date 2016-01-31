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

    public var parenTrailLineNo: Int = -1
    public var parenTrailStartX: Int = -1
    public var parenTrailEndX: Int = -1
    public var parenTrailOpeners: ArrayList<StackItm> = arrayListOf<StackItm>()

    public var cursorX: Int = -1
    public var cursorLine: Int = -1
    public var cursorDx: Int = -1

    public var isInCode: Boolean = true
    public var isEscaping: Boolean = false
    public var isInStr: Boolean = false
    public var isInComment: Boolean = false
    public var commentX: Int = -1

    public var quoteDanger: Boolean = false
    public var trackingIndent: Boolean = false
    public var skipChar: Boolean = false
    public var success: Boolean = false

    public var maxIndent: Int = -1
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
    result.commentX = -1
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

fun peek(arr: ArrayList<StackItm>) : StackItm? {
    val arrSize = arr.size()
    if (arrSize == 0) {
        return null
    }
    return arr.get(arrSize - 1)
}

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

fun onOpenParen(result: Result) {
    if (result.isInCode) {
        val newStackItm = StackItm(result.lineNo, result.x, result.ch, result.indentDelta)
        result.parenStack.add(newStackItm)
    }
}

fun onMatchedCloseParen(result: Result) {
    val parenStackSize = result.parenStack.size()
    if (parenStackSize > 0) {
        val opener = result.parenStack.get(parenStackSize - 1)
        result.parenTrailEndX = result.x + 1
        result.parenTrailOpeners.add(opener)
        result.maxIndent = opener.x
        result.parenStack.remove(parenStackSize - 1)
    }
}

fun onUnmatchedCloseParen(result: Result) {
    result.ch = ""
}

fun onCloseParen(result: Result) {
    if (result.isInCode) {
        if (isValidCloseParen(result.parenStack, result.ch)) {
            onMatchedCloseParen(result)
        }
        else {
            onUnmatchedCloseParen(result)
        }
    }
}

fun onTab(result: Result) {
    if (result.isInCode) {
        result.ch = DOUBLE_SPACE
    }
}

fun onSemicolon(result: Result) {
    if (result.isInCode) {
        result.isInComment = true
        result.commentX = result.x
    }
}

fun onNewline(result: Result) {
    result.isInComment = false
    result.ch = ""
}

fun onQuote(result: Result) {
    if (result.isInStr) {
        result.isInStr = false
    }
    else if (result.isInComment) {
        result.quoteDanger = ! result.quoteDanger
        if (result.quoteDanger) {
            // TODO:
            //cacheErrorPos()
        }
    }
    else {
        result.isInStr = true
        // TODO:
        //cacheErrorPos()
    }
}

fun onBackslash(result: Result) {
    result.isEscaping = true
}

fun afterBackslash(result: Result) {
    result.isEscaping = false

    if (result.ch == NEWLINE) {
        if (result.isInCode) {
            // TODO: figure out throw
        }
        onNewline(result)
    }
}

fun onChar(result: Result) {
    val ch = result.ch;
    if (result.isEscaping)       { afterBackslash(result) }
    else if (isOpenParen(ch))    { onOpenParen(result) }
    else if (isCloseParen(ch))   { onCloseParen(result) }
    else if (ch == DOUBLE_QUOTE) { onQuote(result) }
    else if (ch == SEMICOLON)    { onSemicolon(result) }
    else if (ch == BACKSLASH)    { onBackslash(result) }
    else if (ch == TAB)          { onTab(result) }
    else if (ch == NEWLINE)      { onNewline(result) }

    result.isInCode = !result.isInComment && !result.isInStr;
}

//--------------------------------------------------------------------------------------------------
// Cursor functions
//--------------------------------------------------------------------------------------------------

fun isCursorOnLeft(result: Result) : Boolean {
    return result.lineNo == result.cursorLine &&
           result.cursorX != -1 &&
           result.cursorX <= result.x
}

fun isCursorOnRight(result: Result, x: Int) : Boolean {
    return result.lineNo == result.cursorLine &&
           result.cursorX != -1 &&
           x != -1 &&
           result.cursorX > x
}

fun isCursorInComment(result: Result) : Boolean {
    return isCursorOnRight(result, result.commentX)
}

fun handleCursorDelta(result: Result) {
    val hasCursorDelta = result.cursorDx != -1 &&
                         result.cursorLine == result.lineNo &&
                         result.cursorX == result.x

    if (hasCursorDelta) {
        result.indentDelta = result.indentDelta + result.cursorDx
    }
}

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
