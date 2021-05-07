plugins {
    kotlin("jvm") version "1.4.32"

    kotlin("kapt") version "1.4.32" apply false
    id("org.jetbrains.intellij") version "0.7.3" apply false

    id("org.openjfx.javafxplugin") version "0.0.9" apply false
    id("org.beryx.jlink") version "2.23.7" apply false
}

group = "cn.varsa"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenLocal()
    maven("https://maven.aliyun.com/repository/public")
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
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
