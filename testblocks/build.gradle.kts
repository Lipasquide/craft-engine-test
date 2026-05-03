plugins {
    id("java")
    id("com.gradleup.shadow")
}

group = "me.testblocks"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    implementation("net.bytebuddy:byte-buddy:1.14.12")
    compileOnly("io.netty:netty-all:4.1.100.Final")
}

tasks.shadowJar {
    archiveClassifier.set("")
    relocate("net.bytebuddy", "me.testblocks.libs.bytebuddy")
}
