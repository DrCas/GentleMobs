plugins {
    id("fabric-loom") version "1.17-SNAPSHOT"
    `maven-publish`
}

group = property("group") as String
version = property("version") as String

base {
    archivesName.set("GentleMobs-Fabric")
}

repositories {
    mavenCentral()
}

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    implementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_api_version")}")
}

sourceSets {
    named("main") {
        java.setSrcDirs(listOf("src/fabricMain/java"))
        resources.setSrcDirs(listOf("src/fabricMain/resources"))
    }
}

loom {
    mods {
        create("gentlemobs") {
            sourceSet(sourceSets["main"])
        }
    }
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
}

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}
