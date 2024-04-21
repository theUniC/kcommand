plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.dokka)
    `maven-publish`
}

allprojects {
    version = "0.1.0"
    group = "io.github.theunic"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "kotlin")
    apply(plugin = "maven-publish")
    apply(plugin = "org.jetbrains.dokka")

    kotlin {
        jvmToolchain(21)
    }

    dependencies {
        implementation(kotlin("stdlib"))
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    publishing {
        publications {
            create<MavenPublication>("maven") {
                groupId = project.group.toString()
                artifactId = project.name
                version = project.version.toString()
                from(components["java"])
            }
        }
    }
}
