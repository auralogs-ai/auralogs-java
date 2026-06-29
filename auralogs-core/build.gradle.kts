mavenPublishing {
    pom {
        name.set("Auralogs SDK core")
        description.set("Core SDK: static facade, transport, logger, error capture. Zero runtime dependencies.")
    }
}

dependencies {
    compileOnly("org.jspecify:jspecify:1.0.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj:assertj-core:3.27.7")
    testImplementation("org.wiremock:wiremock-standalone:3.13.2")
    testImplementation("org.awaitility:awaitility:4.3.0")
    testImplementation("org.jspecify:jspecify:1.0.0")
}
