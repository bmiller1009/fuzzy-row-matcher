package org.bradfordmiller.fuzzyrowmatcher.utils

/**
 * string utilty class
 */
class Strings {
    companion object {

        /**
         * calculates the length of the string [compareRow] and the string [currentRow] and
         * determines whether the delta difference in [strLenDeltaPct] is exceeded
         */
        fun checkStrLen(compareRow: String, currentRow: String, strLenDeltaPct: Double): Boolean {
            val compareLen = compareRow.length
            val currRow = currentRow.length
            val pct =
                    if(compareLen > currRow) {
                        currRow.toDouble() / compareLen.toDouble()
                    } else {
                        compareLen.toDouble() / currRow.toDouble()
                    }
            return (pct * 100) >= strLenDeltaPct
        }
    }
}