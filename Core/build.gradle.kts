import com.google.protobuf.gradle.*

plugins {
    java
    idea
    id("com.google.protobuf") version "0.8.11"

}

group = "com.example.zoldater"
version = "1.0-SNAPSHOT"

buildscript {
    repositories {
        maven("https://plugins.gradle.org/m2/")
    }
    dependencies {
        classpath("com.google.protobuf", "protobuf-gradle-plugin", "0.8.11")
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.tinylog", "tinylog-api", "2.0.1")
    implementation("org.tinylog", "tinylog-impl", "2.0.1")
    implementation("com.google.protobuf:protobuf-java:3.9.2")
    implementation("io.grpc:grpc-stub:1.27.0")
    implementation("io.grpc:grpc-protobuf:1.27.0")
    if (JavaVersion.current().isJava9Compatible) {
        // Workaround for @javax.annotation.Generated
        // see: https://github.com/grpc/grpc-java/issues/3633
        implementation("javax.annotation:javax.annotation-api:1.3.1")
    }
    testImplementation("junit:junit:4.12")
}

sourceSets {
    main {
        proto {
            srcDir("src/main/proto")
        }
        java {
            srcDirs("src/main/java", "generated-sources/main/java")
        }
    }
    test {
        proto {
            srcDir("src/test/proto")
        }
        proto {
            srcDir("src/test/java")
        }
    }
}


protobuf {
    // Configure the protoc executable
    protoc {
        // The artifact spec for the Protobuf Compiler
        artifact = "com.google.protobuf:protoc:3.9.2"
    }
    plugins {
        // Optional: an artifact spec for a protoc plugin, with "grpc" as
        // the identifier, which can be referred to in the "plugins"
        // container of the "generateProtoTasks" closure.
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.27.0"
        }
    }
    generateProtoTasks {
        generatedFilesBaseDir = "generated-sources"
    }

    generateProtoTasks {
        ofSourceSet("main").forEach {
            it.plugins {
                // Apply the "grpc" plugin whose spec is defined above, without options.
                id("grpc")
            }
        }
    }
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_11
}