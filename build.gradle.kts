import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.vanniktech)
}

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "kotlin")
    apply(plugin = "com.vanniktech.maven.publish")

    kotlin {
        jvmToolchain(21)
    }

    dependencies {
        implementation(kotlin("stdlib"))
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    mavenPublishing {
        val artifactId = project.properties["name"].toString()
        coordinates(rootProject.properties["groupId"].toString(), artifactId, rootProject.properties["version"].toString())

        pom {
            name.set(artifactId)
            description.set(project.properties["description"].toString())
            inceptionYear.set("2024")
            url.set("https://github.com/theUniC/kcommand/")
            licenses {
                license {
                    name.set("The MIT License")
                    url.set("https://opensource.org/license/mit")
                    distribution.set("https://opensource.org/license/mit")
                }
            }
            developers {
                developer {
                    id.set("theUniC")
                    name.set("Christian Soronellas")
                    url.set("https://github.com/theUniC/")
                }
            }
            scm {
                url.set("https://github.com/theUniC/kcommand/")
                connection.set("scm:git:git://github.com/theUniC/kcommand.git")
                developerConnection.set("scm:git:ssh://git@github.com/theUniC/kcommand.git")
            }
        }
    }
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.DEFAULT)
    signAllPublications()
}
