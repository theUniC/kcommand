plugins {
    alias(libs.plugins.nmcp)
}

dependencies {
    implementation(libs.kotlinx.coroutines)
    implementation(libs.arrow.core)
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
}

nmcp {
    publishAllPublications {}
}
