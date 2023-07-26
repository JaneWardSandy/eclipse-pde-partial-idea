plugins {
  kotlin("jvm") version "1.9.0"

  kotlin("kapt") version "1.9.0" apply false
  id("org.jetbrains.intellij") version "1.15.0" apply false

  id("org.openjfx.javafxplugin") version "0.0.14"
  id("org.beryx.jlink") version "2.26.0"
}

group = "cn.varsa"
version = "1.6.3"

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
  dependsOn(tasks.getByPath(":wishes-launcher:jlinkZip"))
}

subprojects {
  group = rootProject.group
  version = rootProject.version

  repositories {
    mavenLocal()
    mavenCentral()
  }
}
