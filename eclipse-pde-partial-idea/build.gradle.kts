import org.jetbrains.intellij.tasks.*

plugins {
    kotlin("jvm")
    id("org.jetbrains.intellij")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(project(":common"))
}

intellij {
    version = "2021.1.1"
    setPlugins("java", "org.jetbrains.kotlin")

    downloadSources = true
}

tasks {
    getByName<PatchPluginXmlTask>("patchPluginXml") {
        changeNotes(
            """
            <p><b>Fixed:</b></p>
            <ul>
            <li>Module in same project was required in another module, but not add into dependency tree.</li>
            <li>Module in same project was required in another module, but reexport not resolve. Module reexport bundle not pass to another module #3 <a href="https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/3" target="_blank">issue</a></li>
            <li>Manifest bundle reference in same project, linked to wrong module</li>
            <li>Kotlin's property ext access inspection not work #4 <a href="https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/4" target="_blank">issue</a></li>
            <li>Kotlin project alert kotlin bundle not required #5 <a href="https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/5" target="_blank">issue</a></li>
            </ul>
        """.trimIndent()
        )

        sinceBuild("203")
        untilBuild("")
    }

    publishPlugin {
        val ideaPluginToken: String by rootProject.extra
        token(ideaPluginToken)
    }

    runIde {
        jvmArgs("--add-exports", "java.base/jdk.internal.vm=ALL-UNNAMED")
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
            apiVersion = "1.5"
            languageVersion = "1.5"
        }
    }
}
