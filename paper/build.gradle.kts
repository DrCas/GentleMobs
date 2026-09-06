// Existing Paper build restored alongside the Fabric checkout, with sources left in place.
plugins {
    `java-library`
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21"
}
group = "io.github.drcas"
version = "0.2.0"
base { archivesName.set("GentleMobs") }
repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}
dependencies { paperweight.paperDevBundle("26.2.build.+") }
java { toolchain.languageVersion.set(JavaLanguageVersion.of(25)) }
sourceSets.main {
    java.setSrcDirs(listOf("../src/main/java"))
    resources.setSrcDirs(listOf("../src/main/resources"))
}
tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("plugin.yml") { expand("version" to project.version) }
}
