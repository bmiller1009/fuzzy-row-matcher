package org.bradfordmiller.fuzzyrowmatcher

import org.bradfordmiller.fuzzyrowmatcher.config.Config
import org.bradfordmiller.fuzzyrowmatcher.config.SourceJndi
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

        val config =
            Config.ConfigBuilder()
            .sourceJndi(sourceJndi)
            .applyJaroDistance(98.0)
            .applyLevenshtein(5)
            .applyFuzzyScore(90)
            .applyCosineDistance(30.0)
            .applyHammingDistance(15)
            .applyJaccardDistance(90.0)
            .strLenDeltaPct(50.0)
            .aggregateScoreResults(true)
            .ignoreDupes(true)
            .build()

        val frm = FuzzyRowMatcher(config)
        val result = frm.fuzzyMatch()
        assert(result)
    }
}