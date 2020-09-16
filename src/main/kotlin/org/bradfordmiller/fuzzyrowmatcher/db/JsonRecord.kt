package org.bradfordmiller.fuzzyrowmatcher.db

import org.bradfordmiller.fuzzyrowmatcher.algos.AlgoType
import java.util.*

data class JsonRecord(val id: String, val jsonData: String)
data class ScoreRecord(val id: String, val currentRecordId: String, val compareRecordId: String, val scores: Map<AlgoType, Number>)
data class DbPayload(val jsonRecords: MutableList<JsonRecord>, val scores: MutableList<ScoreRecord?>)
