package org.bradfordmiller.fuzzyrowmatcher.db

import org.bradfordmiller.fuzzyrowmatcher.algos.AlgoType
import org.bradfordmiller.fuzzyrowmatcher.config.TargetJndi
import org.bradfordmiller.simplejndiutils.JNDIUtils
import java.sql.SQLException
import org.slf4j.LoggerFactory

class SqlPersistor {
    companion object {

        val logger = LoggerFactory.getLogger(SqlPersistor::class.java)

        fun writeRecords(payload: List<DbPayload>, timestamp: String, tj: TargetJndi): Boolean {

            val jsonInsert = "INSERT INTO json_data_$timestamp VALUES (?,?)"
            val scoreInsert = "INSERT INTO scores_$timestamp VALUES (?,?,?,?,?,?,?,?,?)"

            JNDIUtils.getJndiConnection(tj.jndiName, tj.context).use {conn ->
                conn.autoCommit = false
                try {
                    val pstJson = conn.prepareStatement(jsonInsert)
                    val pstScores = conn.prepareStatement(scoreInsert)

                    payload.forEach { p ->
                        pstJson.setLong(1, p.compareJsonRecord.id)
                        pstJson.setString(2, p.compareJsonRecord.jsonData)
                        pstJson.addBatch()
                        pstJson.setLong(1, p.currentJsonRecord.id)
                        pstJson.setString(2, p.currentJsonRecord.jsonData)
                        pstJson.addBatch()
                        p.scoreRecord?.let { sr ->
                            pstScores.setLong(1, sr.id)
                            pstScores.setLong(2, p.compareJsonRecord.id)
                            pstScores.setLong(3, p.currentJsonRecord.id)
                            var algoCount = 1
                            p.scoreRecord.scores.forEach { me ->
                                when (me.key) {
                                    AlgoType.JaroDistance -> pstScores.setDouble(3 + algoCount, me.value.toDouble())
                                    AlgoType.LevenshteinDistance -> pstScores.setInt(3 + algoCount, me.value.toInt())
                                    AlgoType.HammingDistance -> pstScores.setInt(3 + algoCount, me.value.toInt())
                                    AlgoType.JaccardDistance -> pstScores.setDouble(3 + algoCount, me.value.toDouble())
                                    AlgoType.CosineDistance -> pstScores.setDouble(3 + algoCount, me.value.toDouble())
                                    AlgoType.FuzzySimilarity -> pstScores.setInt(3 + algoCount, me.value.toInt())
                                }
                                algoCount += 1
                            }
                            pstScores.addBatch()
                        }
                    }
                    try {
                        pstJson.executeBatch()
                        pstScores.executeBatch()
                    } catch (sqlEx: SQLException) {
                        logger.error("Error committing batch: ${sqlEx.message}")
                        throw sqlEx
                    }
                    conn.commit()
                }  catch (sqlEx: SQLException) {
                    logger.error("Error while inserting data: ${sqlEx.message}")
                    conn.rollback()
                    throw sqlEx
                }
            }
            return true;
        }
    }
}