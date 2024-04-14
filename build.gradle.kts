plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.nmcp)
}

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "kotlin")
    apply(plugin = "com.gradleup.nmcp")

    kotlin {
        jvmToolchain(21)
    }

    dependencies {
        implementation(kotlin("stdlib"))
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    nmcp {
        publishAllProjectsProbablyBreakingProjectIsolation {
            username = System.getenv("CENTRAL_PORTAL_USERNAME")
            password = System.getenv("CENTRAL_PORTAL_PASSWORD")
            publicationType = "AUTOMATIC"
        }
    }
}
