plugins {
  kotlin("jvm") version "1.7.10"

  kotlin("kapt") version "1.7.10" apply false
  id("org.jetbrains.intellij") version "1.7.0" apply false

  id("org.openjfx.javafxplugin") version "0.0.13" apply false
  id("org.beryx.jlink") version "2.25.0" apply false
}

group = "cn.varsa"
version = "2.0.0-SNAPSHOT"

repositories {
  mavenLocal()
  mavenCentral()
}

subprojects {
  group = rootProject.group
  version = rootProject.version

  apply {
    plugin("org.jetbrains.kotlin.jvm")
  }

  repositories {
    mavenLocal()
    mavenCentral()
  }

  dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
  }

  tasks {
    compileJava {
      sourceCompatibility = "11"
      targetCompatibility = "11"
    }
    compileTestJava {
      sourceCompatibility = "11"
      targetCompatibility = "11"
    }
    compileKotlin {
      kotlinOptions {
        jvmTarget = "11"
        apiVersion = "1.7"
        languageVersion = "1.7"
      }
    }
    compileTestKotlin {
      kotlinOptions {
        jvmTarget = "11"
        apiVersion = "1.7"
        languageVersion = "1.7"
      }
    }
  }
}
