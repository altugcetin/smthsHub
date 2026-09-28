plugins {
    id("java")
    id("com.gradleup.shadow") version "9.2.2"
}

group = "ist.alchm"
version = "4.0.3"
description = "smthsHub"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-public/")
    maven("https://repo.opencollab.dev/main/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://repo.tcoded.com/releases")
    maven("https://jitpack.io")
    maven("https://repo.codemc.org/repository/maven-public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://libraries.minecraft.net/")
    maven("https://repo.maven.apache.org/maven2/")
}

dependencies {
    implementation("com.github.cryptomorin:XSeries:13.7.1")
    implementation("javax.inject:javax.inject:1")
    implementation("javax.annotation:javax.annotation-api:1.2")
    implementation("com.github.BGMP.CommandFramework:command-framework-bukkit:master") {
        exclude(group = "org.bukkit", module = "bukkit")
    }
    implementation("com.tcoded:FoliaLib:0.5.1")
    implementation("de.tr7zw:item-nbt-api:2.16.0") // UPDATE THIS FOR EACH NEW MC VERSION
    implementation("org.bstats:bstats-bukkit-lite:1.7")
    implementation("com.github.shynixn.headdatabase:hdb-api:1.0")
    implementation("com.github.ItzSave:ZithiumLibrary:1f5182b77f")

    compileOnly("io.papermc.paper:paper-api:26.2.build.129-stable")
    compileOnly("com.github.retrooper:packetevents-spigot:2.13.0")
    compileOnly("com.github.dmulloy2:ProtocolLib:5.3.0")
    compileOnly("org.geysermc.floodgate:api:2.2.5-SNAPSHOT")
    compileOnly("net.md-5:bungeecord-chat:1.16-R0.1")
    compileOnly("com.mojang:authlib:1.5.21")
    compileOnly("me.clip:placeholderapi:2.11.6")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks {
    build {
        dependsOn(shadowJar)
    }

    processResources {
        inputs.properties(mapOf("version" to version))
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand("version" to version)
        }
    }

    shadowJar {
        minimize {
            exclude(dependency("com.tcoded:FoliaLib:.*"))
        }

        archiveClassifier.set("") // Removes "-all" suffix

        relocate("org.bstats", "ist.alchm.smthsHub.libs.metrics")
        relocate("cl.bgmp", "ist.alchm.smthsHub.libs.command")
        relocate("com.tcoded.folialib", "ist.alchm.smthsHub.libs.folialib")
        relocate("de.tr7zw.changeme.nbtapi", "ist.alchm.smthsHub.libs.nbt")
        relocate("net.zithium.library", "ist.alchm.smthsHub.libs.library")
        relocate("com.cryptomorin.xseries", "ist.alchm.smthsHub.libs.xseries")
    }

    named<Test>("test") {
        failOnNoDiscoveredTests.set(false)
    }
}

val spawnMemoryTest = tasks.register<JavaExec>("spawnMemoryTest") {
    group = "verification"
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("ist.alchm.smthsHub.spawn.SpawnMemoryTest")
    dependsOn(tasks.named("testClasses"))
}

val bedrockFormsTest = tasks.register<JavaExec>("bedrockFormsTest") {
    group = "verification"
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("ist.alchm.smthsHub.inventory.BedrockFormsTest")
    dependsOn(tasks.named("testClasses"))
}

val bedrockDebugTest = tasks.register<JavaExec>("bedrockDebugTest") {
    group = "verification"
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("ist.alchm.smthsHub.debug.BedrockDebugTest")
    dependsOn(tasks.named("testClasses"))
}

tasks.named("build") {
    dependsOn(spawnMemoryTest, bedrockFormsTest, bedrockDebugTest)
}
