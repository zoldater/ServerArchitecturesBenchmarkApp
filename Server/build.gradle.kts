plugins {
    java
    application
}

group = "com.example.zoldater"
version = "1.0-SNAPSHOT"

application {
    mainClassName = "com.example.zoldater.ServerApplication"
    applicationDefaultJvmArgs = listOf(
            "-Dlog4j.rootLogger=info,stdout",
            "-Dlog4j.appender.stdout=org.apache.log4j.ConsoleAppender"
    )
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":Core"))
    implementation("org.apache.logging.log4j", "log4j-api", "2.13.0")
    implementation("org.apache.logging.log4j", "log4j-core", "2.13.0")
    implementation("com.intellij:annotations:+@jar")
    testImplementation("junit", "junit", "4.12")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}