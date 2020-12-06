package org.bradfordmiller.fuzzyrowmatcher.db

import org.bradfordmiller.fuzzyrowmatcher.algos.AlgoType

/**
 * database object which represents a json object of all hashcolumns and a primary key (which is the md5 hash of the stringified JSON object)
 *
 * @property [id] primary key of the JSON object, it's md5 hash representation
 * @property jsonData json representation of all columns specified in the SELECT query which queries the source JNDI
 */
data class JsonRecord(val id: String, val jsonData: String)

/**
 * database object which represents the scores for the comparisons between [currentRecordId] and [compareRecordId] along with
 * an id for the primary key and map of scores for each algorithm
 *
 * @property primary key for the row
 * @property currentRecordId foreign key to the [JsonRecord]
 * @property compareRecordId foreign key to the [JsonRecord]
 * @property scores map of each algorithm and it's corresponding score when comparing [currentRecordId] and [compareRecordId]
 */
data class ScoreRecord(val id: String, val currentRecordId: String, val compareRecordId: String, val scores: Map<AlgoType, Number>)

/**
 * a wrapper object for a list of [JsonRecord] and a list of [ScoreRecord] which will be persisted to a JDBC target
 */
data class DbPayload(val jsonRecords: MutableList<JsonRecord>, val scores: MutableList<ScoreRecord>) {
    fun isEmpty(): Boolean = jsonRecords.isEmpty() && scores.isEmpty()
}
