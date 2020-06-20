package org.bradfordmiller.fuzzyrowmatcher.db

import org.apache.commons.io.FileUtils
import org.apache.ibatis.javassist.NotFoundException
import org.apache.ibatis.jdbc.RuntimeSqlException
import org.apache.ibatis.jdbc.ScriptRunner
import org.bradfordmiller.simplejndiutils.JNDIUtils
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.File
import java.sql.Connection
import java.io.StringReader

class SqlRunner {
    companion object {
        val logger = LoggerFactory.getLogger(SqlRunner::class.java)

        private fun prepScript(conn: Connection, timestamp: String): String {
            val vendor = conn.metaData.databaseProductName.toLowerCase()
            val fileName = "src/main/resources/dbscripts/bootstrap_"
            val formattedSql =
                    when (vendor) {
                        "sqlite" -> FileUtils.readFileToString(File("${fileName}sqlite.sql"), "UTF-8")
                        else -> throw NotFoundException("Database vendor not ")
                    }
            return formattedSql.replace("**TIMESTAMP**", timestamp)
        }

        fun runScript(jndi: String, context: String, timestamp: String): Boolean {
            return try {
                JNDIUtils.getJndiConnection(jndi, context).use { c ->
                    val formattedScript = prepScript(c, timestamp)
                    BufferedReader(StringReader(formattedScript)).use { br ->
                        val sr = ScriptRunner(c)
                        sr.setEscapeProcessing(false)
                        sr.runScript(br)
                    }
                }
                true
            } catch(rse: RuntimeSqlException) {
                logger.error("Error running sql bootstrap script: ${rse.message}")
                false
            }
        }
    }
}