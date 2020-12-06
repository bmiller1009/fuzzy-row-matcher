package org.bradfordmiller.fuzzyrowmatcher.db

import org.apache.commons.io.IOUtils
import org.apache.ibatis.javassist.NotFoundException
import org.apache.ibatis.jdbc.RuntimeSqlException
import org.apache.ibatis.jdbc.ScriptRunner
import org.bradfordmiller.simplejndiutils.JNDIUtils
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.sql.Connection
import java.io.StringReader
import java.nio.charset.StandardCharsets

/**
 * runs sql scripts which scaffold out the output tables when a target JDBC endpoint is set
 */
class SqlRunner {
    companion object {
        val logger = LoggerFactory.getLogger(SqlRunner::class.java)

        /**
         * prepares the database script for running against the target JDBC endpoint
         *
         * @property conn the database connection the scripts will be run against
         * @property timestamp the suffix string which will all output tables in the target JDBC will be appended with
         *
         * returns a fully runnable SQL script with all tokenized values populated
         */
        private fun prepScript(conn: Connection, timestamp: String): String {
            val vendor = conn.metaData.databaseProductName.toLowerCase()
            val classLoader = this.javaClass.classLoader

            val formattedSql =
                    when (vendor) {
                        "sqlite" -> {
                            val stmt = conn.createStatement()
                            stmt.executeUpdate("PRAGMA foreign_keys = ON;")
                            classLoader.getResourceAsStream("dbscripts/bootstrap_sqlite.sql").use {`is` ->
                                IOUtils.toString(`is`, StandardCharsets.UTF_8.name())
                            }
                        }
                        else -> throw NotFoundException("Database vendor not recognized.")
                    }
            return formattedSql.replace("**TIMESTAMP**", timestamp)
        }

        /**
         *  runs a SQL script against a specific [jndi] and [context]
         *
         *  @property timestamp suffix value for all tables in a particular run of the fuzzy matcher.
         *
         *  returns indicator for whether the script ran successfully or not
         */
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