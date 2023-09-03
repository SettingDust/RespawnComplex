import net.fabricmc.loom.api.LoomGradleExtensionAPI
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    `kotlin-dsl`

    alias(libs.plugins.architectury.loom) apply false
    alias(libs.plugins.architectury.plugin)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.serialization)
}

architectury {
    minecraft = rootProject.libs.versions.minecraft.get()
}

subprojects {
    apply(plugin = "dev.architectury.loom")

    val loom = project.extensions.getByName<LoomGradleExtensionAPI>("loom")

    repositories {
        maven {
            name = "parchmentmc"
            url = uri("https://maven.parchmentmc.org")
        }
    }
    dependencies {
        "minecraft"(rootProject.libs.minecraft)
        "mappings"(
            loom.layered {
                officialMojangMappings()
                parchment(
                    variantOf(rootProject.libs.parchment) {
                        artifactType("zip")
                    },
                )
            },
        )
        // The following line declares the yarn mappings you may select this one as well.
        // "mappings"("net.fabricmc:yarn:1.19.2+build.3:v2")
    }
}

allprojects {
    apply(plugin = "java")
    apply(plugin = "architectury-plugin")
    apply(plugin = "maven-publish")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.serialization")

    base.archivesName.set(rootProject.property("archives_base_name").toString())
    // base.archivesBaseName = rootProject.property("archives_base_name").toString()
    version = rootProject.property("mod_version").toString()
    group = rootProject.property("maven_group").toString()

    repositories {
        // Add repositories to retrieve artifacts from in here.
        // You should only use this when depending on other mods because
        // Loom adds the essential maven repositories to download Minecraft and libraries from automatically.
        // See https://docs.gradle.org/current/userguide/declaring_repositories.html
        // for more information about repositories.
    }

    dependencies {
        compileOnly(rootProject.project.libs.kotlinGradlePlugin)
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(17)
    }

    java {
        withSourcesJar()
    }

    val compileKotlin: KotlinCompile by tasks
    compileKotlin.kotlinOptions {
        jvmTarget = "17"
    }
    val compileTestKotlin: KotlinCompile by tasks
    compileTestKotlin.kotlinOptions {
        jvmTarget = "17"
    }
}
