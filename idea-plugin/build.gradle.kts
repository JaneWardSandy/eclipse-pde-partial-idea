import java.io.FileNotFoundException

plugins {
  id("org.jetbrains.intellij")
}

intellij {
  pluginName.set("eclipse-pde-partial-idea")
  version.set("2022.1.3")
  plugins.set(listOf("java", "org.jetbrains.kotlin"))
  downloadSources.set(true)
}

tasks {
  patchPluginXml {
    sinceBuild.set("221")
    untilBuild.set("")

    val description = file("description.html")
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
