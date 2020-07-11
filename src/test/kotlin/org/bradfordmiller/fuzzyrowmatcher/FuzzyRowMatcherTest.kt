package org.bradfordmiller.fuzzyrowmatcher

import org.bradfordmiller.fuzzyrowmatcher.config.Config
import org.bradfordmiller.fuzzyrowmatcher.config.SourceJndi
import org.bradfordmiller.fuzzyrowmatcher.config.TargetJndi
import org.bradfordmiller.fuzzyrowmatcher.db.SqlRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FuzzyRowMatcherTest {
    @Test
    fun simpleFuzzyMatch() {

        val hashColumns = mutableSetOf("street","city", "state", "zip", "price")
        val sourceJndi =
            SourceJndi(
                    "RealEstateIn",
                    "default_ds",
                    "SELECT * FROM Sacramentorealestatetransactions",
                    hashColumns
            )
        //Add defaults for each algos
        val config =
            Config.ConfigBuilder()
                .sourceJndi(sourceJndi)
                .targetJndi(TargetJndi("SqlLiteTest", "default_ds"))
                .applyJaroDistance(98.0)
                .applyLevenshtein(5)
                .applyFuzzyScore(90)
                .applyCosineDistance(30.0)
                .applyHammingDistance(15)
                .applyJaccardDistance(90.0)
                .strLenDeltaPct(50.0)
                .aggregateScoreResults(false)
                .ignoreDupes(false)
                .samplePercentage(25)
                .build()

        val frm = FuzzyRowMatcher(config)
        val result = frm.fuzzyMatch()
        println(result)
        assert(true)
    }

    @Test
    fun testTableBootstrap() {
        val tj = TargetJndi("SqlLiteTest", "default_ds")
        val timestamp = (System.currentTimeMillis() / 1000).toString()
        val success = SqlRunner.runScript(tj.jndiName, tj.context, timestamp)
        assert(success)
    }
}