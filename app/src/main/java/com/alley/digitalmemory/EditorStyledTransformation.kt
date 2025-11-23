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
import androidx.compose.ui.unit.sp

class EditorStyledTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val rawText = text.text

        val styledText = buildAnnotatedString {
            append(rawText)

            // 1. BOLD (**text**)
            val boldRegex = Regex("\\*\\*(.*?)\\*\\*")
            boldRegex.findAll(rawText).forEach { match ->
                // Hide the stars (**)
                addStyle(SpanStyle(color = Color.Transparent), match.range.first, match.range.first + 2)
                addStyle(SpanStyle(color = Color.Transparent), match.range.last - 1, match.range.last + 1)

                // Style the content
                addStyle(
                    SpanStyle(fontWeight = FontWeight.Bold, color = Color.Black),
                    match.range.first + 2,
                    match.range.last - 1
                )
            }

            // 2. ITALIC (_text_)
            val italicRegex = Regex("_(.*?)_")
            italicRegex.findAll(rawText).forEach { match ->
                // Hide the underscores (_)
                addStyle(SpanStyle(color = Color.Transparent), match.range.first, match.range.first + 1)
                addStyle(SpanStyle(color = Color.Transparent), match.range.last, match.range.last + 1)

                // Style the content
                addStyle(
                    SpanStyle(fontStyle = FontStyle.Italic, color = Color.Black),
                    match.range.first + 1,
                    match.range.last
                )
            }

            // 3. CROSS (~~text~~)
            val strikeRegex = Regex("~~(.*?)~~")
            strikeRegex.findAll(rawText).forEach { match ->
                // Hide the tildes (~~)
                addStyle(SpanStyle(color = Color.Transparent), match.range.first, match.range.first + 2)
                addStyle(SpanStyle(color = Color.Transparent), match.range.last - 1, match.range.last + 1)

                // Style the content
                addStyle(
                    SpanStyle(textDecoration = TextDecoration.LineThrough, color = Color.Gray),
                    match.range.first + 2,
                    match.range.last - 1
                )
            }

            // 4. CHECKBOX ([ ])
            val checkboxRegex = Regex("\\[ \\]")
            checkboxRegex.findAll(rawText).forEach { match ->
                // Hide [ ]
                addStyle(SpanStyle(color = Color.Transparent), match.range.first, match.range.last + 1)

                // Iski jagah hum kuch draw nahi kar sakte text field me easily,
                // to hum bas text ko thoda Grey kar dete hain taaki alag lage
                // (Real checkbox rendering TextField me bohot complex hai)
            }
        }

        return TransformedText(styledText, OffsetMapping.Identity)
    }
}