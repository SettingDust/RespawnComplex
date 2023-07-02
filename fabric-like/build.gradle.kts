import org.jetbrains.kotlin.gradle.plugin.extraProperties

architectury {
    val enabled_platforms: String by rootProject
    common(enabled_platforms.split(","))
}

loom {
    accessWidenerPath.set(project(":common").loom.accessWidenerPath)
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
    api(libs.kotlinx.serialization.core)
    api(libs.kotlinx.serialization.json)

    api(libs.luckperms)

    modImplementation(libs.fabric.loader)
    modCompileOnly(libs.fabric.api)

    modCompileOnly(libs.yacl)
    modCompileOnly(libs.modmenu)

    val id = rootProject.property("mod_id").toString()
    project.extraProperties["components"] = arrayOf("complex_respawning", "complex_respawn_points").joinToString(", ") {
        "\"$id:$it\""
    }.removeSurrounding("\"")

    modCompileOnly(libs.cardinalComponents.base)
    modCompileOnly(libs.cardinalComponents.entity)
    modCompileOnly(libs.cardinalComponents.world)

    modCompileOnly(libs.minecratTagSerializationLocal)

    compileOnly(kotlin("stdlib-jdk8"))
    compileOnly(project(":common", configuration = "namedElements")) { isTransitive = false }
}