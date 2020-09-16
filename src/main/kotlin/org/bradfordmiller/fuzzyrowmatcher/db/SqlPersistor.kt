package org.bradfordmiller.fuzzyrowmatcher.db

import org.bradfordmiller.fuzzyrowmatcher.algos.AlgoType
import org.bradfordmiller.fuzzyrowmatcher.config.TargetJndi
import org.bradfordmiller.simplejndiutils.JNDIUtils
import java.sql.SQLException
import org.slf4j.LoggerFactory
import java.sql.Statement
import java.sql.Types

class SqlPersistor(timestamp: String) {

    companion object {
        val logger = LoggerFactory.getLogger(SqlPersistor::class.java)
    }

    val params = (1..9).map{"?"}.joinToString()
    val jsonInsert = "INSERT INTO json_data_$timestamp VALUES (?,?)"
    val scoreInsert = "INSERT INTO scores_$timestamp VALUES ($params)"
    val algos = AlgoType.values()

    fun writeRecords(payload: DbPayload, tj: TargetJndi) {
        JNDIUtils.getJndiConnection(tj.jndiName, tj.context).use {conn ->
            conn.autoCommit = false
            try {
                val pstJson = conn.prepareStatement(jsonInsert, Statement.RETURN_GENERATED_KEYS)
                val pstScores = conn.prepareStatement(scoreInsert)

                payload.jsonRecords.map {jr ->
                    pstJson.setString(1, jr.id)
                    pstJson.setString(2, jr.jsonData)
                    pstJson.addBatch()
                }

                payload.scores.forEach { s ->
                    s?.let { sr ->
                        pstScores.setString(1, sr.id)
                        pstScores.setString(2, sr.compareRecordId)
                        pstScores.setString(3, sr.currentRecordId)
                        algos.forEach { al ->
                            if (al == AlgoType.JaroDistance) {
                                if (sr.scores.containsKey(al)) {
                                    pstScores.setDouble(4, sr.scores[al]!!.toDouble())
                                } else {
                                    pstScores.setNull(4, Types.DOUBLE)
                                }
                            } else if (al == AlgoType.LevenshteinDistance) {
                                if (sr.scores.containsKey(al)) {
                                    pstScores.setInt(5, sr.scores[al]!!.toInt())
                                } else {
                                    pstScores.setNull(5, Types.INTEGER)
                                }
                            } else if (al == AlgoType.HammingDistance) {
                                if (sr.scores.containsKey(al)) {
                                    pstScores.setInt(6, sr.scores[al]!!.toInt())
                                } else {
                                    pstScores.setNull(6, Types.INTEGER)
                                }
                            } else if (al == AlgoType.JaccardDistance) {
                                if (sr.scores.containsKey(al)) {
                                    pstScores.setDouble(7, sr.scores[al]!!.toDouble())
                                } else {
                                    pstScores.setNull(7, Types.DOUBLE)
                                }
                            } else if (al == AlgoType.CosineDistance) {
                                if (sr.scores.containsKey(al)) {
                                    pstScores.setDouble(8, sr.scores[al]!!.toDouble())
                                } else {
                                    pstScores.setNull(8, Types.DOUBLE)
                                }
                            } else {
                                if (sr.scores.containsKey(al)) {
                                    pstScores.setInt(9, sr.scores[al]!!.toInt())
                                } else {
                                    pstScores.setNull(9, Types.INTEGER)
                                }
                            }
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
    }
}