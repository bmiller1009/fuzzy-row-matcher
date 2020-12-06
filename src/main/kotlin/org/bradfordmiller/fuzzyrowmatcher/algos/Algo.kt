package org.bradfordmiller.fuzzyrowmatcher.algos

import org.apache.commons.text.similarity.*
import org.slf4j.LoggerFactory
import java.util.*

enum class AlgoType {
    JaroDistance,
    LevenshteinDistance,
    HammingDistance,
    JaccardDistance,
    CosineDistance,
    FuzzySimilarity
}

data class AlgoResult(val algoType: AlgoType, val qualifies: Boolean, val score: Number, val compareRow: String, val currentRow: String) {
    override fun toString(): String {
        return "The result of the algorithm ${algoType.name} had a score of $score for string {{{$compareRow}}} and {{{$currentRow}}} which resulted which had a qualification result of $qualifies"
    }
}

abstract class Algo<T: Number>(internal val threshold: T, val algoType: AlgoType) {
    companion object {
        val logger = LoggerFactory.getLogger(Algo::class.java)
    }
    abstract fun applyAlgo(compareRow: String, currentRow: String): T
    abstract fun qualifyThreshold(incomingThreshold: T): Boolean
}
class JaroDistanceAlgo(threshold: Double): Algo<Double>(threshold, AlgoType.JaroDistance) {
    private val jaroWinkler by lazy {JaroWinklerDistance()}
    override fun applyAlgo(compareRow: String, currentRow: String): Double {
        return (jaroWinkler.apply(compareRow, currentRow) * 100)
    }
    override fun qualifyThreshold(incomingThreshold: Double): Boolean {
        return incomingThreshold >= threshold
    }
}
class LevenshteinDistanceAlgo(threshold: Int): Algo<Int>(threshold, AlgoType.LevenshteinDistance) {
    private val levenshteinDistance by lazy {LevenshteinDistance()}
    override fun applyAlgo(compareRow: String, currentRow: String): Int {
        return levenshteinDistance.apply(compareRow, currentRow)
    }
    override fun qualifyThreshold(incomingThreshold: Int): Boolean {
        return threshold >= incomingThreshold
    }
}
class HammingDistanceAlgo(threshold: Int): Algo<Int>(threshold, AlgoType.HammingDistance) {
    private val hammingDistance by lazy {HammingDistance()}
    override fun applyAlgo(compareRow: String, currentRow: String): Int {
        val compareRowLen = compareRow.length
        val currentRowLen = currentRow.length
        if(compareRowLen == currentRowLen) {
            val differingLength = hammingDistance.apply(compareRow, currentRow)
            return compareRowLen - differingLength
        } else {
            logger.trace("Hamming Distance algorithm requires strings to be of same length.")
            return -1
        }
    }
    override fun qualifyThreshold(incomingThreshold: Int): Boolean {
        return if(incomingThreshold == -1) {
            false
        } else {
            threshold <= incomingThreshold
        }
    }
}
class JaccardDistanceAlgo(threshold: Double): Algo<Double>(threshold, AlgoType.JaccardDistance) {
    private val jaccardDistance by lazy {JaccardDistance()}
    override fun applyAlgo(compareRow: String, currentRow: String): Double {
        return jaccardDistance.apply(compareRow, currentRow)
    }
    override fun qualifyThreshold(incomingThreshold: Double): Boolean {
        return threshold >= incomingThreshold
    }
}
class CosineDistanceAlgo(threshold: Double): Algo<Double>(threshold, AlgoType.CosineDistance) {
    private val cosineDistance by lazy {CosineDistance()}
    override fun applyAlgo(compareRow: String, currentRow: String): Double {
        return (cosineDistance.apply(compareRow, currentRow) * 100)
    }
    override fun qualifyThreshold(incomingThreshold: Double): Boolean {
        return threshold >= incomingThreshold
    }
}
class FuzzyScoreSimilarAlgo(threshold: Int, locale: Locale = Locale.getDefault()): Algo<Int>(threshold, AlgoType.FuzzySimilarity) {
    private val fuzzyScore by lazy {FuzzyScore(locale)}
    override fun applyAlgo(compareRow: String, currentRow: String): Int {
        return fuzzyScore.fuzzyScore(compareRow, currentRow)
    }
    override fun qualifyThreshold(incomingThreshold: Int): Boolean {
        return incomingThreshold >= threshold
    }
}