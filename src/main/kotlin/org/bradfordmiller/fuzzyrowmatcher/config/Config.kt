package org.bradfordmiller.fuzzyrowmatcher.config

import org.apache.commons.lang.NullArgumentException
import org.bradfordmiller.fuzzyrowmatcher.algos.*
import org.slf4j.LoggerFactory

/**
 * Defines output information for csv target data based on the [jndiName] name and jndi [context]
 */
interface SimpleJndi {
    val jndiName: String
    val context: String
}
/**
 * A source jndi entity
 *
 * @property jndiName the jndi name defined in the simple-jndi properties file
 * @property context the context name for the jndi name, which basically maps to a properties file of the same name
 * IE if context = "test" then there should be a corresponding test.properties file present in the org.osjava.sj.root
 * defined directory in jndi.properties.  In the above example, if the context = "test" and org.osjava.sj.root =
 * src/main/resources/jndi then the jndi name will be searched for in src/main/resources/jndi/test.properties
 * @property tableQuery can either be a table (which 'SELECT *' will be run against) or a specific 'SELECT' SQL query
 * @property hashKeys a list of column names which will be used to hash the values returned by [tableQuery]
 */
data class SourceJndi(
        override val jndiName: String,
        override val context: String,
        val tableQuery: String,
        val hashKeys: MutableSet<String> = mutableSetOf()
): SimpleJndi {
    val sql by lazy {
        if (tableQuery.startsWith("SELECT", true)) {
            tableQuery
        } else {
            "SELECT * FROM ${tableQuery}"
        }
    }
}

/**
 * Defines output information for csv target data based on the [jndiName] name and jndi [context]
 */
data class TargetJndi(
    override val jndiName: String,
    override val context: String
): SimpleJndi

/**
 * Configuration for a fuzzy match process
 *
 * @property sourceJndi a [SourceJndi] object
 * @property targetJndi a [TargetJndi] object
 * @property strLenDeltaPct determines whether two strings of differing lengths will be compared by setting a max difference between the string lengths
 * @property aggregateScoreResults determines whether qualfication of two strings is based on ALL algorithms passing threshold or ANY. If set to true, then ALL algorithms must pass before qualification
 * @property ignoreDupes determines whether exact duplicate strings (based off of MD5 hashes) are scored or if they are skipped
 * @property dbCommitSize defines how often data is persisted to the output if a [targetJndi] is defined.  The default is a commitSize of 500, which means every time 500 rows are collected they will be persisted to the [targetJndi]
 * @property algoSet the list of algorithms of type [Algo] that will be run against the data set
 */
class Config private constructor(
    val sourceJndi: SourceJndi,
    val targetJndi: TargetJndi?,
    val strLenDeltaPct: Double,
    val aggregateScoreResults: Boolean,
    val ignoreDupes: Boolean,
    val dbCommitSize: Long,
    val algoSet: HashSet<Algo<Number>>
) {

    data class ConfigBuilder(
        private var sourceJndi: SourceJndi? = null,
        private var targetJndi: TargetJndi? = null,
        private var strLenDeltaPct: Double? = null,
        private var aggregateScoreResults: Boolean? = null,
        private var ignoreDupes: Boolean? = null,
        private var dbCommitSize: Long? = null,
        private var jaroDistance: Algo<Number>? = null,
        private var fuzzyScore: Algo<Number>? = null,
        private var levenshteinDistance: Algo<Number>? = null,
        private var cosineDistance: Algo<Number>? = null,
        private var hammingDistance: Algo<Number>? = null,
        private var jaccardDistance: Algo<Number>? = null
    ) {
        companion object {
            private val logger = LoggerFactory.getLogger(ConfigBuilder::class.java)
        }

        /**
         * sets the [sourceJndi] for the builder object
         */
        fun sourceJndi(sourceJndi: SourceJndi) = apply {this.sourceJndi = sourceJndi}

        /**
         * sets the [targetJndi] for the builder object
         */
        fun targetJndi(targetJndi: TargetJndi) = apply {this.targetJndi = targetJndi}

        /**
         * sets the [threshold] value for the Jaro Winkler string distance algorithm
         */
        fun applyJaroDistance(threshold: Double) = apply {this.jaroDistance = JaroDistanceAlgo(threshold) as Algo<Number> }

        /**
         * sets the [threshold] value for the Cosine string distance algorithm
         */
        fun applyCosineDistance(threshold: Double) = apply{this.cosineDistance = CosineDistanceAlgo(threshold) as Algo<Number>}

        /**
         * sets the [threshold] value for the Hamming string distance algorithm
         */
        fun applyHammingDistance(threshold: Int) = apply{this.hammingDistance = HammingDistanceAlgo(threshold) as Algo<Number>}

        /**
         * sets the [threshold] value for the Jaccard string distance algorithm
         */
        fun applyJaccardDistance(threshold: Double) = apply{this.jaccardDistance = JaccardDistanceAlgo(threshold) as Algo<Number>}

        /**
         * sets the [threshold] value for the Levenshtein string distance algorithm
         */
        fun applyLevenshtein(threshold: Int) = apply {this.levenshteinDistance = LevenshteinDistanceAlgo(threshold) as Algo<Number>}

        /**
         * sets the [threshold] value for the Fuzzy Score string distance algorithm
         */
        fun applyFuzzyScore(threshold: Int) = apply {this.fuzzyScore = FuzzyScoreSimilarAlgo(threshold) as Algo<Number> }

        /**
         * sets the [strLenDeltaPct] value which determines how different in length two strings can be before a string distance comparison is ignored
         */
        fun strLenDeltaPct(strLenDeltaPct: Double) = apply {this.strLenDeltaPct = strLenDeltaPct}

        /**
         * sets the [aggregateScoreResults] which determines whether ALL algorithms qualify a string comparison or ANY algorithms qualify
         */
        fun aggregateScoreResults(aggregateScoreResults: Boolean) = apply{this.aggregateScoreResults = aggregateScoreResults}

        /**
         * sets [ignoreDupes] which sets whether the fuzzy match process will score duplicate strings (by MD5 hash) or skip them
         */
        fun ignoreDupes(ignoreDupes: Boolean) = apply{this.ignoreDupes = ignoreDupes}

        /**
         * sets [dbCommitSize] which determines how often rows are commited to the target [targetJndi]
         */
        fun dbCommitSize(dbCommitSize: Long) = apply {this.dbCommitSize = dbCommitSize}

        /**
         * returns [Config] object with builder options set
         */
        fun build(): Config {

            val algoSet = HashSet<Algo<Number>>()

            fun addAlgo(algo: Algo<Number>?) {
                algo?.let {al ->
                    algoSet.add(al)
                }
            }

            val sourceJndi = sourceJndi ?: throw NullArgumentException("Source JNDI must be set")
            addAlgo(jaroDistance)
            addAlgo(fuzzyScore)
            addAlgo(levenshteinDistance)
            addAlgo(cosineDistance)
            addAlgo(hammingDistance)
            addAlgo(jaccardDistance)
            val strLenDeltaPct = strLenDeltaPct ?: 50.0

            val aggregateScoreResults = aggregateScoreResults ?: false
            val ignoreDupes = ignoreDupes ?: false
            val dbCommitSize = dbCommitSize ?: 500
            val config = Config(sourceJndi, targetJndi, strLenDeltaPct, aggregateScoreResults, ignoreDupes, dbCommitSize, algoSet)
            logger.trace("Built config object $config")
            return config
        }
    }
}