plugins {
    id("java")
    id("com.gradleup.shadow")
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.momirealms.net/releases/")
    maven("https://jitpack.io/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    implementation("net.bytebuddy:byte-buddy:1.14.12")
    implementation("net.momirealms:sparrow-reflection:0.26")
    implementation("net.momirealms:sparrow-util:0.93")
    implementation("net.momirealms:sparrow-nbt:0.14")
    implementation("it.unimi.dsi:fastutil:8.5.18")
    implementation("com.google.guava:guava:33.0.0-jre")

    // Core and Bukkit proxies are still needed unless we rewrite all NMS proxies
    implementation(project(":bukkit:proxy"))
    implementation(project(":core"))
    implementation(project(":bukkit"))
}

tasks.shadowJar {
    archiveClassifier = ""
}
