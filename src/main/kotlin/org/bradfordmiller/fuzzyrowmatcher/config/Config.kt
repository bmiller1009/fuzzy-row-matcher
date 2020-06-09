package org.bradfordmiller.fuzzyrowmatcher.config

import org.apache.commons.lang.NullArgumentException
import org.apache.commons.text.similarity.JaroWinklerDistance
import org.apache.commons.text.similarity.LevenshteinDistance
import org.bradfordmiller.fuzzyrowmatcher.algos.*
import org.slf4j.LoggerFactory

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
    val jndiName: String,
    val context: String,
    val tableQuery: String,
    val hashKeys: MutableSet<String> = mutableSetOf()
) {
    val sql by lazy {
        if (tableQuery.startsWith("SELECT", true)) {
            tableQuery
        } else {
            "SELECT * FROM ${tableQuery}"
        }
    }
}

class Config private constructor(
    val sourceJndi: SourceJndi,
    val strLenDeltaPct: Double,
    val aggregateScoreResults: Boolean,
    val ignoreDupes: Boolean,
    val algoSet: HashSet<Algo<Number>>
) {

    data class ConfigBuilder(
        private var sourceJndi: SourceJndi? = null,
        private var strLenDeltaPct: Double? = null,
        private var aggregateScoreResults: Boolean? = null,
        private var ignoreDupes: Boolean? = null,
        private var jaroDistance: Algo<Number>? = null,
        private var fuzzyScore: Algo<Number>? = null,
        private var levenshteinDistance: Algo<Number>? = null
    ) {
        companion object {
            private val logger = LoggerFactory.getLogger(ConfigBuilder::class.java)
        }

        val algoSet = HashSet<Algo<Number>>()

        private fun addAlgo(algo: Algo<Number>?) {
            algo?.let {al ->
                algoSet.add(al)
            }
        }

        fun sourceJndi(sourceJndi: SourceJndi) = apply {this.sourceJndi = sourceJndi}
        fun applyJaroDistance(threshold: Double) = apply {this.jaroDistance = JaroDistanceAlgo(threshold) as Algo<Number> }
        fun applyLevenshtein(threshold: Int) = apply {this.levenshteinDistance = LevenshteinDistanceAlgo(threshold) as Algo<Number>}
        fun applyFuzzyScore(threshold: Int) = apply {this.fuzzyScore = FuzzyScoreSimilarAlgo(threshold) as Algo<Number> }
        fun strLenDeltaPct(strLenDeltaPct: Double) = apply {this.strLenDeltaPct = strLenDeltaPct}
        fun aggregateScoreResults(aggregateScoreResults: Boolean) = apply{this.aggregateScoreResults = aggregateScoreResults}
        fun ignoreDupes(ignoreDupes: Boolean) = apply{this.ignoreDupes = ignoreDupes}

        fun build(): Config {
            val sourceJndi = sourceJndi ?: throw NullArgumentException("Source JNDI must be set")
            addAlgo(jaroDistance)
            addAlgo(fuzzyScore)
            addAlgo(levenshteinDistance)
            val strLenDeltaPct = strLenDeltaPct ?: 50.0
            val aggregateScoreResults = aggregateScoreResults ?: false
            val ignoreDupes = ignoreDupes ?: false
            val config = Config(sourceJndi, strLenDeltaPct, aggregateScoreResults, ignoreDupes, algoSet)
            logger.trace("Built config object $config")
            return config
        }
    }
}