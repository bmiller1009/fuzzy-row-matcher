# fuzzy-row-matcher

[![Build Status](https://travis-ci.org/bmiller1009/fuzzy-row-matcher.svg?branch=master)](https://travis-ci.org/bmiller1009/fuzzy-row-matcher)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.bradfordmiller/fuzzy-row-matcher/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.bradfordmiller/fuzzy-row-matcher)
[![github: bmiller1009/fuzzy-row-matcher](https://img.shields.io/badge/github%3A-issues-blue.svg?style=flat-square)](https://github.com/bmiller1009/fuzzy-row-matcher/issues)

Framework for finding similar rows in a JDBC source

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes.

### Prerequisites

What things you need to install the software and how to install them

* Gradle 5.4.1 or greater if you want to build from source
* JVM 8+

### Installing

#### Running with Maven

If you're using [Maven](maven.apache.org) simply specify the GAV coordinate below and Maven will do the rest

```xml
<dependency>
  <groupId>org.bradfordmiller</groupId>
  <artifactId>fuzzy-row-matcher</artifactId>
  <version>1.0.6</version>
</dependency>
```

#### Running with SBT

Add this GAV coordinate to your SBT dependency list

```sbt
libraryDependencies += "org.bradfordmiller" %% "fuzzy-row-matcher" % "1.0.6"
```

#### Running with Gradle

Add this GAV coordinate to your Gradle dependencies section

```gradle
dependencies {
    ...
    ...
    implementation 'org.bradfordmiller:fuzzy-row-matcher:1.0.6'
}
```

## Using the library

API docs were generated using [dokka](https://github.com/Kotlin/dokka) and are hosted [here](https://bmiller1009.github.io/fuzzy-row-matcher/).

Configuation information is stored using the [simple-jndi](https://github.com/h-thurow/Simple-JNDI)
API so a [jndi.properties](https://github.com/bmiller1009/fuzzy-row-matcher/blob/master/src/test/resources/jndi.properties) file will need to be present
in src/main/resources and correctly configured for this library to work. 

### <a id="conf-jndi">Configuring jndi</a>
See [jndi.properties](https://github.com/bmiller1009/fuzzy-row-matcher/blob/master/src/test/resources/jndi.properties) sample file
in this project. This will need to be configured and dropped into **_src/main/resources_** for the API to work.  Actual jndi
files will be searched and loaded based upon the path in the **_org.osjava.sj.root_** property setting.  In the example file 
for this project, you will see the path is **_src/main/resources/jndi_**.  

#### <a id="conf-jndi-contexts">Configuring jndi contexts</a>
Jndi property files can be dropped into the **_org.osjava.sj.root_** configured path. In our case, that path is **_src/main/resources/jndi_**.  There is one context that fuzzy-row-matcher can handle:  A **javax.sql.DataSource**

Datasources are used when reading or writing data using a JDBC interface. These concepts will be explained in detail later. You can see a sample jndi context file [here](https://github.com/bmiller1009/fuzzy-row-matcher/blob/master/src/test/resources/jndi/default_ds.properties).  Note the location of the context file is in the directory set in **_org.osjava.sj.root_**:  **_src/test/resources/jndi_**.  All jndi context files must be placed under this directory.

Here is a sample DataSource entry for a sql lite database which is used by this projects unit tests (note that username and password are optional and depend on how the security of the database being targeted is configured):
```properties
SqliteChinook/type=javax.sql.DataSource  
SqliteChinook/driver=org.sqlite.JDBC  
SqliteChinook/url=jdbc:sqlite:src/test/resources/data/chinook.db  
SqliteChinook/user=  
SqliteChinook/password=
```

The jndi name in this case is "SqliteChinook".  The context is "default\_ds" because the name of the property file is "default_ds.properties".

#### Adding Jndi entries programatically

Use the **_JNDIUtils_** class in the deduper library to add jndi entries programatically

Kotlin code for adding a new DataSource jndi entry to the default_ds.properties jndi file:
```kotlin
import org.bradfordmiller.deduper.jndi.JNDIUtils  
...  
JNDIUtils.addJndiConnection(  
	    "BradTestJNDI_23",  
	    "default_ds",  
	     mapOf(  
		    "type" to "javax.sql.DataSource",  
		    "driver" to "org.sqlite.JDBC",  
		    "url" to "jdbc:sqlite:src/test/resources/data/outputData/real_estate.db",  
		    "user" to "test_user",  
		    "password" to "test_password"  
	    )  
    )  
```

### Configuring and running a deduper process

The library uses the [builder](https://www.baeldung.com/kotlin-builder-pattern) design pattern to construct the configuration to run a fuzzy matching job. 

There are a bunch of options which can be configured as part of a fuzzy matching process.  Let's start with the basics. Use the **_Config_** class to set up the fuzzy matching job.  This object will be passed into the **_FuzzyRowMatcher_** class as part of the instantiation of the **_FuzzyRowMatcher_** class.  

The only _required_ inputs to fuzzy-row-matcher are a JDBC souce in the form of a JNDI Connection and at least one algorithm and it's threshold value.  The JNDI connection is set up using the SourceJndi class.  Here is some Kotlin code which instantiates a **_SourceJndi_** object. 
```kotlin
import org.bradfordmiller.fuzzyrowmatcher.config.config.SourceJndi
...  
val csvSourceJndi = SourceJndi("RealEstateIn", "default_ds", "Sacramentorealestatetransactions")
```
In the above case "RealEstateIn" is the jndi name, "default\_ds" is the context name (and correlates to "default\_ds.properties"), and "Sacramentorealestatetransactions" is the table to be queried. 

By default, a "SELECT *" query will be issued against the table ("Sacramentorealestatetransactions" in this case). It is also possible to pass in a query, rather than a table name, like so:
```kotlin
import org.bradfordmiller.fuzzyrowmatcher.config.SourceJndi  
...  
val csvSourceJndi = SourceJndi("RealEstateIn", "default_ds", "SELECT street from Sacramentorealestatetransactions")
```
Fuzzy Row Matcher is an engine which can detect row similarity within a table or flat file, so by default it will use every value in the row to create a duplicate. The API also accepts a subset of columns in the table on which to "fuzzily match".  Here is some Kotlin code which demonstrates this:
```kotlin
import org.bradfordmiller.fuzzyrowmatcher.config.SourceJndi  
...  
val hashColumns = mutableSetOf("street","city", "state", "zip", "price")  
val csvSourceJndi = SourceJndi("RealEstateIn", "default_ds", "Sacramentorealestatetransactions", hashColumns)
```
Now only the columns specified in the column set will be considered for detecting duplicates.

### Complete example with Jaro-Winkler scoring algorithm
```kotlin
import org.bradfordmiller.fuzzyrowmatcher.config.SourceJndi
import org.bradfordmiller.fuzzyrowmatcher.FuzzyRowMatcher
import org.bradfordmiller.fuzzyrowmatcher.config.Config
...

val csvSourceJndi = SourceJndi("RealEstateIn", "default_ds", "Sacramentorealestatetransactions")

val config = 
Config.ConfigBuilder()
    .sourceJndi(csvSourceJndi)
    .applyJaroDistance(98.0)
    .build()

val frm = FuzzyRowMatcher(config)

val result = frm.fuzzyMatch()

println(result)
```    
The output of this run is:

     FuzzyRowMatcherRpt(rowCount=987, comparisonCount=485606, matchCount=7, duplicateCount=0, algos={JaroDistance=AlgoStats(min=63.25954223397656, firstQuartile=73.01287149712347, median=74.83295979588313, thirdQuartile=76.85458437015583, max=100.0, mean=75.04290962954407, stddeviation=3.0175386794224877)})

So this run of the FuzzyMatch found a total of **987** rows, ran a total of **485606** comparisons against the table, and found a total of **7** rows which are similar.  Similar in this case means that there were seven rows in the table which passed the **98.0** percent similar threshold of the JaroWinkler distance algorithm.

Also note that each algorithm returns a set of **seven** statistics for the run:

1) The minimum threshold found (in the above output, **63.25954223397656**)
2) The first quartile of the threshold found (in the above output, **73.01287149712347**)
3) The median threshold found (in the above output, **74.83295979588313**)
4) The third quartile of the threshold found (in the above output, **76.85458437015583**)
5) The maximum threshold found (in the above output, **100.0**)
6) The mean threshold found (in the above output, **75.04290962954407**)
7) The standard deviation of the threshold found (in the above output, **3.0175386794224877**)

If more algorithms are applied, their statistics will also appear in the **algos** variable.  Note also in the above example **all** columns in each row were concatenated together because a list of columns was not provided in the **_SourceJndi_** object instantiation.

### List of algorithms availability for fuzzy matching:

Fuzzy Matcher has the following algorithms available:

1) [Jaro-Winkler Distance](https://en.wikipedia.org/wiki/Jaro%E2%80%93Winkler_distance)
2) [Levenshtein Distance](https://en.wikipedia.org/wiki/Levenshtein_distance)
3) [Hamming Distance](https://en.wikipedia.org/wiki/Hamming_distance)
4) [Jaccard Distance](https://en.wikipedia.org/wiki/Jaccard_index)
5) [Cosine Distance](https://en.wikipedia.org/wiki/Cosine_similarity)
6) [Fuzzy Similarity](https://commons.apache.org/proper/commons-text/apidocs/org/apache/commons/text/similarity/FuzzyScore.html)

The results of these algorithms can be combined for the qualification of similarity, or each algorithm can be applied independently.  There is a setting in the config for controlling this:

```kotlin
val config =
    Config.ConfigBuilder()
            .sourceJndi(sourceJndi)
            .applyJaroDistance(98.0)
            .applyLevenshtein(5)
            .aggregateScoreResults(true)
            .build()
```

In this example, the Jaro-Winkler threshold is set to **98** percent, and the Levenshtein algorithm is set to **5**.  Because aggregateScoreResults is set to **true**, only rows which pass the threshold of 98 on Jaro **and** 5 on Levenshtein will be considered "similar".  By default, aggregateScoreResults is set to **false**.  In the case where aggregateScoreResults is set to **false** rows will be considered "similar" if the Jaro threshold of 98 **OR** a Levenshtein threshold of **5** is met. 

### Handling duplicates
The Fuzzy Match engine has the ability to ignore duplicates if you are only interested in similar but not exact matches.  This is controlled by the 
```kotlin
 .ignoreDupes(true)
 ``` 
method on the _**Config**_ object.  If this flag is set to true then the fuzzy comparisons will not be done when an exact match is detected.  This can speed up processing if the table you are interrogating has lots of exact matches.  The default value for this is **false**.

### String length differences
Another optimization the engine contains is in regards to string length differences.  You can tell the engine to ignore strings which differ greatly in their length.  The method for this is 
```kotlin
 .strLenDeltaPct(50.0)
``` 
on the _**Config**_ object
In this case, the engine will not perform similarity comparisons on strings which differ in length by more than **50 percent**.

### Persisting the score results
Thus far we have only seen examples of the Fuzzy Matcher aggregating information about statistics and matches, now we will see how to persist the results of the Fuzzy Match run.  The API has a setting for a target JNDI location as seen here:
```kotlin
.targetJndi(TargetJndi("SqlLiteTest", "default_ds"))
```
A full example looks like this:

```kotlin
val config =
                Config.ConfigBuilder()
                        .sourceJndi(sourceJndi)
                        .targetJndi(TargetJndi("SqlLiteTest", "default_ds"))
                        .applyJaroDistance(98.0)
                        .applyLevenshtein(5)
                        .aggregateScoreResults(false)
                        .build()
```
The _**TargetJndi object**_ accepts a named jndi resource, in this case "SqlLiteTest" and a context, in this case "default_ds".  See the [Configuring JNDI](#conf-jndi) and [Configuring JNDI Contexts](#conf-jndi-contexts) sections on how to add JNDI entries to Fuzzy Matcher.  **Note that as of right, now the only supported output is to SqlLite**.

The output created by Fuzzy Matcher consists of two tables and a view. The tables are created with an appended timestamp for convenience so that we can avoid table name collisions.

The templates for the documentation below can be found [here](https://github.com/bmiller1009/fuzzy-row-matcher/blob/master/src/main/resources/dbscripts/bootstrap_sqlite.sql)

The first table is simply a "jsonified" view of the row being compared, and has a schema which is as follows:

```sql
CREATE TABLE json_data_**TIMESTAMP** (
	id	INTEGER NOT NULL,
	json_row	TEXT NOT NULL,
	PRIMARY KEY(id)
);
```

Again, this table has a unique id and a string which represents the row in a "jsonified" format. Here is an example row:

id: 1
json_row: 
```json
{"zip":"95838","baths":"1","city":"SACRAMENTO","sale_date":"Wed May 21 00:00:00 EDT 2008","street":"3526 HIGH ST","price":"59222","latitude":"38.631913","state":"CA","beds":"2","type":"Residential","sq__ft":"836","longitude":"-121.434879"}
```

The second table contains qualifying "similar" rows and their scores. The schema for the "scores" table is as follows:

```sql
CREATE TABLE scores_**TIMESTAMP** (
	id	INTEGER NOT NULL,
	json_data1_row_id	INTEGER NOT NULL,
	json_data2_row_id	INTEGER NOT NULL,
	jaro_dist_score	REAL NULL,
	levenshtein_distance_score	INTEGER NULL,
	hamming_distance_score	INTEGER NULL,
	jaccard_distance_score	REAL NULL,
	cosine_distance_score	REAL NULL,
	fuzzy_similiarity_score	INTEGER NULL,
	FOREIGN KEY(json_data1_row_id) REFERENCES json_data(id),
	FOREIGN KEY(json_data2_row_id) REFERENCES json_data(id),
	PRIMARY KEY(id,json_data1_row_id,json_data2_row_id)
);
```

This table contains the id of each row being compared (foreign keys link back to the json_data table described earlier), as well as each algorithm scored calculated between the two rows.

Lastly, there is a view which ties everything together for ease of viewing the results:

```sql
CREATE VIEW final_scores_**TIMESTAMP** AS
SELECT a.json_row json_row_1, c.json_row json_row_2,
b.jaro_dist_score,
	b.levenshtein_distance_score	,
	b.hamming_distance_score	,
	b.jaccard_distance_score	,
	b.cosine_distance_score	,
	b.fuzzy_similiarity_score
FROM
json_data_**TIMESTAMP** a
INNER JOIN scores_**TIMESTAMP** b ON a.id = b.json_data1_row_id
INNER JOIN json_data_**TIMESTAMP** c ON c.id = b.json_data2_row_id;
```

This view simply links back the actual json strings as well as the scores for a simpler viewing of the run.

**Note that score values will be populated as NULL in the **scores** table for those algorithms which were not chosen for the particular run.**

Future versions of the software will support more database types to output results to (IE, postgres, mysql, oracle, etc)

### Fully configured example with all flags being toggled:

```kotlin
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
            .ignoreDupes(true)
            .build()

    val frm = FuzzyRowMatcher(config)
    val result = frm.fuzzyMatch()
```

## Built With

* [kotlin](https://kotlinlang.org/) - The programming language
* [simple-jndi](https://github.com/h-thurow/Simple-JNDI) - source and target configuration management
* [csvjdbc](http://csvjdbc.sourceforge.net/) - Provides a query API on top of csv
* [apache-commons-text](https://commons.apache.org/proper/commons-text/) - For string distance and similarity algorithms
* [apache-commons-math](https://commons.apache.org/proper/commons-math/) - For statistical calculations

## Versioning

For the versions available, see the [tags on this repository](https://github.com/bmiller1009/fuzzy-row-matcher/tags). 

## Authors

* **Bradford Miller** - *Initial work* - [bfm](https://github.com/bmiller1009)

See also the list of [contributors](https://github.com/bmiller1009/fuzzy-row-matcher/contributors) who participated in this project.

## License

This project is licensed under the Apache 2.0 License - see the [LICENSE](LICENSE) file for details

## Acknowledgments

* Thanks to [PurpleBooth](https://gist.github.com/PurpleBooth) for the README template as seen [here](https://gist.github.com/PurpleBooth/109311bb0361f32d87a2)
