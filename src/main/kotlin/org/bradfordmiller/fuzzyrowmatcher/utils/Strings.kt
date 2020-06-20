package org.bradfordmiller.fuzzyrowmatcher.utils

class Strings {
    companion object {
        fun checkStrLen(compareRow: String, currentRow: String, strLenDeltaPct: Double): Boolean {
            val compareLen = compareRow.length
            val currentRow = currentRow.length
            val pct =
                    if(compareLen > currentRow) {
                        currentRow.toDouble() / compareLen.toDouble()
                    } else {
                        compareLen.toDouble() / currentRow.toDouble()
                    }
            return (pct * 100) >= strLenDeltaPct
        }
    }
}