/*
 * This file was generated by the Gradle 'init' task.
 *
 * This generated file contains a sample Kotlin application project to get you started.
 */
import org.jetbrains.dokka.gradle.DokkaTask
import groovy.lang.Closure
import org.jetbrains.kotlin.gradle.utils.loadPropertyFromResources
import java.util.Properties
import java.io.File
import java.time.Duration
import org.apache.commons.io.FileUtils

plugins {
    // Apply the Kotlin JVM plugin to add support for Kotlin on the JVM.
    id("org.jetbrains.kotlin.jvm").version("1.3.72")
    id("org.jetbrains.dokka").version("0.10.0")
    id("net.researchgate.release").version("2.6.0")
    id("java-library")
    id ("com.github.johnrengelman.shadow").version( "5.1.0")
    id ("distribution")
    // Apply the application plugin to add support for building a CLI application.
    application
    java
}

group = "org.bradfordmiller"

tasks.create("set-defaults") {
    doFirst {
        val props = Properties()
        val inputStream = File("version.properties").inputStream()
        props.load(inputStream)
        val softwareVersion = props.get("version")!!.toString()

        println("Software Version: " + softwareVersion)

        group = "org.bradfordmiller"
        version = softwareVersion
        inputStream.close()

        /*val source = File("version.properties")
        val dest = File("src/dist/lib/conf/version.properties")

        FileUtils.copyFile(source, dest)*/
    }
    doLast {
        println("Current software version is $version")
    }
}

tasks.build {
    dependsOn("set-defaults")
}

//Sample gradle CLI: gradle release -Prelease.useAutomaticVersion=true
release {
    failOnCommitNeeded = true
    failOnPublishNeeded = true
    failOnSnapshotDependencies = true
    failOnUnversionedFiles = true
    failOnUpdateNeeded = true
    revertOnFail = true
    preCommitText = ""
    preTagCommitMessage = "[Gradle Release Plugin] - pre tag commit: "
    tagCommitMessage = "[Gradle Release Plugin] - creating tag: "
    newVersionCommitMessage = "[Gradle Release Plugin] - new version commit: "
    version = "$version"
    versionPropertyFile = "version.properties"
}

repositories {
    // Use jcenter for resolving your dependencies.
    // You can declare any Maven/Ivy/file repository here.
    jcenter()
}

dependencies {
    // Use the Kotlin JDK 8 standard library.
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    
    implementation("org.apache.logging.log4j", "log4j-slf4j-impl", "2.13.3")
    implementation("org.apache.logging.log4j",  "log4j-core",  "2.13.3")
    implementation("org.apache.logging.log4j",  "log4j-api",  "2.13.3")
    implementation("com.fasterxml.jackson.core", "jackson-core", "2.11.2")
    implementation("com.fasterxml.jackson.core", "jackson-databind", "2.11.2")
    implementation("com.fasterxml.jackson.module", "jackson-module-kotlin", "2.11.2")
    implementation("io.vavr:vavr-kotlin:0.10.2")
    implementation("org.apache.commons", "commons-text", "1.9")
    implementation("net.sourceforge.csvjdbc:csvjdbc:1.0.36")
    implementation("org.json", "json", "20200518")
    implementation("org.mybatis", "mybatis", "3.5.5")
    implementation("org.xerial", "sqlite-jdbc","3.32.3.2")
    implementation("org.apache.commons", "commons-math3", "3.6.1")
            // Use the Kotlin test library.
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    api("org.bradfordmiller", "simplejndiutils", "0.0.10") {
        isTransitive = true
    }
    api("commons-codec", "commons-codec","1.12")
    api("org.bradfordmiller:sqlutils:0.0.1")
    // Use the Kotlin JUnit integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.5.1")
}

tasks.matching{it.name != "set-defaults"}.forEach {t ->
    println("Found Task: " + t.name)
    t.dependsOn("set-defaults")
}



/*tasks.named<CreateStartScripts>("startScripts") {
    classpath = files(classpath) + files("src/dist/lib/conf")
}*/

/*distributions {
    getByName("main") {
        contents {
            from("src/dist/bin/conf/jndi") {
                into("bin/conf/jndi")
            }
            from("registration") {
                into("bin/registration")
            }
        }
    }
}*/

tasks {
    val dokka by getting(DokkaTask::class) {
        outputFormat = "html"
        outputDirectory = "$buildDir/dokka"
    }
}


application {
    // Define the main class for the application.
    mainClassName = "org.bradfordmiller.fuzzymatcher.DriverKt"

    group = "org.bradfordmiller.fuzzymatcher"
    version = "$version"

}