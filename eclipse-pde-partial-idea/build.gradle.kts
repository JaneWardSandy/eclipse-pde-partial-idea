import java.io.FileNotFoundException

plugins {
  id("org.jetbrains.intellij")
}

dependencies {
  subprojects.forEach { pluginProject -> implementation(project(":$name:${pluginProject.name}")) }
}

intellij {
  type.set(findProperty("platformType").toString())
  version.set(findProperty("platformVersion").toString())
  plugins.set(findProperty("platformPlugins").toString().split(',').map(String::trim).filter(String::isNotBlank))
  downloadSources.set(true)
}

tasks {
  patchPluginXml {
    version.set(findProperty("pluginVersion").toString())
    sinceBuild.set(findProperty("pluginSinceBuild").toString())
    untilBuild.set(findProperty("pluginUntilBuild").toString())

    val description = file("plugin-description.html")
    if (!description.exists()) error("file not found: $description")
    pluginDescription.set(description.readText(Charsets.UTF_8))

    val versionChange = file("change-notes${File.separator}${project.version}.html")
    if (!versionChange.exists()) throw FileNotFoundException("file not found: $versionChange")
    changeNotes.set(versionChange.readText(Charsets.UTF_8))
  }
  publishPlugin {
    val ideaPluginToken: String by rootProject.extra
    token.set(ideaPluginToken)
  }
}

subprojects {
  apply {
    plugin("org.jetbrains.intellij")
  }

  intellij {
    type.set(findProperty("platformType").toString())
    version.set(findProperty("platformVersion").toString())
    updateSinceUntilBuild.set(false)
  }

  tasks {
    // disable runIde tasks in subprojects to prevent starting-up multiple ide.
    runIde {
      enabled = false
    }
  }
}