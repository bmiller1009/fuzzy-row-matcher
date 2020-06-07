package org.bradfordmiller.fuzzyrowmatcher.config

import org.apache.commons.lang.NullArgumentException
import org.slf4j.LoggerFactory

/**
 * A source jndi entity
 *
 * @property jndiName the jndi name defined in the simple-jndi properties file
 * @property context the context name for the jndi name, which basically maps to a properties file of the same name
 * IE if context = "test" then there should be a corresponding test.properties file present in the org.osjava.sj.root
 * defined directory in jndi.properties.  In the above example, if the context = "test" and org.osjava.sj.root =
 * src/main/resources/jndi then the jndi name will be searched for in src/main/resources/jndi/test.properties
 * @property tableQuery can either be a table (which 'SELECT *' will be run against) or a specific 'SELECT' SQL query
 * @property hashKeys a list of column names which will be used to hash the values returned by [tableQuery]
 */
data class SourceJndi(
    val jndiName: String,
    val context: String,
    val tableQuery: String,
    val hashKeys: MutableSet<String> = mutableSetOf()
) {
    val sql by lazy {
        if (tableQuery.startsWith("SELECT", true)) {
            tableQuery
        } else {
            "SELECT * FROM ${tableQuery}"
        }
    }
}

class Config private constructor(
    val sourceJndi: SourceJndi,
    val jaroWinkler: JaroWinkler?
) {

    data class JaroWinkler(
      val threshold: Double
    )

    data class ConfigBuilder(
        private var sourceJndi: SourceJndi? = null,
        private var jaroWinkler: JaroWinkler? = null
    ) {
        companion object {
            private val logger = LoggerFactory.getLogger(ConfigBuilder::class.java)
        }

        fun sourceJndi(sourceJndi: SourceJndi) = apply {this.sourceJndi = sourceJndi}
        fun applyJaro(threshold: Double) = apply {this.jaroWinkler = JaroWinkler(threshold)}

        fun build(): Config {
            val sourceJndi = sourceJndi ?: throw NullArgumentException("Source JNDI must be set")
            val jaroWinkler = jaroWinkler
            val config = Config(sourceJndi, jaroWinkler)
            logger.trace("Built config object $config")
            return config
        }
    }
}