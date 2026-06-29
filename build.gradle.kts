plugins {
    id("com.diffplug.spotless") version "6.25.0" apply false
    id("com.vanniktech.maven.publish") version "0.37.0" apply false
}

allprojects {
    group = "ai.auralogs"
    version = "1.0.0"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    apply(plugin = "signing")
    apply(plugin = "com.diffplug.spotless")
    apply(plugin = "com.vanniktech.maven.publish")

    extensions.configure<JavaPluginExtension> {
        // Bytecode is targeted at Java 11 via `options.release.set(11)`. For local
        // dev, any JDK >= 11 works. CI pins exact versions via actions/setup-java.
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        // Sources + Javadoc jars are added by com.vanniktech.maven.publish, don't
        // add them here or we end up with two of each.
    }

    tasks.withType<JavaCompile>().configureEach {
        options.release.set(11)
        options.encoding = "UTF-8"
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
            showStandardStreams = false
        }
    }

    extensions.configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        java {
            googleJavaFormat("1.25.2")
            target("src/**/*.java")
            targetExclude("**/module-info.java", "**/package-info.java")
        }
    }

    extensions.configure<com.vanniktech.maven.publish.MavenPublishBaseExtension> {
        publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL)
        signAllPublications()

        pom {
            url.set("https://github.com/auralogs-ai/auralogs-java")
            licenses {
                license {
                    name.set("The MIT License")
                    url.set("https://opensource.org/licenses/MIT")
                    distribution.set("repo")
                }
            }
            developers {
                developer {
                    id.set("jamescethomas")
                    name.set("James Thomas")
                    email.set("james.c.e.thomas@gmail.com")
                }
            }
            scm {
                url.set("https://github.com/auralogs-ai/auralogs-java")
                connection.set("scm:git:git://github.com/auralogs-ai/auralogs-java.git")
                developerConnection.set("scm:git:ssh://github.com:auralogs-ai/auralogs-java.git")
            }
        }
    }
}
