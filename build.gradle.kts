plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.15.0"
}

group = "com.promptly"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

// Define the IntelliJ IDEA dependency
intellij {
    version.set("2023.1.4")
    type.set("IC") // IntelliJ IDEA Community Edition
    plugins.set(listOf())
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2") // For JSON handling
    implementation("com.squareup.okhttp3:okhttp:4.10.0") // For HTTP requests to LLM APIs
}

tasks {
    // Set the JVM compatibility version
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    patchPluginXml {
        sinceBuild.set("231") // IntelliJ Platform 2023.1+
        untilBuild.set("233.*") // Supports up to 2023.3.x
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
} 