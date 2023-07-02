plugins {
    `maven-publish`
}

architectury {
    val enabled_platforms: String by rootProject
    common(enabled_platforms.split(","))
}

loom {
    accessWidenerPath.set(file("src/main/resources/${rootProject.property("mod_id")}.accesswidener"))
}

dependencies {
    // We depend on fabric loader here to use the fabric @Environment annotations and get the mixin dependencies
    // Do NOT use other classes from fabric loader
    modImplementation(libs.fabric.loader)
    // Remove the next line if you don't want to depend on the API
//    modApi(libs.architectury)
    compileOnly(kotlin("stdlib-jdk8"))
    api(libs.kotlinx.serialization.core)
    api(libs.kotlinx.serialization.json)

    api(libs.luckperms)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = rootProject.property("archives_base_name").toString()
            from(components.getByName("java"))
        }
    }

    // See https://docs.gradle.org/current/userguide/publishing_maven.html for information on how to set up publishing.
    repositories {
        // Add repositories to publish to here.
    }
}
