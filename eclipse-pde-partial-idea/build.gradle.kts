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
    setPlugins("java", "org.jetbrains.kotlin:211-1.4.32-release-IJ6693.72")
}

tasks {
    getByName<PatchPluginXmlTask>("patchPluginXml") {
        changeNotes(
            """
            Initial project, migrating
        """.trimIndent()
        )

        sinceBuild("203")
        untilBuild("")
    }

    compileJava {
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }

    compileKotlin {
        kotlinOptions {
            jvmTarget = "11"
        }
    }
}
