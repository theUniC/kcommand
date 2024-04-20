description =
    """
    This library offers a scalable Message Bus implementation, enabling loose coupling for seamless system integration.
    It emphasizes the use of middlewares for customized message handling, ensuring flexibility in distributed architectures.
    Its user-friendly design allows developers to focus more on core functionalities, entrusting communication orchestration
    to the Message Bus implementations.
    """.trimIndent().replace("\n", " ")

dependencies {
    implementation(libs.kotlinx.coroutines)
    implementation(libs.arrow.core)
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
}
