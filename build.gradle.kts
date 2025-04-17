plugins {
  id("java") // Java support
  alias(libs.plugins.kotlin) // Kotlin support

  id("org.jetbrains.intellij.platform") version "2.5.0" apply false

  // wishes-launcher deprecated
//  kotlin("kapt") version "1.9.0" apply false
//  id("org.openjfx.javafxplugin") version "0.0.14"
//  id("org.beryx.jlink") version "2.26.0"
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

repositories {
  mavenLocal()
  mavenCentral()
}

tasks {
  register("pack") {
    group = "partial"

    dependsOn(getByPath(":eclipse-pde-partial-idea:buildPlugin"))
  }

  wrapper {
    gradleVersion = providers.gradleProperty("gradleVersion").get()
  }
}

subprojects {
  group = rootProject.group
  version = rootProject.version

  repositories {
    mavenLocal()
    mavenCentral()
  }
}
