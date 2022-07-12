import org.jetbrains.kotlin.gradle.tasks.*

plugins {
  kotlin("jvm") version "1.7.10"

  kotlin("kapt") version "1.7.10" apply false
  id("org.jetbrains.intellij") version "1.7.0" apply false

  id("org.openjfx.javafxplugin") version "0.0.13" apply false
  id("org.beryx.jlink") version "2.25.0" apply false
}

group = "cn.varsa"
version = findProperty("pluginVersion").toString()

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
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
  }

  tasks {
    val javaSettings: JavaCompile.() -> Unit = {
      sourceCompatibility = findProperty("javaVersion").toString()
      targetCompatibility = findProperty("javaVersion").toString()
      options.encoding = "UTF-8"
    }
    val kotlinSettings: KotlinCompile.() -> Unit = {
      kotlinOptions {
        jvmTarget = findProperty("javaVersion").toString()
        apiVersion = "1.7"
        languageVersion = "1.7"
        freeCompilerArgs = listOf(
          "-Xno-call-assertions",
          "-Xno-receiver-assertions",
          "-Xno-param-assertions",
          "-Xjvm-default=all",
          "-Xallow-kotlin-package",
          "-opt-in=kotlin.ExperimentalStdlibApi",
          "-opt-in=kotlin.ExperimentalUnsignedTypes",
          "-opt-in=kotlin.contracts.ExperimentalContracts",
          "-XXLanguage:+InlineClasses",
          "-XXLanguage:+UnitConversion"
        )
      }
    }

    compileJava(javaSettings)
    compileTestJava(javaSettings)

    compileKotlin(kotlinSettings)
    compileTestKotlin(kotlinSettings)
  }
}