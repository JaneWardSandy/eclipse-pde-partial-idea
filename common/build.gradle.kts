plugins {
  kotlin("jvm")
}

dependencies {
  compileOnly(kotlin("stdlib"))
  compileOnly(kotlin("reflect"))
}

tasks {
  compileJava {
    sourceCompatibility = "11"
    targetCompatibility = "11"
  }

  compileKotlin {
    kotlinOptions {
      freeCompilerArgs = listOf("-Xjsr305=strict")
      jvmTarget = "11"
    }
  }
}
