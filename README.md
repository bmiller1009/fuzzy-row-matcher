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
  <version>1.0.0</version>
</dependency>
```

#### Running with SBT

Add this GAV coordinate to your SBT dependency list

```sbt
libraryDependencies += "org.bradfordmiller" %% "fuzzy-row-matcher" % "1.0.0"
```

#### Running with Gradle

Add this GAV coordinate to your Gradle dependencies section

```gradle
dependencies {
    ...
    ...
    implementation 'org.bradfordmiller:fuzzy-row-matcher:1.0.0'
}
```

## Using the library

API docs were generated using [dokka](https://github.com/Kotlin/dokka) and are hosted [here](https://bmiller1009.github.io/fuzzy-row-matcher/).

Configuation information is stored using the [simple-jndi](https://github.com/h-thurow/Simple-JNDI)
API so a [jndi.properties](https://github.com/bmiller1009/fuzzy-row-matcher/blob/master/src/test/resources/jndi.properties) file will need to be present
in src/main/resources and correctly configured for this library to work. 

### Configuring jndi
See [jndi.properties](https://github.com/bmiller1009/fuzzy-row-matcher/blob/master/src/test/resources/jndi.properties) sample file
in this project. This will need to be configured and dropped into **_src/main/resources_** for the API to work.  Actual jndi
files will be searched and loaded based upon the path in the **_org.osjava.sj.root_** property setting.  In the example file 
for this project, you will see the path is **_src/main/resources/jndi_**.  

#### Configuring jndi contexts
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

The only _required_ inputs to fuzzy-row-matcher are a JDBC souce in the form of a JNDI Connection and at least one algorithm and it's threshold value.  This is set up using the SourceJndi class.  Here is some Kotlin code which instantiates a **_SourceJndi_** object. 
```kotlin
import org.bradfordmiller.org.bradfordmiller.fuzzyrowmatcher.config.config.SourceJndi  

...  
val csvSourceJndi = SourceJndi("RealEstateIn", "default_ds", "Sacramentorealestatetransactions")
```
In the above case "RealEstateIn" is the jndi name, "default\_ds" is the context name (and correlates to "default\_ds.properties"), and "Sacramentorealestatetransactions" is the table to be queried. 

By default, a "SELECT *" query will be issued against the table ("Sacramentorealestatetransactions" in this case). It is also possible to pass in a query, rather than a table name, like so:
```kotlin
import org.bradfordmiller.deduper.config.SourceJndi  
...  
val csvSourceJndi = SourceJndi("RealEstateIn", "default_ds", "SELECT street from Sacramentorealestatetransactions")