plugins {
  kotlin("jvm") version "1.7.10"

  kotlin("kapt") version "1.7.10" apply false
  id("org.jetbrains.intellij") version "1.9.0" apply false

  id("org.openjfx.javafxplugin") version "0.0.11" apply false
  id("org.beryx.jlink") version "2.24.4" apply false
}

group = "cn.varsa"
version = "1.5.0"

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
