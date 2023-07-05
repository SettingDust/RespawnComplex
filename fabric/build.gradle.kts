import org.jetbrains.kotlin.gradle.plugin.extraProperties

plugins {
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

architectury {
    platformSetupLoomIde()
    fabric()
}

loom {
    accessWidenerPath.set(project(":common").loom.accessWidenerPath)
}

/**
 * @see: https://docs.gradle.org/current/userguide/migrating_from_groovy_to_kotlin_dsl.html
 * */
val common: Configuration by configurations.creating
val shadowCommon: Configuration by configurations.creating // Don't use shadow from the shadow plugin because we don't want IDEA to index this.
val developmentFabric: Configuration = configurations.getByName("developmentFabric")
configurations {
    compileClasspath { extendsFrom(configurations["common"]) }
    runtimeClasspath { extendsFrom(configurations["common"]) }
    developmentFabric.extendsFrom(configurations["common"])
}

repositories {
    maven("https://maven.isxander.dev/releases") {
        name = "Xander Maven"
    }
    maven("https://maven.terraformersmc.com/releases") // Mod Menu. YACL need it
    maven("https://ladysnake.jfrog.io/artifactory/mods")
    maven("https://jitpack.io")
    mavenLocal()
}

dependencies {
    modImplementation(libs.fabric.loader)
//    modApi(libs.fabric.api)
    // Remove the next line if you don't want to depend on the API
//    modApi(libs.architectury.fabric)

    common(project(":common", configuration = "namedElements")) { isTransitive = false }
    shadowCommon(project(":common", configuration = "transformProductionFabric")) { isTransitive = false }
    common(project(":fabric-like", configuration = "namedElements")) { isTransitive = false }
    shadowCommon(project(":fabric-like", configuration = "transformProductionFabric")) { isTransitive = false }

    modImplementation(libs.fabric.languageKotlin)

    annotationProcessor(libs.mixin.extras)
    implementation(libs.mixin.extras)
    include(libs.mixin.extras)
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
        inputs.property("fabric_language_kotlin_version", libs.versions.fabric.language.kotlin.get())
        inputs.property("components", project(":fabric-like").extraProperties["components"])

        filesMatching("fabric.mod.json") {
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
        archiveClassifier.set("fabric")
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
            create<MavenPublication>("mavenFabric") {
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
