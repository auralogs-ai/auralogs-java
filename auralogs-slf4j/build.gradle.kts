mavenPublishing {
    pom {
        name.set("Auralogs SLF4J bridge")
        description.set("SLF4J 2.0+ service provider that routes org.slf4j calls to the Auralogs core SDK.")
    }
}

dependencies {
    api(project(":auralogs-core"))
    implementation("org.slf4j:slf4j-api:2.0.16")
    compileOnly("org.jspecify:jspecify:1.0.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj:assertj-core:3.27.2")
    testImplementation("org.wiremock:wiremock-standalone:3.9.2")
    testImplementation("org.awaitility:awaitility:4.2.2")
    testImplementation("org.slf4j:slf4j-api:2.0.16")
}
