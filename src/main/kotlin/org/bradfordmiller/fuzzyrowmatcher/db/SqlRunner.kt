package org.bradfordmiller.fuzzyrowmatcher.db

import org.apache.ibatis.jdbc.RuntimeSqlException
import org.apache.ibatis.jdbc.ScriptRunner
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.File
import java.sql.Connection
import java.io.FileReader

class SqlRunner {
    companion object {
        val logger = LoggerFactory.getLogger(SqlRunner::class.java)

        fun runScript(conn: Connection, f: File): Boolean {
            return try {
                conn.use { c ->
                    BufferedReader(FileReader(f)).use { br ->
                        val sr = ScriptRunner(c)
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