plugins {
    alias(libs.plugins.shadow)
}

architectury {
    platformSetupLoomIde()
    forge()
}

val modId: String = rootProject.property("archives_base_name").toString()
loom {
    accessWidenerPath.set(project(":common").loom.accessWidenerPath)

    forge {
        convertAccessWideners.set(true)
        extraAccessWideners.add(loom.accessWidenerPath.get().asFile.name)
    }

    runs {
        create("data") {
            data()
            programArgs("--all", "--mod", "spawn_complex")
            programArgs("--existing", file("src/main/resources").absolutePath)
        }
    }
}

sourceSets {
    main {
        resources {
            srcDir("src/generated/resources")
        }
    }
}

/**
 * @see: https://docs.gradle.org/current/userguide/migrating_from_groovy_to_kotlin_dsl.html
 * */
val common: Configuration by configurations.creating
val shadowCommon: Configuration by configurations.creating // Don't use shadow from the shadow plugin because we don't want IDEA to index this.
val developmentForge: Configuration = configurations.getByName("developmentForge")
configurations {
    compileOnly.get().extendsFrom(configurations["common"])
    runtimeClasspath.get().extendsFrom(configurations["common"])
    developmentForge.extendsFrom(configurations["common"])
}

dependencies {
    forge(libs.forge)
    // Remove the next line if you don't want to depend on the API
//    modApi(libs.architectury.forge)
    common(project(":common", configuration = "namedElements")) { isTransitive = false }
    shadowCommon(project(":common", configuration = "transformProductionForge")) { isTransitive = false }
    implementation(libs.kotlinforforge)
}

repositories {
    maven {
        name = "Kotlin for Forge"
        url = uri("https://thedarkcolour.github.io/KotlinForForge/")
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
        inputs.property("minecraft_version", libs.versions.min.minecraft.get())
        inputs.property("forge_version", libs.versions.forge.get())
        inputs.property("kotlinforforge_version", libs.versions.kotlinforforge.get())

        duplicatesStrategy = DuplicatesStrategy.INCLUDE

        filesMatching("META-INF/mods.toml") {
            expand(
                "id" to rootProject.property("mod_id").toString(),
                "version" to project.version,
                "group" to project.group,
                "name" to rootProject.property("mod_name").toString(),
                "description" to rootProject.property("mod_description").toString(),
                "author" to rootProject.property("mod_author").toString(),
                "minecraft_version" to libs.versions.min.minecraft.get(),
                "forge_version" to libs.versions.forge.get(),
                "kotlinforforge_version" to libs.versions.kotlinforforge.get(),
            )
        }

        filesMatching("pack.meta") {
            expand(
                "name" to rootProject.property("mod_name").toString(),
            )
        }
    }

    shadowJar {
        exclude("fabric.mod.json")
        exclude("architectury.common.json")
        configurations = listOf(project.configurations["shadowCommon"])
        archiveClassifier.set("dev-shadow")
    }

    remapJar {
        inputFile.set(shadowJar.flatMap { it.archiveFile })
        dependsOn(shadowJar)
        archiveClassifier.set("forge")
    }

    jar {
        archiveClassifier.set("dev")
    }

    sourcesJar {
        val commonSources = project(":common").tasks.getByName<Jar>("sourcesJar")
        dependsOn(commonSources)
        from(commonSources.archiveFile.map { zipTree(it) })
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }

    publishing {
        publications {
            create<MavenPublication>("mavenForge") {
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
