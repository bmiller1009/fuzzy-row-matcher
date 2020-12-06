package org.bradfordmiller.fuzzyrowmatcher.algos

import org.apache.commons.text.similarity.*
import org.slf4j.LoggerFactory
import java.util.*

/**
 * enumerated list of all algorithm types supported by Fuzzy Matcher
 *
 * The values are
 * - JaroDistance
 * - LevenshteinDistance
 * - HammingDistance
 * - JaccardDistance
 * - CosineDistance
 * - FuzzySimilarity
 */
enum class AlgoType {
    JaroDistance,
    LevenshteinDistance,
    HammingDistance,
    JaccardDistance,
    CosineDistance,
    FuzzySimilarity
}

/**
 * represents the result of an applied Fuzzy match on two strings
 *
 * @property algoType the enumerated algorithm applied to [compareRow] and [currentRow]
 * @property qualifies whether or not the fuzzy match meets the criteria of the [score] and threshold
 * @property score the score returned by the algorithm [algoType] when applied to [compareRow] and [currentRow]
 * @property compareRow the first string being compared
 * @property currentRow the second string being compared
 */
data class AlgoResult(val algoType: AlgoType, val qualifies: Boolean, val score: Number, val compareRow: String, val currentRow: String) {
    override fun toString(): String {
        return "The result of the algorithm ${algoType.name} had a score of $score for string {{{$compareRow}}} and {{{$currentRow}}} which resulted which had a qualification result of $qualifies"
    }
}

/**
 * Base definition of an algorithm with a threshold
 *
 * @param T - the type of Number being returned by the Fuzzy Matching algorithm defined in [algoType]
 * @property threshold - the threshold score which must be met by the application of the algorithm defined in [algoType]
 */
abstract class Algo<T: Number>(internal val threshold: T, val algoType: AlgoType) {
    companion object {
        val logger = LoggerFactory.getLogger(Algo::class.java)
    }

    /**
     * applies algorithm defined in [algoType]
     *
     * returns the score calculated by the algorithm defined in [algoType]
     */
    abstract fun applyAlgo(compareRow: String, currentRow: String): T

    /**
     * determines whether the score returned after applying the algorithm exceeds the [incomingThreshold]
     *
     * returns a true/false based on whether the threshold is met
     */
    abstract fun qualifyThreshold(incomingThreshold: T): Boolean
}

/**
 * calculates the Jaro string distance algorithm based on the [threshold]
 */
class JaroDistanceAlgo(threshold: Double): Algo<Double>(threshold, AlgoType.JaroDistance) {
    private val jaroWinkler by lazy {JaroWinklerDistance()}

    /**
     * applies the Jaro string distance algorithm on [compareRow] and [currentRow]
     *
     * returns the score calculated by the Jaro string distance
     */
    override fun applyAlgo(compareRow: String, currentRow: String): Double {
        return (jaroWinkler.apply(compareRow, currentRow) * 100)
    }

    /**
     *  determines whether the calculated score meets the [incomingThreshold]
     *
     *  returns the determination of whether the calculation qualifies based on the score calculated and the [incomingThreshold]
     */
    override fun qualifyThreshold(incomingThreshold: Double): Boolean {
        return incomingThreshold >= threshold
    }
}

/**
 * calculates the Levenshtein string distance algorithm based on the [threshold]
 */
class LevenshteinDistanceAlgo(threshold: Int): Algo<Int>(threshold, AlgoType.LevenshteinDistance) {
    private val levenshteinDistance by lazy {LevenshteinDistance()}

    /**
     * applies the Levenshtein string distance algorithm on [compareRow] and [currentRow]
     *
     * returns the score calculated by the Levenshtein string distance
     */
    override fun applyAlgo(compareRow: String, currentRow: String): Int {
        return levenshteinDistance.apply(compareRow, currentRow)
    }

    /**
     *  determines whether the calculated score meets the [incomingThreshold]
     *
     *  returns the determination of whether the calculation qualifies based on the score calculated and the [incomingThreshold]
     */
    override fun qualifyThreshold(incomingThreshold: Int): Boolean {
        return threshold >= incomingThreshold
    }
}

/**
 * calculates the Hamming string distance algorithm based on the [threshold]
 */
class HammingDistanceAlgo(threshold: Int): Algo<Int>(threshold, AlgoType.HammingDistance) {
    private val hammingDistance by lazy {HammingDistance()}

    /**
     * applies the Hamming string distance algorithm on [compareRow] and [currentRow]
     *
     * returns the score calculated by the Hamming string distance
     */
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

    /**
     *  determines whether the calculated score meets the [incomingThreshold]
     *
     *  returns the determination of whether the calculation qualifies based on the score calculated and the [incomingThreshold]
     */
    override fun qualifyThreshold(incomingThreshold: Int): Boolean {
        return if(incomingThreshold == -1) {
            false
        } else {
            threshold <= incomingThreshold
        }
    }
}

/**
 * calculates the Hamming string distance algorithm based on the [threshold]
 */
class JaccardDistanceAlgo(threshold: Double): Algo<Double>(threshold, AlgoType.JaccardDistance) {
    private val jaccardDistance by lazy {JaccardDistance()}

    /**
     * applies the Jaccard string distance algorithm on [compareRow] and [currentRow]
     *
     * returns the score calculated by the Jaccard string distance
     */
    override fun applyAlgo(compareRow: String, currentRow: String): Double {
        return jaccardDistance.apply(compareRow, currentRow)
    }

    /**
     *  determines whether the calculated score meets the [incomingThreshold]
     *
     *  returns the determination of whether the calculation qualifies based on the score calculated and the [incomingThreshold]
     */
    override fun qualifyThreshold(incomingThreshold: Double): Boolean {
        return threshold >= incomingThreshold
    }
}

/**
 * calculates the Cosine string distance algorithm based on the [threshold]
 */
class CosineDistanceAlgo(threshold: Double): Algo<Double>(threshold, AlgoType.CosineDistance) {
    private val cosineDistance by lazy {CosineDistance()}

    /**
     * applies the Cosine string distance algorithm on [compareRow] and [currentRow]
     *
     * returns the score calculated by the Cosine string distance
     */
    override fun applyAlgo(compareRow: String, currentRow: String): Double {
        return (cosineDistance.apply(compareRow, currentRow) * 100)
    }

    /**
     * applies the Cosine string distance algorithm on [compareRow] and [currentRow]
     *
     * returns the score calculated by the Cosine string distance
     */
    override fun qualifyThreshold(incomingThreshold: Double): Boolean {
        return threshold >= incomingThreshold
    }
}

/**
 * calculates the Fuzzy similarity string distance algorithm based on the [threshold]
 */
class FuzzyScoreSimilarAlgo(threshold: Int, locale: Locale = Locale.getDefault()): Algo<Int>(threshold, AlgoType.FuzzySimilarity) {
    private val fuzzyScore by lazy {FuzzyScore(locale)}

    /**
     * applies the Fuzzy similarity string distance algorithm on [compareRow] and [currentRow]
     *
     * returns the score calculated by the Fuzzy similarity string distance
     */
    override fun applyAlgo(compareRow: String, currentRow: String): Int {
        return fuzzyScore.fuzzyScore(compareRow, currentRow)
    }

    /**
     *  determines whether the calculated score meets the [incomingThreshold]
     *
     *  returns the determination of whether the calculation qualifies based on the score calculated and the [incomingThreshold]
     */
    override fun qualifyThreshold(incomingThreshold: Int): Boolean {
        return incomingThreshold >= threshold
    }
}