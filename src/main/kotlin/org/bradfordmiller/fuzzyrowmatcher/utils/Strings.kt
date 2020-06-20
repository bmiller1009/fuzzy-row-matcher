package org.bradfordmiller.fuzzyrowmatcher.utils

class Strings {
    companion object {
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