package org.bradfordmiller.fuzzyrowmatcher.db

import org.bradfordmiller.fuzzyrowmatcher.config.TargetJndi
import org.bradfordmiller.simplejndiutils.JNDIUtils

class SqlPersistor {
    companion object {
        fun writeRecords(payload: DbPayload, timestamp: String, tj: TargetJndi): Boolean {

            val jsonInsert = "INSERT INTO json_data_$timestamp VALUES (?,?)"
            val scoreInsert = "INSERT INTO scores_$timestamp VALUES (?,?,?,?,?,?,?,?,?)"

            JNDIUtils.getJndiConnection(tj.jndiName, tj.context).use {conn ->
                conn.autoCommit = false
                
            }


            return true;
        }
    }
}