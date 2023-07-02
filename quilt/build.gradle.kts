import org.jetbrains.kotlin.gradle.plugin.extraProperties

plugins {
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

repositories {
    mavenLocal()
    maven("https://maven.quiltmc.org/repository/release/")
    maven("https://maven.isxander.dev/releases") {
        name = "Xander Maven"
    }
    maven("https://maven.terraformersmc.com/releases") // Mod Menu. YACL need it
    maven("https://ladysnake.jfrog.io/artifactory/mods")
    maven("https://jitpack.io")
    mavenCentral()
}

architectury {
    platformSetupLoomIde()
    loader("quilt")
}

loom {
    accessWidenerPath.set(project(":common").loom.accessWidenerPath)
}

/**
 * @see: https://docs.gradle.org/current/userguide/migrating_from_groovy_to_kotlin_dsl.html
 * */
val common: Configuration by configurations.creating
val shadowCommon: Configuration by configurations.creating // Don't use shadow from the shadow plugin because we don't want IDEA to index this.

val developmentQuilt: Configuration = configurations.getByName("developmentQuilt")
configurations {
    compileClasspath { extendsFrom(configurations["common"]) }
    runtimeClasspath { extendsFrom(configurations["common"]) }
    developmentQuilt.extendsFrom(configurations["common"])
}

dependencies {
    modImplementation(libs.quilt.loader)
    modImplementation(libs.quilt.fabricApi) {
        exclude(group = "org.quiltmc")
    }
    modImplementation(libs.quilt.standardLibraries)
    // Remove the next few lines if you don't want to depend on the API
//    modApi(libs.architectury.fabric) {
//        // We must not pull Fabric Loader from Architectury Fabric
//        exclude(group = "net.fabricmc")
//        exclude(group = "net.fabricmc.fabric-api")
//    }

    modRuntimeOnly(libs.yacl) {
        exclude(group = "net.fabricmc")
        exclude(group = "net.fabricmc.fabric-api")
    }
    modRuntimeOnly(libs.cardinalComponents.base) {
        exclude(group = "net.fabricmc")
        exclude(group = "net.fabricmc.fabric-api")
    }
    modRuntimeOnly(libs.cardinalComponents.entity) {
        exclude(group = "net.fabricmc")
        exclude(group = "net.fabricmc.fabric-api")
    }
    modRuntimeOnly(libs.cardinalComponents.world) {
        exclude(group = "net.fabricmc")
        exclude(group = "net.fabricmc.fabric-api")
    }
    include(libs.cardinalComponents.base)
    include(libs.cardinalComponents.entity)
    include(libs.cardinalComponents.world)
    modRuntimeOnly(libs.minecratTagSerializationLocal)
    include(libs.minecratTagSerializationLocal)

    common(project(":common", configuration = "namedElements")) { isTransitive = false }
    shadowCommon(project(":common", configuration = "transformProductionQuilt")) { isTransitive = false }
    common(project(":fabric-like", configuration = "namedElements")) { isTransitive = false }
    shadowCommon(project(":fabric-like", configuration = "transformProductionQuilt")) { isTransitive = false }

    modRuntimeOnly(libs.fabric.languageKotlin) {
        exclude(group = "net.fabricmc")
        exclude(group = "net.fabricmc.fabric-api")
    }
}

val javaComponent = components.getByName<AdhocComponentWithVariants>("java")
javaComponent.withVariantsFromConfiguration(configurations["sourcesElements"]) {
    skip()
}

tasks {
    processResources {
        inputs.property("id", rootProject.property("mod_id").toString())
        inputs.property("version", project.version)
        inputs.property("group", project.group)
        inputs.property("name", rootProject.property("mod_name").toString())
        inputs.property("description", rootProject.property("mod_description").toString())
        inputs.property("author", rootProject.property("mod_author").toString())
        inputs.property("source", rootProject.property("mod_source").toString())
        inputs.property("minecraft_version", libs.versions.minecraft.get())
        inputs.property("quilt_kotlin_libraries_version", libs.versions.quilt.kotlin.libraries.get())
        inputs.property("fabric_language_kotlin_version", libs.versions.fabric.language.kotlin.get())
        inputs.property("components", project(":fabric-like").extraProperties["components"])

        filesMatching("quilt.mod.json") {
            expand(
                "id" to rootProject.property("mod_id").toString(),
                "version" to project.version,
                "group" to project.group,
                "name" to rootProject.property("mod_name").toString(),
                "description" to rootProject.property("mod_description").toString(),
                "author" to rootProject.property("mod_author").toString(),
                "source" to rootProject.property("mod_source").toString(),
                "minecraft_version" to libs.versions.minecraft.get(),
                "fabric_language_kotlin_version" to libs.versions.fabric.language.kotlin.get(),
                "quilt_kotlin_libraries_version" to libs.versions.quilt.kotlin.libraries.get(),
                "components" to project(":fabric-like").extraProperties["components"],
            )
        }
    }

    shadowJar {
        exclude("architectury.common.json")
        configurations = listOf(project.configurations["shadowCommon"])
        archiveClassifier.set("dev-shadow")
    }

    remapJar {
        injectAccessWidener.set(true)
        inputFile.set(shadowJar.flatMap { it.archiveFile })
        dependsOn(shadowJar)
        archiveClassifier.set("quilt")
    }

    jar {
        archiveClassifier.set("dev")
    }

    sourcesJar {
        val commonSources = project(":common").tasks.getByName<Jar>("sourcesJar")
        dependsOn(commonSources)
        from(commonSources.archiveFile.map { zipTree(it) })
    }

    publishing {
        publications {
            create<MavenPublication>("mavenQuilt") {
                artifactId = "${rootProject.property("archives_base_name")}-${project.name}"
                from(javaComponent)
            }
        }

        // See https://docs.gradle.org/current/userguide/publishing_maven.html for information on how to set up publishing.
        repositories {
            // Add repositories to publish to here.
        }
    }
}
