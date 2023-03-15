plugins {
  kotlin("jvm")
  id("org.jetbrains.intellij")
}

dependencies {
  implementation(project(":common"))
}

intellij {
  version.set("2022.3.3")
  plugins.set(listOf("java", "org.jetbrains.kotlin"))

  downloadSources.set(true)
}

tasks {
  patchPluginXml {
    sinceBuild.set("223")
    untilBuild.set("")

    val projectPath = rootProject.projectDir.path
    pluginDescription.set(File("$projectPath/DESCRIPTION.html").readText(Charsets.UTF_8))
    changeNotes.set(File("$projectPath/CHANGES.html").readText(Charsets.UTF_8))
  }

  publishPlugin {
    token.set(System.getenv("PUBLISH_TOKEN"))
  }

  runIde {
    jvmArgs("-Xmx4096m", "--add-exports", "java.base/jdk.internal.vm=ALL-UNNAMED")
  }

  buildSearchableOptions {
    jvmArgs("--add-exports", "java.base/jdk.internal.vm=ALL-UNNAMED")
  }

  compileJava {
    sourceCompatibility = "17"
    targetCompatibility = "17"
  }

  compileKotlin {
    kotlinOptions {
      jvmTarget = "17"
      apiVersion = "1.7"
      languageVersion = "1.7"
    }
  }
}
