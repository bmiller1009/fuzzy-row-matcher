/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package org.bradfordmiller.fuzzyrowmatcher

import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.math3.stat.descriptive.moment.Mean
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation
import org.apache.commons.math3.stat.descriptive.rank.Median
import org.apache.commons.math3.stat.descriptive.rank.Percentile
import org.bradfordmiller.fuzzyrowmatcher.algos.AlgoResult
import org.bradfordmiller.fuzzyrowmatcher.algos.AlgoType
import org.bradfordmiller.fuzzyrowmatcher.utils.Strings
import org.bradfordmiller.fuzzyrowmatcher.config.Config
import org.bradfordmiller.fuzzyrowmatcher.consumer.DBConsumer
import org.bradfordmiller.fuzzyrowmatcher.db.*
import org.bradfordmiller.simplejndiutils.JNDIUtils
import org.bradfordmiller.sqlutils.SqlUtils
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.RuntimeException
import java.sql.ResultSet
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * stats for each algorithm in a fuzzy matcher run
 *
 * @property min the smallest value in the collection
 * @property firstQuartile the first quartile value in the collection
 * @property median the median value in the collection
 * @property thirdQuartile the third quartile value in the collection
 * @property max the max value in the collection
 * @property mean the mean value in the collection
 * @property stddeviation the standard deviation of the collection
 */
data class AlgoStats(val min: Number, val firstQuartile: Double, val median: Double, val thirdQuartile: Double, val max: Number, val mean: Double, val stddeviation: Double)

/**
 *  The summary report of a fuzzy matching operation
 *
 *  @property rowCount number of rows traversed on the source data
 *  @property comparisonCount number of comparisons performed during the run
 *  @property matchCount number of "fuzzy matches" found during the run
 *  @property duplicateCount number of duplicate values found during the run
 *  @property algos a key value pair of each algorithm [AlgoType] and the corresponding stats [AlgoStats]
 *  @property targetTimeStamp the timestamp used on the target JDBC for suffixing all of the tables during the run
 */
data class FuzzyRowMatcherRpt(val rowCount: Long, val comparisonCount: Long, val matchCount: Long, val duplicateCount: Long, val algos: Map<AlgoType, AlgoStats>, val targetTimeStamp: String?)

/**
 * runs the fuzzy match process, calculates the statistics and publishes a report [FuzzyRowMatcherRpt]
 *
 * @property config [Config] builder object used to define the fuzzy match settings
 * @property producerQueue data is published here for persistence to the target database if one is defined
 * @property statsQueue statistics are published here for consumption by the main fuzzy match thread
 * @property timestamp the timestamp used on the target JDBC for suffixing all of the tables during the run
 */
class FuzzyRowMatcherProducer(
   private val config: Config,
   private val producerQueue: ArrayBlockingQueue<DbPayload>?,
   private val statsQueue: ArrayBlockingQueue<FuzzyRowMatcherRpt>,
   private val timestamp: String?
   ): Runnable {

    companion object {
        val logger = LoggerFactory.getLogger(FuzzyRowMatcherProducer::class.java)
    }

    /**
     * runs the fuzzy match process
     */
    override fun run() {
        val ds = JNDIUtils.getDataSource(config.sourceJndi.jndiName, config.sourceJndi.context).left
        val hashColumns = config.sourceJndi.hashKeys
        val sql = config.sourceJndi.sql
        val algoSet = config.algoSet
        val stringLenPct = config.strLenDeltaPct
        val algoCount = config.algoSet.size
        val aggregateResults = config.aggregateScoreResults
        val ignoreDupes = config.ignoreDupes
        val commitSize = config.dbCommitSize
        val targetJndi = config.targetJndi
        val persistData = targetJndi != null

        val jsonRecords = mutableListOf<JsonRecord>()
        val scoreRecords = mutableListOf<ScoreRecord>()
        val bitVectorList = mutableListOf<List<AlgoResult>>()

        val algoResults = mutableMapOf<AlgoType, MutableList<Number>>(
                AlgoType.FuzzySimilarity to mutableListOf(),
                AlgoType.LevenshteinDistance to mutableListOf(),
                AlgoType.HammingDistance to mutableListOf(),
                AlgoType.JaccardDistance to mutableListOf(),
                AlgoType.CosineDistance to mutableListOf(),
                AlgoType.JaroDistance to mutableListOf()
        )

        val standardDeviation = StandardDeviation()

        var comparisonCount = 0L
        var duplicates = 0L
        var scoreCount = 0L
        var rowCount = 1L

        fun loadRecords(jsonRecords: MutableList<JsonRecord>, scoreRecords: MutableList<ScoreRecord>) {
            val copyJson = mutableListOf<JsonRecord>()
            val copyScores = mutableListOf<ScoreRecord>()

            copyJson.addAll(jsonRecords)
            copyScores.addAll(scoreRecords)

            val dbPayload = DbPayload(copyJson, copyScores)

            producerQueue?.put(dbPayload)

            jsonRecords.clear()
            scoreRecords.clear()
        }

        logger.info("Beginning fuzzy matching process...")

        JNDIUtils.getConnection(ds).use {conn ->
            conn.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)!!.use { stmt ->
                stmt.executeQuery().use { rs ->
                    var rowIndex = 1
                    var firstPass = true
                    val rsmd = rs.metaData
                    val rsColumns = SqlUtils.getColumnsFromRs(rsmd)
                    while(rs.next()) {
                        val currentRowData = SqlUtils.stringifyRow(rs, hashColumns)
                        val currentRowHash = DigestUtils.md5Hex(currentRowData).toUpperCase()
                        val currentRsMap = SqlUtils.getMapFromRs(rs, rsColumns)
                        val jsonRecordCurrent = JsonRecord(currentRowHash, JSONObject(currentRsMap).toString())

                        if (firstPass)
                            jsonRecords.add(jsonRecordCurrent)

                        rowCount += 1
                        while (rs.next()) {

                            val rowData = SqlUtils.stringifyRow(rs, hashColumns)
                            val rowHash = DigestUtils.md5Hex(rowData).toUpperCase()
                            val rowRsMap = SqlUtils.getMapFromRs(rs, rsColumns)
                            val jsonRecordRow = JsonRecord(rowHash, JSONObject(rowRsMap).toString())

                            if (ignoreDupes && currentRowHash == rowHash) {
                                //Duplicate row found, skip everything else
                                duplicates += 1
                                logger.trace("Duplicate found: $currentRowData is identical to $rowData. Skipping comparison")
                                continue
                            }
                            //First check if the row qualifies based on the number of characters in each string
                            if (!Strings.checkStrLen(rowData, currentRowData, stringLenPct)) {
                                logger.trace("String $rowData with length ${rowData.length} will not be checked against $currentRowData with length ${currentRowData.length}")
                                continue
                            }

                            if (firstPass)
                                jsonRecords.add(jsonRecordRow)

                            val bitVector =
                                    algoSet.map { algo ->
                                        val score = algo.applyAlgo(rowData, currentRowData)
                                        AlgoResult(algo.algoType, algo.qualifyThreshold(score), score, currentRowData, rowData)
                                    }
                            //Now determine if this match qualifies
                            val qualifies =
                                    if (aggregateResults) {
                                        bitVector.none { bv -> !bv.qualifies }
                                    } else {
                                        bitVector.any { bv -> bv.qualifies }
                                    }
                            //Publish records to queue
                            if (qualifies) {
                                val scores = bitVector.map { bv -> bv.algoType to bv.score }.toMap()
                                scoreCount += 1
                                val sr = ScoreRecord(UUID.randomUUID().toString(), currentRowHash, rowHash, scores)
                                scoreRecords.add(sr)
                            }

                            bitVectorList += bitVector

                            comparisonCount += algoCount
                            if (persistData && (jsonRecords.size % commitSize == 0L)) {
                                loadRecords(jsonRecords, scoreRecords)
                            }
                        }
                        firstPass = false
                        rowIndex += 1
                        rowCount = rowIndex.toLong()

                        rs.absolute(rowIndex)
                    }
                    if(persistData)
                        loadRecords(jsonRecords, scoreRecords)

                    logger.info("Fuzzy match is complete. $comparisonCount comparisons calculated and $scoreCount successful matches. $duplicates times duplicate values were detected.")
                }
            }
        }
        //Give an empty array to the producer queue to indicate the run is over
        loadRecords(jsonRecords, scoreRecords)

        logger.info("Calculating Statistics for the run.")

        bitVectorList.flatten().forEach {bv ->
            algoResults[bv.algoType]?.add(bv.score)
        }

        val algoStats = algoResults.filter{it.value.size > 0}.map { ar ->
            val doubleList = ar.value.map { it.toDouble() }.toDoubleArray()
            ar.key to AlgoStats(
                    doubleList.min() as Number,
                    Percentile().evaluate(doubleList, 25.0),
                    Median().evaluate(doubleList),
                    Percentile().evaluate(doubleList, 75.0),
                    doubleList.max() as Number,
                    Mean().evaluate(doubleList),
                    standardDeviation.evaluate(doubleList)
            )
        }.toMap()

        logger.info("Calculating Statistics complete.")

        val matchReport = FuzzyRowMatcherRpt(rowCount, comparisonCount, scoreCount, duplicates, algoStats, timestamp)

        statsQueue.put(matchReport)
    }
}

/**
 * launches and coordinates all fuzzy matching components
 *
 * @property config - [Config] builder for all fuzzy match settings for a run
 */
class FuzzyRowMatcher(private val config: Config) {

    companion object {
        val logger: Logger = LoggerFactory.getLogger(FuzzyRowMatcher::class.java)
    }

    /**
     * kicks off the fuzzy match components
     *
     * returns a [FuzzyRowMatcherRpt] when the run is complete
     */
    fun fuzzyMatch(): FuzzyRowMatcherRpt {

        val statsQueue = ArrayBlockingQueue<FuzzyRowMatcherRpt>(100)
        val persistData = config.targetJndi != null

        var producerQueue: ArrayBlockingQueue<DbPayload>? = null
        var threadCount = 1
        var streamComplete = false
        var timestamp: String? = null

        lateinit var fuzzyRowMatcherRpt: FuzzyRowMatcherRpt
        lateinit var dbConsumer: DBConsumer

        if(config.targetJndi != null) {

            timestamp = (System.currentTimeMillis() / 1000).toString()

            threadCount += 1
            producerQueue = ArrayBlockingQueue(100)
            dbConsumer = DBConsumer(producerQueue, config.targetJndi, timestamp)

            //Prep table
            config.targetJndi.let { tj ->
                logger.info("Beginning table creation....")
                val status = SqlRunner.runScript(tj.jndiName, tj.context, timestamp)
                logger.info("Tables successfully created")
                if (!status)
                    throw RuntimeException("Failed to build database tables")
            }
        }

        val fuzzyRowMatcherProducer = FuzzyRowMatcherProducer(config, producerQueue, statsQueue, timestamp)

        val executorService = Executors.newFixedThreadPool(threadCount)
        executorService.execute(fuzzyRowMatcherProducer)
        logger.info("Producer thread started")

        if(persistData) {
            executorService.execute(dbConsumer)
            logger.info("Database consumer thread started")
        }

        while(!streamComplete) {
            fuzzyRowMatcherRpt = statsQueue.take()
            streamComplete = true
        }

        executorService.shutdown()

        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow()
            }
        } catch(iex:InterruptedException) {
            executorService.shutdownNow()
            Thread.currentThread().interrupt()
            logger.error(iex.message)
            throw iex
        }

        logger.info("All consuming services are complete....shutting down.")

        return fuzzyRowMatcherRpt
    }
}