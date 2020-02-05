plugins {
    java
}

group = "com.example.zoldater"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project("ClientCli"))
    implementation(project("Server"))
    implementation("org.tinylog", "tinylog-api", "2.0.1")
    implementation("org.tinylog", "tinylog-impl", "2.0.1")
    testImplementation("junit", "junit", "4.12")
}
