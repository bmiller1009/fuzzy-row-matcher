package org.bradfordmiller.fuzzyrowmatcher.consumer

import org.bradfordmiller.fuzzyrowmatcher.config.TargetJndi
import org.bradfordmiller.fuzzyrowmatcher.db.DbPayload
import org.bradfordmiller.fuzzyrowmatcher.db.SqlPersistor
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.SQLException
import java.util.concurrent.ArrayBlockingQueue

/**
 * definition of a runnable database Consumer. Consumers are responsible for persisting data a JDBC compliant target
 *
 * @property producerQueue - queue where persistor receives data to persist
 * @property targetJndi - the target JNDI location where data will be persisted to
 * @property timestamp - the timestamp which will be suffixed on the name of all output tables in the [targetJndi]
 */
class DBConsumer(
        private val producerQueue: ArrayBlockingQueue<DbPayload>,
        private val targetJndi: TargetJndi,
        timestamp: String
): Runnable {
    companion object {
        val logger: Logger = LoggerFactory.getLogger(DBConsumer::class.java)
    }

    init {
        logger.info("${this.javaClass.canonicalName}:: Initializing target consumer")
    }

    val sqlPersistor = SqlPersistor(timestamp)
    /**
     * pulls/processes the first message off of the [producerQueue] and returns whether or not the message is empty
     */
    private fun processFirstMessage(): Boolean {
        return try {
            val firstMsg = producerQueue.take()
            return if(firstMsg.isEmpty()) {
                logger.warn("First message is empty, an error has occurred in the fuzzy match process. Check the log for details.")
                true
            } else {
                sqlPersistor.writeRecords(firstMsg, targetJndi)
                logger.info("${this.javaClass.canonicalName}:: First data packet written to target.")
                false
            }
        } catch(sqlEx: SQLException) {
            logger.error("Error persisting first message: ${sqlEx.message}")
            true
        }
    }

    /**
     * loops over the queue consuming messages until [doneFlag] is set to true, meaning the last message was empty
     */
    private fun processQueueData(doneFlag: Boolean) {
        var done = doneFlag
        while(!done) {
            val data = producerQueue.take()
            //Empty records hit, means stream is complete
            if(data.isEmpty()) {
                logger.info("Received empty data object. Terminating data consumer thread.")
                done = true
            } else {
                sqlPersistor.writeRecords(data, targetJndi)
            }
        }
    }

    /**
     * launches consumer as a runnable
     */
    override fun run() {
        try {
            val complete = processFirstMessage()
            processQueueData(complete)
        } catch(ex: Exception) {
            logger.error("An error occurred while running consumer. See logs for details.")
            throw ex
        }
    }
}