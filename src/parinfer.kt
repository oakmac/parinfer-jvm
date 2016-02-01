// parinfer-jvm - a Parinfer implementation for the JVM
// v0.1.0-DEV
// https://github.com/oakmac/parinfer-jvm
//
// More information about Parinfer can be found here:
// http://shaunlebron.github.io/parinfer/
//
// Copyright (c) 2016, Chris Oakman and other contributors
// Released under the ISC license
// https://github.com/oakmac/parinfer-jvm/blob/master/LICENSE.md

package com.github.oakmac.parinfer

import java.lang.Math
import java.util.ArrayList
import java.util.HashMap
import java.util.Stack

//--------------------------------------------------------------------------------------------------
// Constants / Predicates
//--------------------------------------------------------------------------------------------------

val INDENT_MODE = "INDENT_MODE"
val PAREN_MODE = "PAREN_MODE"

val BACKSLASH = "\\"
val BACKSLASH_CHAR = '\\'
val BLANK_SPACE = " "
val DOUBLE_SPACE = "  "
val DOUBLE_QUOTE = "\""
val NEWLINE = "\n"
val SEMICOLON = ";"
val TAB = "\t"

// NOTE: LINE_ENDING not needed

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
// Class Definitions
//--------------------------------------------------------------------------------------------------

class ParinferOptions(cursorX: Int?, cursorLine: Int?, cursorDx: Int?) {
    public var cursorX: Int = -1
    public var cursorLine: Int = -1
    public var cursorDx: Int = -1

    init {
        if (cursorX != null) {
            this.cursorX = cursorX
        }
        if (cursorLine != null) {
            this.cursorLine = cursorLine
        }
        if (cursorDx != null) {
            this.cursorDx = cursorDx
        }
    }
}

class ParinferException(result: MutableResult, errorName: String, errorMessage: String, lineNo: Int?, x: Int?) : Throwable() {
    public val name: String = errorName
    public val description: String = errorMessage
    public var lineNo: Int = -1
    public var x: Int = -1

    init {
        if (lineNo == null) {
            this.lineNo = result.errorPosCache[errorName]!!.lineNo
        }
        else {
            this.lineNo = lineNo
        }

        if (x == null) {
            this.x = result.errorPosCache[errorName]!!.x
        }
        else {
            this.x = x
        }
    }
}

class ErrorPos(val lineNo: Int, val x: Int)

class StackItm(val lineNo: Int,
               val x: Int,
               val ch: String,
               val indentDelta: Int)

class MutableResult(text: String, mode: String, options: ParinferOptions?) {
    public val mode: String = mode

    public val origText: String = text
    public var origLines: List<String> = text.split("\\r?\\n".toRegex())

    public var lines: ArrayList<String> = arrayListOf()
    public var lineNo: Int = -1
    public var ch: String = ""
    public var x: Int = 0

    public var parenStack: Stack<StackItm> = Stack()

    public var parenTrailLineNo: Int = -1
    public var parenTrailStartX: Int = -1
    public var parenTrailEndX: Int = -1
    public var parenTrailOpeners: Stack<StackItm> = Stack()

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

    public var error: ParinferException? = null

    public var errorPosCache: HashMap<String, ErrorPos> = HashMap()

    init {
        if (options != null) {
            this.cursorX = options.cursorX
            this.cursorLine = options.cursorLine
            this.cursorDx = options.cursorDx
        }
    }
}

class LineDelta(val lineNo: Int, val line: String)

// NOTE: this is the "public result" class
class ParinferResult(result: MutableResult) {
    @JvmField public var text: String = ""
    @JvmField public var success: Boolean = true
    @JvmField public var error: ParinferException? = null
    @JvmField public var changedLines: ArrayList<LineDelta>? = null

    init {
        if (result.success) {
            val lineEnding = getLineEnding(result.origText)
            this.text = poorMansJoin(result.lines, lineEnding)
            this.success = true
            this.changedLines = getChangedLines(result)
        }
        else {
            this.text = result.origText
            this.success = false
            this.error = result.error
        }
    }
}

//--------------------------------------------------------------------------------------------------
// Errors
//--------------------------------------------------------------------------------------------------

val ERROR_QUOTE_DANGER = "quote-danger"
val ERROR_EOL_BACKSLASH = "eol-backslash"
val ERROR_UNCLOSED_QUOTE = "unclosed-quote"
val ERROR_UNCLOSED_PAREN = "unclosed-paren"

val QUOTE_DANGER_MSG = "Quotes must balanced inside comment blocks."
val EOL_BACKSLASH_MSG = "Line cannot end in a hanging backslash."
val UNCLOSED_QUOTE_MSG = "String is missing a closing quote."
val UNCLOSED_PAREN_MSG = "Unmatched open-paren."

fun cacheErrorPos(result: MutableResult, errorName: String, lineNo: Int, x: Int) {
    result.errorPosCache[errorName] = ErrorPos(lineNo, x)
}

//--------------------------------------------------------------------------------------------------
// String Operations
//--------------------------------------------------------------------------------------------------

fun insertWithinString(orig: String, idx: Int, insert: String) : String {
    if (insert == "") {
        return orig
    }

    val head = orig.substring(0, idx)
    var tail = orig.substring(idx, orig.length)
    return head + insert + tail
}

fun replaceWithinString(orig: String, start: Int, end: Int, replace: String) : String {
    var head = orig.substring(0, start)
    var tail = ""
    if (end < orig.length) {
        tail = orig.substring(end, orig.length)
    }

    return head + replace + tail
}

// NOTE: We assume that if the CR char "\r" is used anywhere,
//       then we should use CRLF line-endings after every line.
fun getLineEnding(text: String) : String {
    val i = text.indexOf("\r")
    if (i > 0) {
        return "\r\n"
    }
    return "\n"
}

fun poorMansJoin(arr: ArrayList<String>, lf: String) : String {
    var theStr = ""
    var i = 0
    while (i < arr.size) {
        theStr += arr[i] + lf
        i++
    }

    // remove the last newline character
    return theStr.substring(0, theStr.length - 1)
}

//--------------------------------------------------------------------------------------------------
// Line operations
//--------------------------------------------------------------------------------------------------

fun insertWithinLine(result: MutableResult, lineNo: Int, idx: Int, insert: String) {
    val line = result.lines[lineNo]
    result.lines[lineNo] = insertWithinString(line, idx, insert)
}

fun replaceWithinLine(result: MutableResult, lineNo: Int, start: Int, end: Int, replace: String) {
    val line = result.lines[lineNo]
    result.lines[lineNo] = replaceWithinString(line, start, end, replace)
}

fun removeWithinLine(result: MutableResult, lineNo: Int, start: Int, end: Int) {
    val line = result.lines[lineNo]
    if (start != end) {
        result.lines[lineNo] = line.removeRange(start, end)
    }
}

fun initLine(result: MutableResult, line: String) {
    result.x = 0
    result.lineNo++
    result.lines.add(line)

    // reset line-specific state
    result.commentX = -1
    result.indentDelta = 0
}

// if the current character has changed, commit its change to the current line.
fun commitChar(result: MutableResult, origCh: String) {
    val ch = result.ch
    if (origCh != ch) {
        replaceWithinLine(result, result.lineNo, result.x, result.x + origCh.length, ch)
    }
    result.x += ch.length
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

fun getChangedLines(result: MutableResult) : ArrayList<LineDelta> {
    var changedLines = ArrayList<LineDelta>()
    var i = 0
    while (i < result.lines.size) {
        if (result.lines[i] != result.origLines[i]) {
            val lineDelta = LineDelta(i, result.lines[i])
            changedLines.add(lineDelta)
        }
        i++
    }
    return changedLines
}

//--------------------------------------------------------------------------------------------------
// Character functions
//--------------------------------------------------------------------------------------------------

fun isValidCloseParen(parenStack: Stack<StackItm>, ch: String) : Boolean {
    val parenStackSize = parenStack.size
    if (parenStackSize == 0) {
        return false
    }

    val lastStackItm = parenStack[parenStackSize - 1]
    return lastStackItm.ch == PARENS[ch]
}

fun onOpenParen(result: MutableResult) {
    if (result.isInCode) {
        val newStackItm = StackItm(result.lineNo, result.x, result.ch, result.indentDelta)
        result.parenStack.push(newStackItm)
    }
}

fun onMatchedCloseParen(result: MutableResult) {
    if (result.parenStack.size > 0) {
        val opener = result.parenStack.peek()
        result.parenTrailEndX = result.x + 1
        result.parenTrailOpeners.push(opener)
        result.maxIndent = opener.x
        result.parenStack.pop()
    }
}

fun onUnmatchedCloseParen(result: MutableResult) {
    result.ch = ""
}

fun onCloseParen(result: MutableResult) {
    if (result.isInCode) {
        if (isValidCloseParen(result.parenStack, result.ch)) {
            onMatchedCloseParen(result)
        }
        else {
            onUnmatchedCloseParen(result)
        }
    }
}

fun onTab(result: MutableResult) {
    if (result.isInCode) {
        result.ch = DOUBLE_SPACE
    }
}

fun onSemicolon(result: MutableResult) {
    if (result.isInCode) {
        result.isInComment = true
        result.commentX = result.x
    }
}

fun onNewline(result: MutableResult) {
    result.isInComment = false
    result.ch = ""
}

fun onQuote(result: MutableResult) {
    if (result.isInStr) {
        result.isInStr = false
    }
    else if (result.isInComment) {
        result.quoteDanger = ! result.quoteDanger
        if (result.quoteDanger) {
            cacheErrorPos(result, ERROR_QUOTE_DANGER, result.lineNo, result.x)
        }
    }
    else {
        result.isInStr = true
        cacheErrorPos(result, ERROR_UNCLOSED_QUOTE, result.lineNo, result.x)
    }
}

fun onBackslash(result: MutableResult) {
    result.isEscaping = true
}

fun afterBackslash(result: MutableResult) {
    result.isEscaping = false

    if (result.ch == NEWLINE) {
        if (result.isInCode) {
            throw ParinferException(result, ERROR_EOL_BACKSLASH, EOL_BACKSLASH_MSG, result.lineNo, result.x - 1)
        }
        onNewline(result)
    }
}

fun onChar(result: MutableResult) {
    val ch = result.ch
    if (result.isEscaping)       { afterBackslash(result) }
    else if (isOpenParen(ch))    { onOpenParen(result) }
    else if (isCloseParen(ch))   { onCloseParen(result) }
    else if (ch == DOUBLE_QUOTE) { onQuote(result) }
    else if (ch == SEMICOLON)    { onSemicolon(result) }
    else if (ch == BACKSLASH)    { onBackslash(result) }
    else if (ch == TAB)          { onTab(result) }
    else if (ch == NEWLINE)      { onNewline(result) }

    result.isInCode = !result.isInComment && !result.isInStr
}

//--------------------------------------------------------------------------------------------------
// Cursor functions
//--------------------------------------------------------------------------------------------------

fun isCursorOnLeft(result: MutableResult) : Boolean {
    return result.lineNo == result.cursorLine &&
           result.cursorX != -1 &&
           result.cursorX <= result.x
}

fun isCursorOnRight(result: MutableResult, x: Int) : Boolean {
    return result.lineNo == result.cursorLine &&
           result.cursorX != -1 &&
           x != -1 &&
           result.cursorX > x
}

fun isCursorInComment(result: MutableResult) : Boolean {
    return isCursorOnRight(result, result.commentX)
}

fun handleCursorDelta(result: MutableResult) {
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

fun updateParenTrailBounds(result: MutableResult) {
    val line = result.lines[result.lineNo]
    val ch = result.ch
    val prevChIsBackslash = result.x > 0 &&
                            line[result.x - 1] == BACKSLASH_CHAR

    val shouldReset = result.isInCode &&
                      ! isCloseParen(ch) &&
                      ch != "" &&
                      (ch != BLANK_SPACE || prevChIsBackslash) &&
                      ch != DOUBLE_SPACE

    if (shouldReset) {
        result.parenTrailLineNo = result.lineNo
        result.parenTrailStartX = result.x + 1
        result.parenTrailEndX = result.x + 1
        result.parenTrailOpeners.clear()
        result.maxIndent = -1
    }
}

fun clampParenTrailToCursor(result: MutableResult) {
    val startX = result.parenTrailStartX
    val endX = result.parenTrailEndX

    val isCursorClamping = isCursorOnRight(result, startX) &&
                           ! isCursorInComment(result)

    if (isCursorClamping) {
        val newStartX = Math.max(startX, result.cursorX)
        val newEndX = Math.max(endX, result.cursorX)

        val line = result.lines[result.lineNo]
        var removeCount = 0
        var i = startX
        while (i < newStartX) {
            if (isCloseParen(line[i].toString())) {
                removeCount++
            }
            i++
        }

        var j = 0
        while (j < removeCount) {
            result.parenTrailOpeners.pop()
            j++
        }
        result.parenTrailStartX = newStartX
        result.parenTrailEndX = newEndX
    }
}

fun removeParenTrail(result: MutableResult) {
    val startX = result.parenTrailStartX
    val endX = result.parenTrailEndX

    if (startX == endX) {
        return
    }

    val openers = result.parenTrailOpeners
    while (! openers.empty()) {
        result.parenStack.push(openers.pop())
    }

    removeWithinLine(result, result.lineNo, startX, endX)
}

fun correctParenTrail(result: MutableResult, indentX: Int) {
    var parens = ""

    while (! result.parenStack.empty()) {
        val opener = result.parenStack.peek()
        if (opener.x >= indentX) {
            result.parenStack.pop()
            parens += PARENS[opener.ch]
        }
        else {
            break
        }
    }

    insertWithinLine(result, result.parenTrailLineNo, result.parenTrailStartX, parens)
}

fun cleanParenTrail(result: MutableResult) {
    val startX = result.parenTrailStartX
    val endX = result.parenTrailEndX

    if (startX == endX || result.lineNo != result.parenTrailLineNo) {
        return
    }

    val line = result.lines[result.lineNo]
    var newTrail = ""
    var spaceCount = 0
    var i = startX
    while (i < endX) {
        if (isCloseParen(line[i].toString())) {
            newTrail += line[i]
        }
        else {
            spaceCount++
        }
        i++
    }

    if (spaceCount > 0) {
        replaceWithinLine(result, result.lineNo, startX, endX, newTrail)
        result.parenTrailEndX -= spaceCount
    }
}

fun appendParenTrail(result: MutableResult) {
    val opener = result.parenStack.pop()
    val closeCh = PARENS[opener.ch].toString()

    result.maxIndent = opener.x
    insertWithinLine(result, result.parenTrailLineNo, result.parenTrailEndX, closeCh)
    result.parenTrailEndX++
}

fun finishNewParenTrail(result: MutableResult) {
    if (result.mode == INDENT_MODE) {
        clampParenTrailToCursor(result)
        removeParenTrail(result)
    }
    else if (result.mode == PAREN_MODE) {
        if (result.lineNo != result.cursorLine) {
            cleanParenTrail(result)
        }
    }
}

//--------------------------------------------------------------------------------------------------
// Indentation functions
//--------------------------------------------------------------------------------------------------

fun correctIndent(result: MutableResult) {
    val origIndent = result.x
    var newIndent = origIndent
    var minIndent = 0
    val maxIndent = result.maxIndent

    if (! result.parenStack.empty()) {
        val opener = result.parenStack.peek()
        minIndent = opener.x + 1
        newIndent += opener.indentDelta
    }

    newIndent = clamp(newIndent, minIndent, maxIndent)

    if (newIndent != origIndent) {
        var indentStr = BLANK_SPACE
        if (newIndent > 0) {
            indentStr = BLANK_SPACE.repeat(newIndent)
        }
        replaceWithinLine(result, result.lineNo, 0, origIndent, indentStr)
        result.x = newIndent
        result.indentDelta += (newIndent - origIndent)
    }
}

fun onProperIndent(result: MutableResult) {
    result.trackingIndent = false

    if (result.quoteDanger) {
        throw ParinferException(result, ERROR_QUOTE_DANGER, QUOTE_DANGER_MSG, null, null)
    }

    if (result.mode == INDENT_MODE) {
        correctParenTrail(result, result.x)
    }
    else if (result.mode == PAREN_MODE) {
        correctIndent(result)
    }
}

fun onLeadingCloseParen(result: MutableResult) {
    result.skipChar = true
    result.trackingIndent = true

    if (result.mode == PAREN_MODE) {
        if (isValidCloseParen(result.parenStack, result.ch)) {
            if (isCursorOnLeft(result)) {
                result.skipChar = false
                onProperIndent(result)
            }
            else {
                appendParenTrail(result)
            }
        }
    }
}

fun onIndent(result: MutableResult) {
    if (isCloseParen(result.ch)) {
        onLeadingCloseParen(result)
    }
    else if (result.ch == SEMICOLON) {
        // comments don't count as indentation points
        result.trackingIndent = false
    }
    else if (result.ch != NEWLINE) {
        onProperIndent(result)
    }
}

//--------------------------------------------------------------------------------------------------
// High-level processing functions
//--------------------------------------------------------------------------------------------------

fun processChar(result: MutableResult, ch: String) {
    val origCh = ch

    result.ch = ch
    result.skipChar = false

    if (result.mode == PAREN_MODE) {
        handleCursorDelta(result)
    }

    if (result.trackingIndent && ch != BLANK_SPACE && ch != TAB) {
        onIndent(result)
    }

    if (result.skipChar) {
        result.ch = ""
    }
    else {
        onChar(result)
        updateParenTrailBounds(result)
    }

    commitChar(result, origCh)
}

fun processLine(result: MutableResult, line: String) {
    initLine(result, line)

    if (result.mode == INDENT_MODE) {
        result.trackingIndent = ! result.parenStack.empty() &&
                                ! result.isInStr
    }
    else if (result.mode == PAREN_MODE) {
        result.trackingIndent = ! result.isInStr
    }

    var i = 0
    val chars = line + NEWLINE
    while (i < chars.length) {
        processChar(result, chars[i].toString())
        i++
    }

    if (result.lineNo == result.parenTrailLineNo) {
        finishNewParenTrail(result)
    }
}

fun finalizeResult(result: MutableResult) {
    if (result.quoteDanger) {
        throw ParinferException(result, ERROR_QUOTE_DANGER, QUOTE_DANGER_MSG, null, null)
    }
    if (result.isInStr) {
        throw ParinferException(result, ERROR_UNCLOSED_QUOTE, UNCLOSED_QUOTE_MSG, null, null)
    }

    if (! result.parenStack.empty()) {
        if (result.mode == PAREN_MODE) {
            val opener = result.parenStack.peek()
            throw ParinferException(result, ERROR_UNCLOSED_PAREN, UNCLOSED_PAREN_MSG, opener.lineNo, opener.x)
        }
        else if (result.mode == INDENT_MODE) {
            correctParenTrail(result, 0)
        }
    }

    result.success = true
}

// NOTE: processError function not needed due to type system

fun processText(text: String, options: ParinferOptions?, mode: String) : MutableResult {
    var result = MutableResult(text, mode, options)

    try {
        var i = 0
        while (i < result.origLines.size) {
            processLine(result, result.origLines[i])
            i++
        }
        finalizeResult(result)
    }
    catch (err: ParinferException) {
        result.success = false
        result.error = err
    }

    return result
}

//--------------------------------------------------------------------------------------------------
// Public API
//--------------------------------------------------------------------------------------------------

fun indentMode(text: String, cursorX: Int?, cursorLine: Int?, cursorDx: Int?): ParinferResult {
    val options = ParinferOptions(cursorX, cursorLine, cursorDx)
    val result = processText(text, options, INDENT_MODE)
    return ParinferResult(result)
}

fun parenMode(text: String, cursorX: Int?, cursorLine: Int?, cursorDx: Int?): ParinferResult {
    val options = ParinferOptions(cursorX, cursorLine, cursorDx)
    val result = processText(text, options, PAREN_MODE)
    return ParinferResult(result)
}

//--------------------------------------------------------------------------------------------------
// DEBUG...
//--------------------------------------------------------------------------------------------------

/*
fun main(args: Array<String>) {
    // val result = indentMode("(def b [[c d] ])", 14, 0, null)
    // val expectedResult = "(def b [[c d] ])"
    val result = indentMode("(let [a 1])\n  ret)", 10, 0, null)
    val expectedResult = "(let [a 1]\n  ret)"

    if (result.text == expectedResult) {
        println("Yay! It worked")
    }
    else {
        println("No bueno :(")
        println( result.text )
    }
}
*/
