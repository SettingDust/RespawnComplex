import org.jetbrains.kotlin.gradle.plugin.extraProperties

architectury {
    val enabled_platforms: String by rootProject
    common(enabled_platforms.split(","))
}

loom {
    accessWidenerPath.set(project(":common").loom.accessWidenerPath)
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
    api(libs.kotlinx.serialization.core)
    api(libs.kotlinx.serialization.json)

    api(libs.luckperms)

    modImplementation(libs.fabric.loader)
    modApi(libs.fabric.api)

    modApi(libs.yacl)
    modApi(libs.modmenu)

    val id = rootProject.property("mod_id").toString()
    project.extraProperties["components"] = arrayOf("complex_respawning", "complex_respawn_points").joinToString(", ") {
        "\"$id:$it\""
    }.removeSurrounding("\"")

    modApi(libs.cardinalComponents.base)
    modApi(libs.cardinalComponents.entity)
    modApi(libs.cardinalComponents.world)

    modApi(libs.kinecraft.serialization)

    compileOnly(kotlin("stdlib-jdk8"))
    compileOnly(project(":common", configuration = "namedElements")) { isTransitive = false }

    annotationProcessor(libs.mixin.extras)
    implementation(libs.mixin.extras)
}
