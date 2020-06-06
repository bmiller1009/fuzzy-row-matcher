package org.bradfordmiller.fuzzyrowmatcher

import org.bradfordmiller.fuzzyrowmatcher.FuzzyRowMatcher
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FuzzyRowMatcherTest {

    @Test
    fun simpleFuzzyMatch() {
        val frm = FuzzyRowMatcher()
        val result = frm.fuzzyMatch()
        assert(result)
    }

}