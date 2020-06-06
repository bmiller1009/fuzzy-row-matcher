/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package org.bradfordmiller.fuzzyrowmatcher

import org.apache.commons.text.similarity.JaroWinklerDistance
import org.apache.commons.text.similarity.JaroWinklerSimilarity
import org.bradfordmiller.fuzzyrowmatcher.config.SourceJndi
import org.bradfordmiller.simplejndiutils.JNDIUtils
import org.bradfordmiller.sqlutils.SqlUtils
import org.slf4j.LoggerFactory
import java.sql.ResultSet

class FuzzyRowMatcher {

    companion object {
        val logger = LoggerFactory.getLogger(FuzzyRowMatcher::class.java)
    }

    fun fuzzyMatch(): Boolean {

        val stringDiffPct = 50.0
        val hashColumns = mutableSetOf("street","city", "state", "zip", "price")
        val sourceJndi = SourceJndi("RealEstateIn", "default_ds","Sacramentorealestatetransactions", hashColumns)

        val sql by lazy {
            if (sourceJndi.tableQuery.startsWith("SELECT", true)) {
                sourceJndi.tableQuery
            } else {
                "SELECT * FROM ${sourceJndi.tableQuery}"
            }
        }

        val ds = JNDIUtils.getDataSource(sourceJndi.jndiName, sourceJndi.context).left
        val jaroDist = JaroWinklerDistance()
        val jaroSim = JaroWinklerSimilarity()

        var comparisonCount = 0

        JNDIUtils.getConnection(ds).use {conn ->
            conn.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)!!.use { stmt ->
                stmt.executeQuery().use { rs ->
                    var rowIndex = 1
                    while(rs.next()) {
                        var currentRowData = SqlUtils.stringifyRow(rs, hashColumns)
                        while (rs.next()) {
                            var rowData = SqlUtils.stringifyRow(rs, hashColumns)
                            var jaroDistPct = jaroDist.apply(currentRowData, rowData)
                            comparisonCount += 1
                            logger.info("$currentRowData was compared with $rowData and the score was $jaroDistPct for Jaro Distance")
                            //var jaroSimPct = jaroSim.apply(currentRowData, rowData)
                            //logger.info("$currentRowData was compared with $rowData and the score was $jaroDistPct for Jaro Distance and $jaroSimPct for Jaro Similar")
                        }
                        rowIndex += 1
                        logger.info("Cursor moved to row index $rowIndex")
                        rs.absolute(rowIndex)
                    }
                    logger.info("Fuzzy match is complete. $comparisonCount comparisons calculated")
                }
            }
        }

        return true
    }


}
