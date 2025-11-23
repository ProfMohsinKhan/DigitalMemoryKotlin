package com.alley.digitalmemory

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle

fun parseMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        // Regex for Bold (**text**), Italic (_text_), Cross (~~text~~)
        val parts = text.split(Regex("(\\*\\*.*?\\*\\*|__.*?__|~~.*?~~|\\[ \\].*?)"))

        // Hamein original text mein dhoondhna padega sequence maintain karne ke liye
        // Simple approach: Line by line scan karte hain

        var currentIndex = 0
        val boldRegex = Regex("\\*\\*(.*?)\\*\\*")
        val italicRegex = Regex("_(.*?)_")
        val strikeRegex = Regex("~~(.*?)~~")
        val checkboxRegex = Regex("\\[ \\](.*)")

        // Note: Full markdown parser is complex. This is a simplified version for your app.
        // Hum Text ko "Parts" mein process karenge.

        // ABHI KE LIYE: Basic implementation jo poore text par styles lagayegi agar match ho.
        // Real-time mixed styling (e.g. "Bold and _Italic_") ke liye humein complex loop chahiye.

        // Let's use a simpler trick: Replace symbols with styles line-by-line
        text.lines().forEach { line ->
            when {
                // Checkbox
                line.trim().startsWith("[ ]") -> {
                    withStyle(style = SpanStyle(color = Color.Gray)) {
                        append("☐ ")
                    }
                    append(line.replace("[ ]", "").trim())
                }
                // Bold
                boldRegex.containsMatchIn(line) -> {
                    val match = boldRegex.find(line)
                    val content = match?.groupValues?.get(1) ?: line
                    val before = line.substring(0, match?.range?.first ?: 0)
                    val after = line.substring(match?.range?.last?.plus(1) ?: line.length)

                    append(before)
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(content)
                    }
                    append(after)
                }
                // Italic
                italicRegex.containsMatchIn(line) -> {
                    val match = italicRegex.find(line)
                    val content = match?.groupValues?.get(1) ?: line
                    val before = line.substring(0, match?.range?.first ?: 0)
                    val after = line.substring(match?.range?.last?.plus(1) ?: line.length)

                    append(before)
                    withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(content)
                    }
                    append(after)
                }
                // Strikethrough
                strikeRegex.containsMatchIn(line) -> {
                    val match = strikeRegex.find(line)
                    val content = match?.groupValues?.get(1) ?: line
                    val before = line.substring(0, match?.range?.first ?: 0)
                    val after = line.substring(match?.range?.last?.plus(1) ?: line.length)

                    append(before)
                    withStyle(style = SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                        append(content)
                    }
                    append(after)
                }
                else -> {
                    append(line)
                }
            }
            append("\n")
        }
    }
}