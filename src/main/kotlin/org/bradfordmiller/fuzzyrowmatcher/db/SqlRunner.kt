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

class SqlRunner {
    companion object {
        val logger = LoggerFactory.getLogger(SqlRunner::class.java)

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