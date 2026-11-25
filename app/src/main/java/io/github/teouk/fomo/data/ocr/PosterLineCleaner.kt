package io.github.teouk.fomo.data.ocr

/**
 * Turns raw OCR lines into useful search suggestions. Pure Kotlin, so it is unit-tested.
 *
 * Posters are full of text that makes a poor search: prices, phone numbers, web
 * addresses, single letters misread from decorations. Event and venue names survive.
 */
object PosterLineCleaner {
    const val MAX_SUGGESTIONS = 10
    private const val MIN_LETTERS = 3

    private val urlOrEmail = Regex("""(https?://|www\.|\.co(m|\.uk)\b|@)""", RegexOption.IGNORE_CASE)
    private val priceOrPhone = Regex("""^[£$€]?\s*[\d\s.,:/+-]+$""")
    private val extraSpaces = Regex("""\s+""")

    /** Words that are never a search on their own, e.g. "THE" above "NUTCRACKER". */
    private val joiningWords = setOf("the", "a", "an", "and", "&", "of", "at", "with", "presents", "featuring", "feat.")

    fun clean(lines: List<String>): List<String> =
        joinLeadingWords(lines.map { it.replace(extraSpaces, " ").trim(' ', '-', '|', '•', '*', '.', ',') })
            .asSequence()
            .filter { it.count(Char::isLetter) >= MIN_LETTERS }
            .filterNot { urlOrEmail.containsMatchIn(it) }
            .filterNot { priceOrPhone.matches(it) }
            .distinctBy { it.lowercase() }
            .take(MAX_SUGGESTIONS)
            .toList()

    /**
     * Posters often split a title over lines in big type ("THE" / "NUTCRACKER").
     * A line made only of joining words is put in front of the line after it.
     */
    private fun joinLeadingWords(lines: List<String>): List<String> {
        val result = mutableListOf<String>()
        var carry = ""
        for (line in lines) {
            val words = line.lowercase().split(" ").filter { it.isNotEmpty() }
            if (words.isNotEmpty() && words.all { it in joiningWords }) {
                carry = "$carry $line".trim()
            } else {
                result += "$carry $line".trim()
                carry = ""
            }
        }
        return result
    }
}
