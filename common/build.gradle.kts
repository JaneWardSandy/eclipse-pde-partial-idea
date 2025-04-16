import org.jetbrains.kotlin.gradle.dsl.*

plugins {
  kotlin("jvm")
}

dependencies {
  compileOnly(kotlin("stdlib"))
  compileOnly(kotlin("reflect"))
}

tasks {
  compileJava {
    sourceCompatibility = "21"
    targetCompatibility = "21"
  }

  compileKotlin {
    compilerOptions {
      freeCompilerArgs = listOf("-Xjsr305=strict")
      jvmTarget.set(JvmTarget.JVM_21)
    }
  }
}
