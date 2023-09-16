import org.jetbrains.kotlin.gradle.plugin.extraProperties

plugins {
    alias(libs.plugins.shadow)
    alias(libs.plugins.minotaur)
}

architectury {
    platformSetupLoomIde()
    fabric()
}

loom {
    accessWidenerPath.set(project(":common").loom.accessWidenerPath)

//    mods {
//        register(rootProject.property("mod_id").toString()) {
//            sourceSet("main")
//            sourceSet("main", project(":common"))
//            sourceSet("main", project(":fabric-like"))
//        }
//    }

    runs {
        named("client") {
            vmArgs("-Dmixin.debug.export=true")
            vmArgs("-Dmixin.debug.verbose=true")
            vmArgs("-Dmixin.hotSwap=true")
        }
    }
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
    exclusiveContent {
        forRepository {
            maven {
                name = "Modrinth"
                url = uri("https://api.modrinth.com/maven")
            }
        }
        filter {
            includeGroup("maven.modrinth")
        }
    }
    maven("https://maven.isxander.dev/releases") {
        name = "Xander Maven"
    }
    maven("https://maven.terraformersmc.com/releases") // Mod Menu. YACL need it
    maven {
        name = "Ladysnake Mods"
        url = uri("https://maven.ladysnake.org/releases")
    }
    maven("https://jitpack.io")
    mavenLocal()
}

dependencies {
    modImplementation(libs.fabric.loader)
    modApi(libs.fabric.api)
    // Remove the next line if you don't want to depend on the API
//    modApi(libs.architectury.fabric)

    common(project(":common", configuration = "namedElements")) { isTransitive = false }
    shadowCommon(project(":common", configuration = "transformProductionFabric")) { isTransitive = false }
    common(project(":fabric-like", configuration = "namedElements")) { isTransitive = false }
    shadowCommon(project(":fabric-like", configuration = "transformProductionFabric")) { isTransitive = false }

    modImplementation(libs.fabric.languageKotlin)

    modApi(libs.yacl) {
        exclude(group = "net.fabricmc")
        exclude(group = "net.fabricmc.fabric-api")
    }
    modApi(libs.cardinalComponents.base) {
        exclude(group = "net.fabricmc")
        exclude(group = "net.fabricmc.fabric-api")
    }
    modApi(libs.cardinalComponents.entity) {
        exclude(group = "net.fabricmc")
        exclude(group = "net.fabricmc.fabric-api")
    }
    modApi(libs.cardinalComponents.world) {
        exclude(group = "net.fabricmc")
        exclude(group = "net.fabricmc.fabric-api")
    }
    include(libs.cardinalComponents.base)
    include(libs.cardinalComponents.entity)
    include(libs.cardinalComponents.world)

    annotationProcessor(libs.mixin.extras)
    implementation(libs.mixin.extras)
    include(libs.mixin.extras)

    modApi(libs.kinecraft.serialization)
    include(libs.kinecraft.serialization)
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
        inputs.property("minecraft_version", libs.versions.min.minecraft.get())
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
                "minecraft_version" to libs.versions.min.minecraft.get(),
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

modrinth {
    token.set(System.getenv("MODRINTH_TOKEN")) // This is the default. Remember to have the MODRINTH_TOKEN environment variable set or else this will fail, or set it to whatever you want - just make sure it stays private!
    projectId.set("respawn-complex") // This can be the project ID or the slug. Either will work!
    syncBodyFrom.set(rootProject.file("README.md").readText())
    versionType.set("release") // This is the default -- can also be `beta` or `alpha`
    uploadFile.set(tasks.remapJar) // With Loom, this MUST be set to `remapJar` instead of `jar`!
    gameVersions.addAll("1.20.1", "1.20") // Must be an array, even with only one version
    changelog.set("feat: allow use bed for skipping night")
    loaders.add("fabric") // Must also be an array - no need to specify this if you're using Loom or ForgeGradle
    loaders.add("quilt")
    dependencies { // A special DSL for creating dependencies
        // scope.type
        // The scope can be `required`, `optional`, `incompatible`, or `embedded`
        // The type can either be `project` or `version`
        required.project("fabric-api") // Creates a new required dependency on Fabric API
        required.version("fabric-language-kotlin", libs.versions.fabric.language.kotlin.get())
        required.project("yacl")
        embedded.version("kinecraft-serialization", libs.versions.kinecraft.serialization.get())
        embedded.version("cardinal-components-api", libs.versions.cardinal.components.api.get())
        optional.project("waystones")
        optional.project("fwaystones")
    }
}
