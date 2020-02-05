plugins {
    java
    application
}

group = "com.example.zoldater"
version = "1.0-SNAPSHOT"

application {
    mainClassName = "com.example.zoldater.ServerApplication"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":Core"))
    implementation("org.tinylog", "tinylog-api", "2.0.1")
    implementation("org.tinylog", "tinylog-impl", "2.0.1")
    implementation("com.intellij:annotations:+@jar")
    testImplementation("junit", "junit", "4.12")
}