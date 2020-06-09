package org.bradfordmiller.fuzzyrowmatcher.algos

import org.apache.commons.text.similarity.FuzzyScore
import org.apache.commons.text.similarity.JaroWinklerDistance
import org.apache.commons.text.similarity.LevenshteinDistance
import java.util.*

data class AlgoResult(val algoName: String, val qualifies: Boolean, val score: Number, val compareRow: String, val currentRow: String) {
    override fun toString(): String {
        return "The result of the algorithm $algoName had a score of $score for string {{{$compareRow}}} and {{{$currentRow}}} which resulted which had a qualification result of $qualifies"
    }
}

class Strings {
    companion object {
        fun checkStrLen(compareRow: String, currentRow: String, strLenDeltaPct: Double): Boolean {
            val compareLen = compareRow.length
            val currentRow = currentRow.length
            val pct =
                if(compareLen > currentRow) {
                    currentRow.toDouble() / compareLen.toDouble()
                } else {
                    compareLen.toDouble() / currentRow.toDouble()
                }
            return (pct * 100) >= strLenDeltaPct
        }
    }
}
abstract class Algo<T: Number>(internal val threshold: T, val name: String) {
    abstract fun applyAlgo(compareRow: String, currentRow: String): T
    abstract fun qualifyThreshold(incomingThreshold: T): Boolean
}
class JaroDistanceAlgo(threshold: Double): Algo<Double>(threshold, "Jaro Distance") {
    val jaroWinkler by lazy {JaroWinklerDistance()}
    override fun applyAlgo(compareRow: String, currentRow: String): Double {
        return (jaroWinkler.apply(compareRow, currentRow) * 100)
    }
    override fun qualifyThreshold(incomingThreshold: Double): Boolean {
        return incomingThreshold >= threshold
    }
}
class LevenshteinDistanceAlgo(threshold: Int): Algo<Int>(threshold, "Levenshtein Distance") {
    val levenshteinDistance by lazy {LevenshteinDistance()}
    override fun applyAlgo(compareRow: String, currentRow: String): Int {
        return levenshteinDistance.apply(compareRow, currentRow)
    }
    override fun qualifyThreshold(incomingThreshold: Int): Boolean {
        return threshold >= incomingThreshold
    }
}
class FuzzyScoreSimilarAlgo(threshold: Int, locale: Locale = Locale.getDefault()): Algo<Int>(threshold, "Fuzzy Similarity") {
    val fuzzyScore by lazy {FuzzyScore(locale)}
    override fun applyAlgo(compareRow: String, currentRow: String): Int {
        return fuzzyScore.fuzzyScore(compareRow, currentRow)
    }
    override fun qualifyThreshold(incomingThreshold: Int): Boolean {
        return incomingThreshold >= threshold
    }
}