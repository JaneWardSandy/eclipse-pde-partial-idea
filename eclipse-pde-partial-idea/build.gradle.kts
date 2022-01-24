plugins {
    kotlin("jvm")
    id("org.jetbrains.intellij")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(project(":common"))
}

intellij {
    version.set("2021.3.1")
    plugins.set(listOf("java", "org.jetbrains.kotlin"))

    downloadSources.set(true)
}

tasks {
    patchPluginXml {
        sinceBuild.set("213")
        untilBuild.set("")

        val projectPath = rootProject.projectDir.path
        pluginDescription.set(File("$projectPath/DESCRIPTION.html").readText(Charsets.UTF_8))
        changeNotes.set(File("$projectPath/CHANGES.html").readText(Charsets.UTF_8))
    }

    publishPlugin {
        val ideaPluginToken: String by rootProject.extra
        token.set(ideaPluginToken)
    }

    runIde {
        jvmArgs("-Xmx4096m", "--add-exports", "java.base/jdk.internal.vm=ALL-UNNAMED")
    }

    buildSearchableOptions {
        jvmArgs("--add-exports", "java.base/jdk.internal.vm=ALL-UNNAMED")
    }

    compileJava {
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }

    compileKotlin {
        kotlinOptions {
            jvmTarget = "11"
            apiVersion = "1.6"
            languageVersion = "1.6"
        }
    }
}
