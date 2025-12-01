package com.alley.digitalmemory

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration

class EditorStyledTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val rawText = text.text

        val styledText = buildAnnotatedString {
            append(rawText)

            // 1. BOLD (**text**)
            Regex("\\*\\*(.*?)\\*\\*").findAll(rawText).forEach { match ->
                addStyle(SpanStyle(color = Color.Transparent), match.range.first, match.range.first + 2)
                addStyle(SpanStyle(color = Color.Transparent), match.range.last - 1, match.range.last + 1)
                addStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color.Black), match.range.first + 2, match.range.last - 1)
            }

            // 2. ITALIC (_text_)
            Regex("_(.*?)_").findAll(rawText).forEach { match ->
                addStyle(SpanStyle(color = Color.Transparent), match.range.first, match.range.first + 1)
                addStyle(SpanStyle(color = Color.Transparent), match.range.last, match.range.last + 1)
                addStyle(SpanStyle(fontStyle = FontStyle.Italic, color = Color.Black), match.range.first + 1, match.range.last)
            }

            // 3. STRIKE (~~text~~)
            Regex("~~(.*?)~~").findAll(rawText).forEach { match ->
                addStyle(SpanStyle(color = Color.Transparent), match.range.first, match.range.first + 2)
                addStyle(SpanStyle(color = Color.Transparent), match.range.last - 1, match.range.last + 1)
                addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough, color = Color.Gray), match.range.first + 2, match.range.last - 1)
            }

            // 4. TASK: UNCHECKED (⚪)
            // Just make it look nice (maybe a bit larger or specific color)
            Regex("⚪").findAll(rawText).forEach { match ->
                addStyle(SpanStyle(color = Color.DarkGray, fontWeight = FontWeight.Bold), match.range.first, match.range.last + 1)
            }

            // 5. TASK: CHECKED (🔘) -> STRIKETHROUGH REST OF LINE
            // Ye logic dhoondhega: 🔘 Button + Uske baad ka text
            Regex("🔘(.*)").findAll(rawText).forEach { match ->
                // Icon Color (Primary/Blue)
                addStyle(SpanStyle(color = Color(0xFF6200EE)), match.range.first, match.range.first + 1) // Just the button

                // Strike through the text part
                if (match.groups.size > 1) {
                    val textRange = match.groups[1]?.range
                    if (textRange != null) {
                        addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough, color = Color.Gray), textRange.first, textRange.last + 1)
                    }
                }
            }
        }

        return TransformedText(styledText, OffsetMapping.Identity)
    }
}