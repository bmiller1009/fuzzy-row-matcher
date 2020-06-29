package org.bradfordmiller.fuzzyrowmatcher.db

import org.bradfordmiller.fuzzyrowmatcher.algos.AlgoType

data class JsonRecord(val id: Long, val jsonData: String)
data class ScoreRecord(val id: Long, val currentRecordId: Long, val compareRecordId: Long, val scores: Map<AlgoType, Number>)
data class DbPayload(val jsonRecords: MutableList<JsonRecord>, val scores: MutableList<ScoreRecord?>)
