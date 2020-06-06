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
        val sourceJndi = SourceJndi("RealEstateIn", "default_ds","Sacramentorealestatetransactions", hashColumns)

        val config = Config.ConfigBuilder()
                .sourceJndi(sourceJndi)
                .build()

        val frm = FuzzyRowMatcher(config)
        val result = frm.fuzzyMatch()
        assert(result)
    }
}