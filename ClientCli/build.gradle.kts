plugins {
    java
    application
}

group = "com.example.zoldater"
version = "1.0-SNAPSHOT"

application {
    mainClassName = "com.example.zoldater.ClientCliApplication"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":Core"))
    testImplementation("junit", "junit", "4.12")
    implementation("org.tinylog", "tinylog-api", "2.0.1")
    implementation("org.tinylog", "tinylog-impl", "2.0.1")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}