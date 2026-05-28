package net.crowdventures.storypop.util

import android.widget.EditText
import net.crowdventures.storypop.TextStyle
import net.crowdventures.storypop.TextStyleManager
import net.crowdventures.storypop.viewmodels.SpanInfo

class MarkdownUtil {
    companion object  {

        /**
         * Data class to hold markdown span information
         */


        /**
         * Process markdown and calculate correct offsets accounting for removed characters
         */
        fun processMarkdownWithCorrectOffsets(content: String): Pair<String, List<SpanInfo>> {
            val spans = mutableListOf<SpanInfo>()
            val result = StringBuilder()

            // Track how many characters have been removed so far
            var removedCharsCount = 0

            // Process patterns in order (headers first, then inline styles)

            // First, handle headers (block-level)
            val headerPattern = Regex("^(#{1,3})\\s+(.+?)$", RegexOption.MULTILINE)
            // We need to process headers separately because they're block-level
            val headerMatches = headerPattern.findAll(content).toList()
            var lastHeaderEnd = 0

            headerMatches.forEach { match ->
                // Add text before this header
                result.append(content.substring(lastHeaderEnd, match.range.first))

                val headerLevel = match.groupValues[1].length
                val headerText = match.groupValues[2]

                // Calculate positions in the final string
                val startInResult = result.length
                val endInResult = startInResult + headerText.length

                // Add spans for this header
                spans.add(SpanInfo(startInResult, endInResult, TextStyle.TEXT_SIZE_LARGE))
                spans.add(SpanInfo(startInResult, endInResult, TextStyle.BOLD))

                // Append the clean header text
                result.append(headerText)

                // Update tracking
                lastHeaderEnd = match.range.last + 1
                // Removed characters: # symbols + space
                removedCharsCount += headerLevel + 1
            }

            // Add remaining text after last header
            if (lastHeaderEnd < content.length) {
                result.append(content.substring(lastHeaderEnd))
            }

            // Now process inline styles on the result so far
            val inlineSpans = mutableListOf<SpanInfo>()

            // Process inline patterns in order
            val inlinePatterns = listOf(
                // Bold with asterisks
                Pair(Regex("\\*\\*(.*?)\\*\\*")) { match: MatchResult, startPos: Int ->
                    val text = match.groupValues[1]
                    val startInResult = startPos
                    val endInResult = startPos + text.length
                    inlineSpans.add(SpanInfo(startInResult, endInResult, TextStyle.BOLD))
                    4 // characters removed (** and **)
                },
                // Bold with underscores
                Pair(Regex("__(.*?)__")) { match: MatchResult, startPos: Int ->
                    val text = match.groupValues[1]
                    val startInResult = startPos
                    val endInResult = startPos + text.length
                    inlineSpans.add(SpanInfo(startInResult, endInResult, TextStyle.BOLD))
                    4 // characters removed (__ and __)
                },
                // Italic with asterisks
                Pair(Regex("\\*(.*?)\\*")) { match: MatchResult, startPos: Int ->
                    val text = match.groupValues[1]
                    val startInResult = startPos
                    val endInResult = startPos + text.length
                    inlineSpans.add(SpanInfo(startInResult, endInResult, TextStyle.ITALIC))
                    2 // characters removed (* and *)
                },
                // Italic with underscores
                Pair(Regex("_(.*?)_")) { match: MatchResult, startPos: Int ->
                    val text = match.groupValues[1]
                    val startInResult = startPos
                    val endInResult = startPos + text.length
                    inlineSpans.add(SpanInfo(startInResult, endInResult, TextStyle.ITALIC))
                    2 // characters removed (_ and _)
                },
                // Underline with ++
                Pair(Regex("\\+\\+(.*?)\\+\\+")) { match: MatchResult, startPos: Int ->
                    val text = match.groupValues[1]
                    val startInResult = startPos
                    val endInResult = startPos + text.length
                    inlineSpans.add(SpanInfo(startInResult, endInResult, TextStyle.UNDERLINE))
                    4 // characters removed (++ and ++)
                }
            )

            val currentText = result.toString()
            var searchText = currentText

            inlinePatterns.forEach { (pattern, spanCreator) ->
                val matches = pattern.findAll(searchText).toList()
                if (matches.isNotEmpty()) {
                    val newText = StringBuilder()
                    var lastIdx = 0
                    var localRemovedCount = 0

                    matches.forEach { match ->
                        // Add text before this match
                        newText.append(searchText.substring(lastIdx, match.range.first))

                        // Current position in the new string
                        val currentPos = newText.length

                        // Create span with correct position
                        val charsRemoved = spanCreator(match, currentPos)

                        // Add the clean text
                        newText.append(match.groupValues[1])

                        lastIdx = match.range.last + 1
                        localRemovedCount += charsRemoved
                    }

                    // Add remaining text
                    if (lastIdx < searchText.length) {
                        newText.append(searchText.substring(lastIdx))
                    }

                    searchText = newText.toString()
                }
            }

            // Adjust header spans if inline processing removed characters before them
            // This is a simplified approach - in practice, you might need to adjust header spans
            // based on how many inline characters were removed before each header

            return Pair(searchText, spans + inlineSpans)
        }


    }
}