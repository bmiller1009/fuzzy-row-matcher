package org.bradfordmiller.fuzzyrowmatcher

import org.apache.commons.codec.digest.DigestUtils
import org.bradfordmiller.fuzzyrowmatcher.algos.AlgoType
import org.bradfordmiller.fuzzyrowmatcher.config.Config
import org.bradfordmiller.fuzzyrowmatcher.config.SourceJndi
import org.bradfordmiller.fuzzyrowmatcher.config.TargetJndi
import org.bradfordmiller.fuzzyrowmatcher.db.SqlRunner
import org.bradfordmiller.simplejndiutils.JNDIUtils
import org.bradfordmiller.sqlutils.SqlUtils
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FuzzyRowMatcherTest {
    private fun getFirstRowFromTarget(targetJndi: TargetJndi, tableName: String): Array<String> {
        JNDIUtils.getJndiConnection(targetJndi.jndiName, targetJndi.context).use { conn ->
            val sql = "SELECT * FROM $tableName LIMIT 1"
            conn.prepareStatement(sql).use {stmt ->
                stmt.executeQuery().use {rs ->
                    val cols = SqlUtils.getColumnsFromRs(rs.metaData)
                    rs.next()
                    return cols.map {c ->
                        rs.getString(c.value)
                    }.toTypedArray()
                }
            }
        }
    }
    private fun getTargetCount(targetJndi: TargetJndi, tableName: String): Long {
        return JNDIUtils.getJndiConnection(targetJndi.jndiName, targetJndi.context).use {conn ->
            val sql = "SELECT COUNT(1) FROM $tableName"
            conn.prepareStatement(sql).use {stmt ->
                stmt.executeQuery().use {rs ->
                    rs.next()
                    rs.getLong(1)
                }
            }
        }
    }
    @Test
    fun simpleFuzzyMatch() {

        val algoMap = mutableMapOf(
                AlgoType.FuzzySimilarity to AlgoStats(0.0, 1.0, 1.0, 2.0, 136.0, 1.6852211088184803, 1.754674502017007),
                AlgoType.LevenshteinDistance to AlgoStats(1.0, 25.0, 29.0, 32.0, 49.0, 27.77592576575672, 5.901593648904725),
                AlgoType.HammingDistance to AlgoStats(-1.0, -1.0, -1.0, -1.0, 55.0, 0.3077195540345852, 5.210791761497745),
                AlgoType.JaccardDistance to AlgoStats(0.0, 0.3076923076923077, 0.37037037037037035, 0.43333333333333335, 0.7272727272727273, 0.3686462303617574, 0.08994667468014585),
                AlgoType.CosineDistance to AlgoStats(14.285714285714302, 73.27387580875757, 85.71428571428572, 87.40118423302576, 91.99359230974564, 80.69712241304543, 9.586290783210107),
                AlgoType.JaroDistance to AlgoStats(46.7063492063492, 62.27152381256181, 65.45815295815297, 68.90715211499942, 99.31818181818183, 65.87141433927935, 5.319269347946669)
        )

        val expectedReport = FuzzyRowMatcherRpt(987,2913588, 485598, 6, algoMap, null)
        val targetJndi = TargetJndi("SqlLiteTest", "default_ds")

        val hashColumns = mutableSetOf("street","city", "state", "zip", "price")
        val sourceJndi =
            SourceJndi(
                    "RealEstateIn",
                    "default_ds",
                    "SELECT * FROM Sacramentorealestatetransactions", //LIMIT 10",
                    hashColumns
            )
        //Add defaults for each algos
        val config =
            Config.ConfigBuilder()
                .sourceJndi(sourceJndi)
                .targetJndi(targetJndi)
                .applyJaroDistance(98.0)
                .applyLevenshtein(5)
                .applyFuzzyScore(90)
                .applyCosineDistance(30.0)
                .applyHammingDistance(15)
                .applyJaccardDistance(90.0)
                .strLenDeltaPct(50.0)
                .aggregateScoreResults(false)
                .ignoreDupes(true)
                .build()

        val frm = FuzzyRowMatcher(config)
        val result = frm.fuzzyMatch()
        val viewName = "final_scores_${result.targetTimeStamp}"
        val targetCount = getTargetCount(targetJndi, viewName)
        val rowSample = getFirstRowFromTarget(targetJndi, viewName).joinToString()
        val rowHash = DigestUtils.md5Hex(rowSample).toUpperCase()

        assert(result.algos == expectedReport.algos)
        assert(result.comparisonCount == expectedReport.comparisonCount)
        assert(result.duplicateCount == expectedReport.duplicateCount)
        assert(result.matchCount == expectedReport.matchCount)
        assert(result.rowCount == expectedReport.rowCount)

        assert(targetCount == 485598L)
        assert(rowHash == "15C4E3464AF97E1D136D535DDC82B777")
    }

    @Test
    fun testTableBootstrap() {
        val tj = TargetJndi("SqlLiteTest", "default_ds")
        val timestamp = (System.currentTimeMillis() / 1000).toString()
        val success = SqlRunner.runScript(tj.jndiName, tj.context, timestamp)
        assert(success)
    }

    @Test
    fun testSourceAndSingleAlgo() {
        val sourceJndi =
                SourceJndi(
                        "RealEstateIn",
                        "default_ds",
                        "Sacramentorealestatetransactions"
                )

        val algoMap = mutableMapOf(
               AlgoType.JaroDistance to AlgoStats(63.25954223397656, 73.01287149712347, 74.83295979588313, 76.85458437015583, 100.0, 75.04290962954407, 3.0175386794224877)
        )

        val expectedReport = FuzzyRowMatcherRpt(987,485606, 7, 0, algoMap, null)

        //Add defaults for each algos
        val config =
                Config.ConfigBuilder()
                        .sourceJndi(sourceJndi)
                        .applyJaroDistance(98.0)
                        .build()

        val frm = FuzzyRowMatcher(config)
        val result = frm.fuzzyMatch()

        assert(result.algos == expectedReport.algos)
        assert(result.comparisonCount == expectedReport.comparisonCount)
        assert(result.duplicateCount == expectedReport.duplicateCount)
        assert(result.matchCount == expectedReport.matchCount)
        assert(result.rowCount == expectedReport.rowCount)
    }

    @Test
    fun testSourceAndMultiAlgoAggregated() {
        val sourceJndi =
                SourceJndi(
                        "RealEstateIn",
                        "default_ds",
                        "Sacramentorealestatetransactions"
                )

        val algoMap = mutableMapOf(
                AlgoType.LevenshteinDistance to AlgoStats(0.0, 43.0, 48.0, 52.0, 80.0, 47.31299860381222, 8.032522633529313),
                AlgoType.JaroDistance to AlgoStats(63.25954223397656, 73.01287149712347, 74.83295979588313, 76.85458437015583, 100.0, 75.04290962954407, 3.0175386794224877)
        )

        val expectedReport = FuzzyRowMatcherRpt(987,971212, 6, 0, algoMap, null)

        //Add defaults for each algos
        val config =
                Config.ConfigBuilder()
                        .sourceJndi(sourceJndi)
                        .applyJaroDistance(98.0)
                        .applyLevenshtein(5)
                        .aggregateScoreResults(true)
                        .build()

        val frm = FuzzyRowMatcher(config)
        val result = frm.fuzzyMatch()

        assert(result.algos == expectedReport.algos)
        assert(result.comparisonCount == expectedReport.comparisonCount)
        assert(result.duplicateCount == expectedReport.duplicateCount)
        assert(result.matchCount == expectedReport.matchCount)
        assert(result.rowCount == expectedReport.rowCount)
    }

    @Test
    fun testSourceAndMultiAlgoNotAggregated() {
        val sourceJndi =
                SourceJndi(
                        "RealEstateIn",
                        "default_ds",
                        "Sacramentorealestatetransactions"
                )

        val algoMap = mutableMapOf(
                AlgoType.LevenshteinDistance to AlgoStats(0.0, 43.0, 48.0, 52.0, 80.0, 47.31299860381222, 8.032522633529313),
                AlgoType.JaroDistance to AlgoStats(63.25954223397656, 73.01287149712347, 74.83295979588313, 76.85458437015583, 100.0, 75.04290962954407, 3.0175386794224877)
        )

        val expectedReport = FuzzyRowMatcherRpt(987,971212, 39, 0, algoMap, null)

        //Add defaults for each algos
        val config =
                Config.ConfigBuilder()
                        .sourceJndi(sourceJndi)
                        .applyJaroDistance(98.0)
                        .applyLevenshtein(5)
                        .aggregateScoreResults(false)
                        .build()

        val frm = FuzzyRowMatcher(config)
        val result = frm.fuzzyMatch()

        assert(result.algos == expectedReport.algos)
        assert(result.comparisonCount == expectedReport.comparisonCount)
        assert(result.duplicateCount == expectedReport.duplicateCount)
        assert(result.matchCount == expectedReport.matchCount)
        assert(result.rowCount == expectedReport.rowCount)
    }

    @Test
    fun testSourceAndMultiAlgoNotAggregatedWithTarget() {
        val sourceJndi =
                SourceJndi(
                        "RealEstateIn",
                        "default_ds",
                        "Sacramentorealestatetransactions"
                )

        val targetJndi = TargetJndi("SqlLiteTest", "default_ds")

        val algoMap = mutableMapOf(
                AlgoType.LevenshteinDistance to AlgoStats(0.0, 43.0, 48.0, 52.0, 80.0, 47.31299860381222, 8.032522633529313),
                AlgoType.JaroDistance to AlgoStats(63.25954223397656, 73.01287149712347, 74.83295979588313, 76.85458437015583, 100.0, 75.04290962954407, 3.0175386794224877)
        )

        val expectedReport = FuzzyRowMatcherRpt(987,971212, 39, 0, algoMap, null)

        //Add defaults for each algos
        val config =
                Config.ConfigBuilder()
                        .sourceJndi(sourceJndi)
                        .targetJndi(targetJndi)
                        .applyJaroDistance(98.0)
                        .applyLevenshtein(5)
                        .aggregateScoreResults(false)
                        .build()

        val frm = FuzzyRowMatcher(config)
        val result = frm.fuzzyMatch()
        val viewName = "final_scores_${result.targetTimeStamp}"
        val targetCount = getTargetCount(targetJndi, viewName)
        val rowSample = getFirstRowFromTarget(targetJndi, viewName).joinToString()
        val rowHash = DigestUtils.md5Hex(rowSample).toUpperCase()

        assert(result.algos == expectedReport.algos)
        assert(result.comparisonCount == expectedReport.comparisonCount)
        assert(result.duplicateCount == expectedReport.duplicateCount)
        assert(result.matchCount == expectedReport.matchCount)
        assert(result.rowCount == expectedReport.rowCount)

        assert(targetCount == 39L)
        assert(rowHash == "CB90E543EE9DDB05DA2A543F0CDCBF1A")
    }
 }