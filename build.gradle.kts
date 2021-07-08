plugins {
    kotlin("jvm") version "1.5.10"

    kotlin("kapt") version "1.5.10" apply false
    id("org.jetbrains.intellij") version "0.7.3" apply false

    id("org.openjfx.javafxplugin") version "0.0.10" apply false
    id("org.beryx.jlink") version "2.24.0" apply false
}

group = "cn.varsa"
version = "1.2.2-Snapshot-1"

repositories {
    mavenLocal()
    maven("https://maven.aliyun.com/repository/public")
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
}

tasks.register("pack") {
    group = "partial"

    dependsOn(tasks.getByPath(":eclipse-pde-partial-idea:buildPlugin"))
    dependsOn(tasks.getByPath(":wishes-launcher:jlinkZip"))
}

subprojects {
    group = rootProject.group
    version = rootProject.version

    repositories {
        mavenLocal()
        maven("https://maven.aliyun.com/repository/public")
        mavenCentral()
    }
}
