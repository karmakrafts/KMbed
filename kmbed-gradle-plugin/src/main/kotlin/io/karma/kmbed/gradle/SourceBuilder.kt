package io.karma.kmbed.gradle

import org.intellij.lang.annotations.Language
import java.util.*

internal class SourceBuilder {
    private val builder: StringBuilder = StringBuilder()
    private var indentStack: Stack<Int> = Stack()
    private var currentIndent: Int = 0

    fun append(@Language("kotlin") s: String) {
        for (i in 0..<currentIndent) {
            builder.append('\t')
        }
        builder.append(s)
    }

    fun line(@Language("kotlin") s: String) = append("$s\n")

    fun newline() {
        builder.append('\n')
    }

    fun pushIndent() {
        indentStack.push(currentIndent)
        currentIndent++
    }

    fun pushIndent(indent: Int) {
        indentStack.push(currentIndent)
        currentIndent = indent
    }

    fun popIndent() {
        currentIndent = indentStack.pop()
    }

    fun render(): String = builder.toString()
}