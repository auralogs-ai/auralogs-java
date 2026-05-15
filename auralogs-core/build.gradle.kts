mavenPublishing {
    pom {
        name.set("Auralogs SDK core")
        description.set("Core SDK: static facade, transport, logger, error capture. Zero runtime dependencies.")
    }
}

dependencies {
    compileOnly("org.jspecify:jspecify:1.0.0")

    testImplementation("org.junit.jupiter:junit-jupiter:6.0.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj:assertj-core:3.27.2")
    testImplementation("org.wiremock:wiremock-standalone:3.9.2")
    testImplementation("org.awaitility:awaitility:4.2.2")
    testImplementation("org.jspecify:jspecify:1.0.0")
}
