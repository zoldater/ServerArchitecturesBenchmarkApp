plugins {
    java
    application
}

group = "com.example.zoldater"
version = "1.0-SNAPSHOT"

application {
    mainClassName = "com.example.zoldater.gui.ClientSwingForm"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":ClientCli"))
    implementation(project(":Core"))
    implementation("org.knowm.xchart", "xchart", "3.6.1")
    testCompile("junit", "junit", "4.12")
}