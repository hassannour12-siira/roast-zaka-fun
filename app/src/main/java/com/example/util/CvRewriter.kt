package com.example.util

import com.example.model.FullAnalysisResult

/**
 * Applies the rescue suggestions to the CV the user actually uploaded.
 *
 * This is deliberately a find-and-replace over their own text rather than asking a model
 * to regenerate the document. Regenerating invites invented employers, dates and numbers,
 * which is the one thing the whole app promises not to do. Every line this produces is
 * either untouched from the original or a rewrite the user has already seen on screen.
 *
 * A suggestion that cannot be located is reported, never silently dropped.
 */
object CvRewriter {

    data class Result(
        val text: String,
        val applied: List<String>,
        val notApplied: List<String>
    ) {
        val appliedCount: Int get() = applied.size
    }

    fun apply(originalCv: String, analysis: FullAnalysisResult): Result {
        var working = originalCv
        val applied = mutableListOf<String>()
        val notApplied = mutableListOf<String>()

        // The summary first: it is usually the largest single win.
        val summary = analysis.rescue.summary
        if (summary.improved.isNotBlank() && summary.original.isNotBlank()) {
            val replaced = replaceLoosely(working, summary.original, summary.improved)
            if (replaced != null) {
                working = replaced
                applied.add("Professional summary")
            } else {
                notApplied.add("Professional summary")
            }
        }

        for (rewrite in analysis.rescue.bulletRewrites) {
            if (rewrite.original.isBlank() || rewrite.improved.isBlank()) continue

            val replaced = replaceLoosely(working, rewrite.original, rewrite.improved)
            if (replaced != null) {
                working = replaced
                applied.add(rewrite.original.trim())
            } else {
                notApplied.add(rewrite.original.trim())
            }
        }

        return Result(text = working, applied = applied, notApplied = notApplied)
    }

    /**
     * Replace [target] with [replacement], tolerating whitespace differences.
     *
     * The model quotes lines back from text we extracted from a PDF, where a single bullet
     * may have been hard-wrapped across two lines or padded with odd spacing. An exact
     * string match therefore fails far more often than it should.
     *
     * Returns null when the target genuinely is not present, so the caller can report it.
     */
    private fun replaceLoosely(haystack: String, target: String, replacement: String): String? {
        if (haystack.contains(target)) return haystack.replaceFirst(target, replacement)

        val needle = normalise(target)
        if (needle.isEmpty()) return null

        // Walk the text building a normalised view while remembering where each normalised
        // character came from, so a match can be mapped back to real offsets.
        val builder = StringBuilder(haystack.length)
        val offsets = IntArray(haystack.length)
        var lastWasSpace = true
        for (i in haystack.indices) {
            val c = haystack[i]
            if (c.isWhitespace()) {
                if (!lastWasSpace && builder.isNotEmpty()) {
                    offsets[builder.length] = i
                    builder.append(' ')
                    lastWasSpace = true
                }
            } else {
                offsets[builder.length] = i
                builder.append(c.lowercaseChar())
                lastWasSpace = false
            }
        }
        val flattened = builder.toString().trim()
        val start = flattened.indexOf(needle)
        if (start < 0) return null

        val end = start + needle.length
        val realStart = offsets[start]
        // The end offset points at the last matched character, so step past it.
        val realEnd = if (end < builder.length) offsets[end] else haystack.length

        return haystack.substring(0, realStart) + replacement + haystack.substring(realEnd)
    }

    private fun normalise(text: String): String =
        text.trim().lowercase().replace(Regex("\\s+"), " ")
}
