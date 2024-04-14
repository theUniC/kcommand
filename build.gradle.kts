import java.net.URI

plugins {
    alias(libs.plugins.kotlin)
    id("maven-publish")
}

allprojects {
    apply(plugin = "kotlin")
    apply(plugin = "maven-publish")

    repositories {
        mavenCentral()
    }

    kotlin {
        jvmToolchain(21)
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}

subprojects {
    publishing {
        repositories {
            maven {
                name = "OSSRH"
                url = URI("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
                credentials {
                    username = System.getenv("MAVEN_USERNAME")
                    password = System.getenv("MAVEN_PASSWORD")
                }
            }
            maven {
                name = "GitHubPackages"
                url = URI("https://maven.pkg.github.com/theunic/kcommand")
                credentials {
                    username = System.getenv("GITHUB_ACTOR")
                    password = System.getenv("GITHUB_TOKEN")
                }
            }
        }
    }
}
