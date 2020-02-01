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
    implementation("org.apache.logging.log4j", "log4j-api", "2.13.0")
    implementation("org.apache.logging.log4j", "log4j-core", "2.13.0")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}