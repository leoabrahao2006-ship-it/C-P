plugins {
    kotlin("jvm") version "1.9.22"
    application
}
repositories { mavenCentral() }
dependencies {
    implementation("io.javalin:javalin:6.1.3")
    implementation("org.slf4j:slf4j-simple:2.0.11")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.1")
}
application { mainClass.set("MainKt") }
