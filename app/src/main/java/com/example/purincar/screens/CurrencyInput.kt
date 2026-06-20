package com.example.purincar.screens

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Formats raw keypad input as a right-to-left currency amount (cents-first entry).
 * Typing "1", "2", "3" yields "0.01" -> "0.12" -> "1.23". Non-digits are ignored
 * and leading zeros are dropped so the value grows from the cents place.
 */
fun formatCentsInput(raw: String): String {
    val digits = raw.filter { it.isDigit() }.trimStart('0').take(9)
    if (digits.isEmpty()) return ""
    return "%.2f".format(digits.toLong() / 100.0)
}

/** Pre-fills a currency field from a stored amount: 12.0 -> "12.00", 0.0 -> "". */
fun centsPrefill(amount: Double): String =
    if (amount > 0) "%.2f".format(amount) else ""

/**
 * Wraps currency text in a [TextFieldValue] with the caret pinned to the end.
 * Required for cents-style entry: because we reformat the text on every keystroke,
 * a plain String field would leave the caret stranded mid-string and insert the
 * next digit in the wrong place. Keeping the caret at the end makes digits append.
 */
fun currencyFieldValue(text: String): TextFieldValue =
    TextFieldValue(text, selection = TextRange(text.length))
