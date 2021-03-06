// parinfer-jvm - a Parinfer implementation for the JVM
// v0.4.0
// https://github.com/oakmac/parinfer-jvm
//
// More information about Parinfer can be found here:
// http://shaunlebron.github.io/parinfer/
//
// Copyright (c) 2016, Chris Oakman and other contributors
// Released under the ISC license
// https://github.com/oakmac/parinfer-jvm/blob/master/LICENSE.md

@file:JvmName("Parinfer")
package com.oakmac.parinfer

import java.util.*

//--------------------------------------------------------------------------------------------------
// Constants / Predicates
//--------------------------------------------------------------------------------------------------

enum class Mode { INDENT, PAREN }

val BACKSLASH = "\\"
val BACKSLASH_CHAR = '\\'
val BLANK_SPACE = " "
val DOUBLE_SPACE = "  "
val DOUBLE_QUOTE = "\""
val NEWLINE = "\n"
val SEMICOLON = ";"
val TAB = "\t"

val STANDALONE_PAREN_TRAIL = "^[\\s\\]\\)\\}]*(;.*)?$".toRegex()

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

class ParinferOptions(var cursorX: Int?, var cursorLine: Int?, var cursorDx: Int?, var previewCursorScope: Boolean);

enum class Error(val message: String) {
    QUOTE_DANGER("Quotes must balanced inside comment blocks."),
    EOL_BACKSLASH("Line cannot end in a hanging backslash."),
    UNCLOSED_QUOTE("String is missing a closing quote."),
    UNCLOSED_PAREN("Unmatched open-paren.")
}

class ParinferException(val result: MutableResult,
                        val error: Error,
                        val lineNo: Int = result.errorPosCache[error]!!.lineNo,
                        val x: Int = result.errorPosCache[error]!!.x) : Throwable();

class ErrorPos(val lineNo: Int, val x: Int)

class StackItm(val lineNo: Int,
               val x: Int,
               val ch: String,
               val indentDelta: Int)

class MutableResult(text: String, val mode: Mode, options: ParinferOptions) {
    val origText: String = text
    val origCursorX: Int? = options.cursorX
    var origLines: List<String> = text.lines()

    var lines: ArrayList<String> = arrayListOf()
    var lineNo: Int = -1
    var ch: String = ""
    var x: Int = 0

    var parenStack: Stack<StackItm> = Stack()

    var parenTrailLineNo: Int? = null
    var parenTrailStartX: Int? = null
    var parenTrailEndX: Int? = null
    var parenTrailOpeners: Stack<StackItm> = Stack()

    var cursorX: Int? = options.cursorX
    var cursorLine: Int? = options.cursorLine
    var cursorDx: Int? = options.cursorDx
    val previewCursorScope: Boolean = options.previewCursorScope
    var canPreviewCursorScope: Boolean = false

    var isInCode: Boolean = true
    var isEscaping: Boolean = false
    var isInStr: Boolean = false
    var isInComment: Boolean = false
    var commentX: Int? = null

    var quoteDanger: Boolean = false
    var trackingIndent: Boolean = false
    var skipChar: Boolean = false
    var success: Boolean = false

    var maxIndent: Int? = null
    var indentDelta: Int = 0

    var error: ParinferException? = null

    var errorPosCache: HashMap<Error, ErrorPos> = HashMap()
}

class LineDelta(val lineNo: Int, val line: String)

// NOTE: this is the "public result" class
class ParinferResult(result: MutableResult) {
    @JvmField var text: String = ""
    @JvmField var success: Boolean = true
    @JvmField var cursorX: Int?
    @JvmField var error: ParinferException? = null
    @JvmField var changedLines: ArrayList<LineDelta>? = null

    init {
        if (result.success) {
            val lineEnding = getLineEnding(result.origText)
            this.text = result.lines.joinToString(lineEnding)
            this.cursorX = result.cursorX
            this.success = true
            this.changedLines = getChangedLines(result)
        }
        else {
            this.text = result.origText
            this.cursorX = result.origCursorX
            this.success = false
            this.error = result.error
        }
    }

    override fun toString(): String{
        return "ParinferResult(text='$text', cursorX=$cursorX, success=$success)"
    }
}

//--------------------------------------------------------------------------------------------------
// Errors
//--------------------------------------------------------------------------------------------------

fun cacheErrorPos(result: MutableResult, error: Error, lineNo: Int, x: Int) {
    result.errorPosCache[error] = ErrorPos(lineNo, x)
}

//--------------------------------------------------------------------------------------------------
// String Operations
//--------------------------------------------------------------------------------------------------

// NOTE: We assume that if the CR char "\r" is used anywhere,
//       then we should use CRLF line-endings after every line.
fun getLineEnding(text: String) : String {
    val i = text.indexOf("\r")
    if (i > 0) {
        return "\r\n"
    }
    return "\n"
}

//--------------------------------------------------------------------------------------------------
// Line operations
//--------------------------------------------------------------------------------------------------

fun isCursorAffected(result: MutableResult, start: Int, end: Int): Boolean {
    val cursorX = result.cursorX ?: return false
    if (cursorX === start &&
        cursorX === end) {
        return cursorX === 0;
    }
    return cursorX >= end;
}

fun shiftCursorOnEdit(result: MutableResult, lineNo: Int, start: Int, end: Int, replace: String) {
    var oldLength = end - start;
    var newLength = replace.length;
    var dx = newLength - oldLength;

    val cursorX = result.cursorX
    if (dx !== 0 &&
        result.cursorLine === lineNo &&
        cursorX !== null &&
        isCursorAffected(result, start, end)) {
        result.cursorX = cursorX + dx;
    }
}

fun replaceWithinLine(result: MutableResult, lineNo: Int, start: Int, end: Int, replace: String) {
    var line = result.lines[lineNo];
    if (end < line.length) {
        result.lines[lineNo] = line.replaceRange(start, end, replace)
    } else {
        result.lines[lineNo] = line.substring(0, start) + replace
    }

    shiftCursorOnEdit(result, lineNo, start, end, replace);
}

fun insertWithinLine(result: MutableResult, lineNo: Int, idx: Int, insert: String) {
    replaceWithinLine(result, lineNo, idx, idx, insert);
}

fun initLine(result: MutableResult, line: String) {
    result.x = 0
    result.lineNo++
    result.lines.add(line)

    // reset line-specific state
    result.commentX = null
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
        returnN = Math.max(minN, returnN)
    }
    if (maxN != null) {
        returnN = Math.min(maxN, returnN)
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
    if (parenStack.isEmpty()) {
        return false
    }

    val lastStackItm = parenStack.peek()
    return lastStackItm.ch == PARENS[ch]
}

fun onOpenParen(result: MutableResult) {
    if (result.isInCode) {
        val newStackItm = StackItm(result.lineNo, result.x, result.ch, result.indentDelta)
        result.parenStack.push(newStackItm)
    }
}

fun onMatchedCloseParen(result: MutableResult) {
    val opener = result.parenStack.peek()
    result.parenTrailEndX = result.x + 1
    result.parenTrailOpeners.push(opener)
    result.maxIndent = opener.x
    result.parenStack.pop()
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
            cacheErrorPos(result, Error.QUOTE_DANGER, result.lineNo, result.x)
        }
    }
    else {
        result.isInStr = true
        cacheErrorPos(result, Error.UNCLOSED_QUOTE, result.lineNo, result.x)
    }
}

fun onBackslash(result: MutableResult) {
    result.isEscaping = true
}

fun afterBackslash(result: MutableResult) {
    result.isEscaping = false

    if (result.ch == NEWLINE) {
        if (result.isInCode) {
            throw ParinferException(result, Error.EOL_BACKSLASH, result.lineNo, result.x - 1)
        }
        onNewline(result)
    }
}

fun onChar(result: MutableResult) {
    val ch = result.ch
    if (result.isEscaping) {
        afterBackslash(result)
    }
    else if (isOpenParen(ch)) {
        onOpenParen(result)
    }
    else if (isCloseParen(ch)) {
        onCloseParen(result)
    }
    else if (ch == DOUBLE_QUOTE) {
        onQuote(result)
    }
    else if (ch == SEMICOLON) {
        onSemicolon(result)
    }
    else if (ch == BACKSLASH) {
        onBackslash(result)
    }
    else if (ch == TAB) {
        onTab(result)
    }
    else if (ch == NEWLINE) {
        onNewline(result)
    }

    result.isInCode = !result.isInComment && !result.isInStr
}

//--------------------------------------------------------------------------------------------------
// Cursor functions
//--------------------------------------------------------------------------------------------------

fun isCursorOnLeft(result: MutableResult) : Boolean {
    val cursorX = result.cursorX
    return result.lineNo == result.cursorLine &&
           cursorX != null &&
           cursorX <= result.x
}

fun isCursorOnRight(result: MutableResult, x: Int?) : Boolean {
    val cursorX = result.cursorX
    return result.lineNo == result.cursorLine &&
           cursorX != null &&
           x != null &&
           cursorX > x
}

fun isCursorInComment(result: MutableResult) : Boolean {
    return isCursorOnRight(result, result.commentX)
}

fun handleCursorDelta(result: MutableResult) {
    val indentDelta = result.indentDelta
    val cursorDx = result.cursorDx

    if (cursorDx != null) {
        val hasCursorDelta = result.cursorLine == result.lineNo && result.cursorX == result.x

        if (hasCursorDelta) {
            result.indentDelta = indentDelta + cursorDx
        }
    }
}

//--------------------------------------------------------------------------------------------------
// Paren Trail functions
//--------------------------------------------------------------------------------------------------

fun resetParenTrail(result: MutableResult, lineNo: Int, x: Int) {
    result.parenTrailLineNo = lineNo
    result.parenTrailStartX = x
    result.parenTrailEndX = x
    result.parenTrailOpeners.clear()
    result.maxIndent = null
}

fun updateParenTrailBounds(result: MutableResult) {
    val line = result.lines[result.lineNo]
    val ch = result.ch
    val prevChIsBackslash = result.x > 0 &&
                            line[result.x - 1] == BACKSLASH_CHAR

    val shouldReset = result.isInCode &&
                      !isCloseParen(ch) &&
                      ch != "" &&
                      (ch != BLANK_SPACE || prevChIsBackslash) &&
                      ch != DOUBLE_SPACE

    if (shouldReset) {
        resetParenTrail(result, result.lineNo, result.x + 1)
    }
}

fun clampParenTrailToCursor(result: MutableResult) {
    val startX = result.parenTrailStartX
    val endX = result.parenTrailEndX

    val isCursorClamping = isCursorOnRight(result, startX) &&
                           !isCursorInComment(result)

    if (isCursorClamping) {
        val cursorX = result.cursorX as Int // TODO is this never null here?
        val newStartX = Math.max(startX as Int, cursorX)
        val newEndX = Math.max(endX as Int, cursorX)

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
            result.parenTrailOpeners.removeAt(0)
            j++
        }
        result.parenTrailStartX = newStartX
        result.parenTrailEndX = newEndX
    }
}

fun popParenTrail(result: MutableResult) {
    val startX = result.parenTrailStartX as Int
    val endX = result.parenTrailEndX as Int

    if (startX == endX) {
        return
    }

    while (result.parenTrailOpeners.isNotEmpty()) {
        result.parenStack.push(result.parenTrailOpeners.pop())
    }
}

fun correctParenTrail(result: MutableResult, indentX: Int) {
    var parens = ""

    while (result.parenStack.isNotEmpty()) {
        val opener = result.parenStack.peek()
        if (opener.x >= indentX) {
            result.parenStack.pop()
            parens += PARENS[opener.ch]
        }
        else {
            break
        }
    }

    replaceWithinLine(result, result.parenTrailLineNo as Int, result.parenTrailStartX as Int, result.parenTrailEndX as Int, parens)
}

fun cleanParenTrail(result: MutableResult) {
    val startX = result.parenTrailStartX as Int
    val endX = result.parenTrailEndX as Int

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
        result.parenTrailEndX = endX - spaceCount
    }
}

fun appendParenTrail(result: MutableResult) {
    val opener = result.parenStack.pop()
    val closeCh = PARENS[opener.ch] as String

    result.maxIndent = opener.x
    val endX = result.parenTrailEndX
    if (endX != null) {
        insertWithinLine(result, result.parenTrailLineNo as Int, endX, closeCh)
        result.parenTrailEndX = endX + 1
    }
}

fun finishNewParenTrail(result: MutableResult) {
    if (result.mode == Mode.INDENT) {
        clampParenTrailToCursor(result)
        popParenTrail(result)
    }
    else if (result.mode == Mode.PAREN) {
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

    if (result.parenStack.isNotEmpty()) {
        val opener = result.parenStack.peek()
        minIndent = opener.x + 1
        newIndent += opener.indentDelta
    }

    newIndent = clamp(newIndent, minIndent, maxIndent)

    if (newIndent != origIndent) {
        var indentStr = " ".repeat(newIndent)
        replaceWithinLine(result, result.lineNo, 0, origIndent, indentStr)
        result.x = newIndent
        result.indentDelta += (newIndent - origIndent)
    }
}

fun tryPreviewCursorScope(result: MutableResult) {
    if (result.canPreviewCursorScope) {
        // If the cursor is to the right of current indentation point we can show
        // scope by adding close-parens to the cursor.
        // (i.e. close-parens may be safely moved from the previous Paren Trail to
        // a new Paren Trail at the cursor since there are no tokens between them.)
        val cursorX = result.cursorX
        val cursorLine = result.cursorLine
        if (cursorX != null && cursorLine != null) {
            if (cursorX > result.x) {
                correctParenTrail(result, cursorX);
                resetParenTrail(result, cursorLine, cursorX);
            }
        }
        result.canPreviewCursorScope = false;
    }
}

fun onIndent(result: MutableResult) {
    result.trackingIndent = false

    if (result.quoteDanger) {
        throw ParinferException(result, Error.QUOTE_DANGER)
    }

    if (result.mode == Mode.INDENT) {
        tryPreviewCursorScope(result)
        correctParenTrail(result, result.x)
    }
    else if (result.mode == Mode.PAREN) {
        correctIndent(result)
    }
}

fun onLeadingCloseParen(result: MutableResult) {
    result.skipChar = true

    if (result.mode == Mode.PAREN) {
        if (isValidCloseParen(result.parenStack, result.ch)) {
            if (isCursorOnLeft(result)) {
                result.skipChar = false
                onIndent(result)
            }
            else {
                appendParenTrail(result)
            }
        }
    }
}

fun checkIndent(result: MutableResult) {
    if (isCloseParen(result.ch)) {
        onLeadingCloseParen(result)
    }
    else if (result.ch == SEMICOLON) {
        // comments don't count as indentation points
        result.trackingIndent = false
    }
    else if (result.ch != NEWLINE &&
             result.ch !== BLANK_SPACE &&
             result.ch !== TAB) {
        onIndent(result)
    }
}

fun initPreviewCursorScope(result: MutableResult) {
    if (result.previewCursorScope && result.cursorLine == result.lineNo) {
        var semicolonX = result.lines[result.lineNo].indexOf(";");
        val cursorX = result.cursorX
        result.canPreviewCursorScope = (result.trackingIndent &&
                                        STANDALONE_PAREN_TRAIL.matches(result.lines[result.lineNo]) &&
                                        (semicolonX == -1 || (cursorX != null && cursorX <= semicolonX)));
    }
}

fun initIndent(result: MutableResult) {
    if (result.mode == Mode.INDENT) {
        result.trackingIndent = (result.parenStack.isNotEmpty() &&
                                 !result.isInStr);

        initPreviewCursorScope(result);
    }
    else if (result.mode === Mode.PAREN) {
        result.trackingIndent = !result.isInStr;
    }
}

//--------------------------------------------------------------------------------------------------
// High-level processing functions
//--------------------------------------------------------------------------------------------------

fun processChar(result: MutableResult, ch: String) {
    val origCh = ch

    result.ch = ch
    result.skipChar = false

    if (result.mode == Mode.PAREN) {
        handleCursorDelta(result)
    }

    if (result.trackingIndent && ch != BLANK_SPACE && ch != TAB) {
        checkIndent(result)
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
    initIndent(result)

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
        throw ParinferException(result, Error.QUOTE_DANGER)
    }
    if (result.isInStr) {
        throw ParinferException(result, Error.UNCLOSED_QUOTE)
    }

    if (result.parenStack.isNotEmpty()) {
        if (result.mode == Mode.PAREN) {
            val opener = result.parenStack.peek()
            throw ParinferException(result, Error.UNCLOSED_PAREN, opener.lineNo, opener.x)
        }
        else if (result.mode == Mode.INDENT) {
            result.x = 0
            onIndent(result)
        }
    }

    result.success = true
}

// NOTE: processError function not needed due to the type system

fun processText(text: String, options: ParinferOptions, mode: Mode) : MutableResult {
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

fun indentMode(text: String, cursorX: Int?, cursorLine: Int?, cursorDx: Int?, previewCursorScope: Boolean): ParinferResult {
    val options = ParinferOptions(cursorX, cursorLine, cursorDx, previewCursorScope)
    val result = processText(text, options, Mode.INDENT)
    return ParinferResult(result)
}

fun parenMode(text: String, cursorX: Int?, cursorLine: Int?, cursorDx: Int?, previewCursorScope: Boolean): ParinferResult {
    val options = ParinferOptions(cursorX, cursorLine, cursorDx, previewCursorScope)
    val result = processText(text, options, Mode.PAREN)
    return ParinferResult(result)
}
