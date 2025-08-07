plugins {
    alias(libs.plugins.kotlin.jvm)
}

group = "fureeish"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.dotenv)

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(18)
}