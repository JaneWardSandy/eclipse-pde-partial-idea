plugins {
  kotlin("jvm") version "2.1.20"

//  kotlin("kapt") version "1.9.0" apply false
  id("org.jetbrains.intellij.platform") version "2.5.0" apply false

//  id("org.openjfx.javafxplugin") version "0.0.14"
//  id("org.beryx.jlink") version "2.26.0"
}

group = "cn.varsa"
version = "1.6.7"

repositories {
  mavenLocal()
  mavenCentral()
}

dependencies {
  implementation(kotlin("stdlib"))
}

tasks.register("pack") {
  group = "partial"

  dependsOn(tasks.getByPath(":eclipse-pde-partial-idea:buildPlugin"))
//  dependsOn(tasks.getByPath(":wishes-launcher:jlinkZip"))
}

subprojects {
  group = rootProject.group
  version = rootProject.version

  repositories {
    mavenLocal()
    mavenCentral()
  }
}
