import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

// Specify what gradle plugins to use
plugins {
    java
    // IntelliJ plugin
    idea
    // used to package needed dependencies into the jar
    id("com.gradleup.shadow") version "9.2.2"
    // used to generate plugin.yml
    id("net.minecrell.plugin-yml.bukkit") version "0.6.0"
    // used to run a test server locally
    id("xyz.jpenilla.run-paper") version "2.3.0"
    // lombok (provides useful annotations)
    id("io.freefair.lombok") version "8.14.1"
}

// Specify the 'group' (eg: io.github.pylonmc.exampleaddon)
group = project.properties["group"]!!

// Add repositories from which to download dependencies
repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc"
    }
    maven("https://jitpack.io") {
        name = "JitPack"
    }
    maven("https://repo.xenondevs.xyz/releases")
}

// Get dependency versions from gradle.properties
val rebarVersion = project.properties["rebar.version"] as String
val pylonVersion = project.properties["pylon.version"] as String

// Download dependencies
dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    compileOnly("io.github.pylonmc:rebar:$rebarVersion")
    compileOnly("io.github.pylonmc:pylon:$pylonVersion")
}

// Settings for IntelliJ
idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

java {
    // Use Java 25 toolchain for compilation
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

// Configuration for the output JAR
tasks.shadowJar {
    archiveClassifier = ""
    archiveFileName.set("LogiTech.jar")
}

// Generate the plugin.yml file using the bukkit gradle plugin
bukkit {
    name = project.name
    main = project.properties["main-class"] as String
    version = project.version.toString()
    apiVersion = "1.21"
    depend = listOf("Rebar", "Pylon")
    load = BukkitPluginDescription.PluginLoadOrder.STARTUP
}

// Run a server using the run server gradle plugin
tasks.runServer {
    doFirst {
        // Remove the plugins folder. This is so any changes to language files etc are propagated.
        val runFolder = project.projectDir.resolve("run")
        val pluginsDir = runFolder.resolve("plugins")
        pluginsDir.deleteRecursively()
    }

    // Download pylon core and add it to the plugins folder
    downloadPlugins {
        github("pylonmc", "rebar", rebarVersion, "rebar-$rebarVersion.jar")
    }

    // Download pylon base and add it to the plugins folder
    downloadPlugins {
        github("pylonmc", "pylon", pylonVersion, "pylon-$pylonVersion.jar")
    }

    maxHeapSize = "2G"
    minecraftVersion("1.21.11")
}