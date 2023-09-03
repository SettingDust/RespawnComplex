import org.jetbrains.kotlin.gradle.plugin.extraProperties

plugins {
    alias(libs.plugins.shadow)
    alias(libs.plugins.minotaur)
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
    maven("https://maven.quiltmc.org/repository/release/")
    maven("https://maven.isxander.dev/releases") {
        name = "Xander Maven"
    }
    maven("https://maven.terraformersmc.com/releases") // Mod Menu. YACL need it
    maven {
        name = "Ladysnake Mods"
        url = uri("https://maven.ladysnake.org/releases")
    }
    maven("https://jitpack.io")
    mavenCentral()
    mavenLocal()
}

architectury {
    platformSetupLoomIde()
    loader("quilt")
}

loom {
    accessWidenerPath.set(project(":common").loom.accessWidenerPath)

    runs {
        getByName("client") {
            vmArg("-Dloader.workaround.disable_strict_parsing=true")
            vmArg("-Dmixin.hotSwap=true")
            vmArg("-Dmixin.checks.interfaces=true")
            vmArg("-Dmixin.debug.export=true")
            vmArg("-Dmixin.debug.verbose=true")
        }
    }
}

/**
 * @see: https://docs.gradle.org/current/userguide/migrating_from_groovy_to_kotlin_dsl.html
 * */
val common: Configuration by configurations.creating
val shadowCommon: Configuration by configurations.creating
val developmentQuilt: Configuration by configurations.getting

configurations {
    compileOnly.configure { extendsFrom(common) }
    runtimeOnly.configure { extendsFrom(common) }
    developmentQuilt.extendsFrom(common)
}

dependencies {
    modImplementation(libs.quilt.loader)
    modApi(libs.quilt.fabricApi) {
        exclude(group = "org.quiltmc")
    }
//    modApi(libs.quilt.standardLibraries) {
//        exclude(group = "org.quiltmc")
//    }
    // Remove the next few lines if you don't want to depend on the API
//    modApi(libs.architectury.fabric) {
//        // We must not pull Fabric Loader from Architectury Fabric
//        exclude(group = "net.fabricmc")
//        exclude(group = "net.fabricmc.fabric-api")
//    }

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

    modApi(libs.kinecraft.serialization)
    include(libs.kinecraft.serialization)

    common(project(":common", "namedElements")) {
        isTransitive = false
    }
    shadowCommon(project(":common", "transformProductionQuilt")) {
        isTransitive = false
    }
    common(project(":fabric-like", "namedElements")) {
        isTransitive = false
    }
    shadowCommon(project(":fabric-like", "transformProductionQuilt")) {
        isTransitive = false
    }

    modApi(libs.fabric.languageKotlin) {
        exclude(group = "net.fabricmc")
        exclude(group = "net.fabricmc.fabric-api")
    }

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
        inputs.property("minecraft_version", libs.versions.min.minecraft.get())
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
                "minecraft_version" to libs.versions.min.minecraft.get(),
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
