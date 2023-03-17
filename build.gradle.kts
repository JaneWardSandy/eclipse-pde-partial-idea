plugins {
  kotlin("jvm") version "1.5.10"

  kotlin("kapt") version "1.5.10" apply false
  id("org.jetbrains.intellij") version "1.12.0" apply false
}

group = "cn.varsa"
version = "1.6.0.1-213"

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
}

subprojects {
  group = rootProject.group
  version = rootProject.version

  repositories {
    mavenLocal()
    mavenCentral()
  }
}
