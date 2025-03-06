plugins {
    kotlin("jvm") version "2.1.10"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

group = "org.mmarket.clans"
version = "1.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") { name = "papermc-repo" }
    maven("https://oss.sonatype.org/content/groups/public/") { name = "sonatype" }
    maven("https://maven.citizensnpcs.co/repo") { name = "citizens-repo" }
    maven("https://jitpack.io")
    maven("https://repo.extendedclip.com/releases/")
}

dependencies {
    // Paper API
    compileOnly("com.destroystokyo.paper", "paper-api", "1.16.5-R0.1-SNAPSHOT")

    // Vault API
    compileOnly("com.github.MilkBowl", "VaultAPI", "1.7")

    // TOML
    implementation("org.tomlj", "tomlj", "1.1.1")

    // Placeholder API
    compileOnly("me.clip", "placeholderapi", "2.11.6")

    // GUI
    implementation("dev.triumphteam", "triumph-gui", "3.1.11")

    // Citizens
    compileOnly("net.citizensnpcs", "citizens-main", "2.0.35-SNAPSHOT") { exclude(group = "*", module = "*") }
}

kotlin { jvmToolchain(17) }

tasks {
    shadowJar {
        archiveBaseName.set("mMarketClans")
        archiveClassifier.set("")
        archiveVersion.set("")

        relocate("dev.triumphteam.gui", "org.mmarket.clans.api.gui")
        relocate("org.tomlj", "org.mmarket.clans.api.toml")
    }

    build { dependsOn("shadowJar") }

    processResources {
        val props = mapOf("version" to version)
        inputs.properties(props)
        filteringCharset = "UTF-8"

        filesMatching("plugin.yml") { expand(props) }
    }

    runServer {
        minecraftVersion("1.16.5")
        jvmArgs("-DPaper.IgnoreJavaVersion=true")
        downloadPlugins {
            url("https://github.com/MilkBowl/Vault/releases/download/1.7.3/Vault.jar")
            modrinth("essentialsx", "2.20.1")
        }
    }
}