plugins {
    id("java")
    id("com.gradleup.shadow")
}

group = "me.testblocks"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.momirealms.net/releases/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    // For standalone, we need to bundle ByteBuddy and Sparrow or use direct reflection
    implementation("net.bytebuddy:byte-buddy:1.14.12")
    implementation("net.momirealms:sparrow-reflection:0.26")
    implementation("net.momirealms:sparrow-util:0.93")
}

tasks.shadowJar {
    archiveClassifier.set("")
    // Relocate dependencies to avoid conflicts
    relocate("net.bytebuddy", "me.testblocks.libs.bytebuddy")
    relocate("net.momirealms.sparrow", "me.testblocks.libs.sparrow")
}
