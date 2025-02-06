import org.jetbrains.intellij.platform.gradle.*

plugins {
  kotlin("jvm")
  id("org.jetbrains.intellij.platform")
}

repositories {
  mavenCentral()
  intellijPlatform {
    defaultRepositories()
  }
}

dependencies {
  implementation(project(":common"))
  intellijPlatform {
    intellijIdeaCommunity("2023.3")
    bundledPlugins(listOf("com.intellij.java", "org.jetbrains.kotlin"))
    plugins(emptyList())

    pluginVerifier()
    zipSigner()
    testFramework(TestFrameworkType.Platform)
  }
}

kotlin {
  jvmToolchain(17)
}

intellijPlatform {
  pluginConfiguration {
    version = project.version.toString()

    val projectPath = rootProject.projectDir.path
    description = File("$projectPath/DESCRIPTION.html").readText(Charsets.UTF_8)
    changeNotes = File("$projectPath/CHANGES.html").readText(Charsets.UTF_8)

    ideaVersion {
      sinceBuild = "233"
    }
  }

  publishing {
    token = providers.environmentVariable("PUBLISH_TOKEN")
  }

  pluginVerification {
    ides {
      recommended()
    }
  }
}

intellijPlatformTesting {
  runIde {
    register("runIdeForUiTests") {
      task {
        jvmArgumentProviders += CommandLineArgumentProvider {
          listOf(
            "-Drobot-server.port=8082",
            "-Dide.mac.message.dialogs.as.sheets=false",
            "-Djb.privacy.policy.text=<!--999.999-->",
            "-Djb.consents.confirmation.enabled=false",
          )
        }
      }

      plugins {
        robotServerPlugin()
      }
    }
  }
}
