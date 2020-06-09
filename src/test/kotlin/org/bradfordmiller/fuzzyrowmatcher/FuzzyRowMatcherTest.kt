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

        val config = Config.ConfigBuilder()
                .sourceJndi(sourceJndi)
                .applyJaroDistance(98.0)
                .applyLevenshtein(5)
                .applyFuzzyScore(90)
                .strLenDeltaPct(80.0)
                .aggregateScoreResults(false)
                .ignoreDupes(true)
                .build()

        val frm = FuzzyRowMatcher(config)
        val result = frm.fuzzyMatch()
        assert(result)
    }
}