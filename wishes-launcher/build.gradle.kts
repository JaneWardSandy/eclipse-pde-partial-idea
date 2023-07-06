plugins {
  idea
  application
  id("org.openjfx.javafxplugin")
  id("org.beryx.jlink")
  kotlin("jvm")
  kotlin("kapt")
}

modularity.disableEffectiveArgumentsAdjustment() // https://github.com/java9-modularity/gradle-modules-plugin/issues/165

dependencies {
  implementation(kotlin("reflect"))
  implementation(kotlin("stdlib"))
  implementation(project(":common"))

  implementation("no.tornado:tornadofx:1.7.20") {
    exclude("org.jetbrains.kotlin")
  }

  implementation("org.slf4j:slf4j-api:2.0.0-alpha1")
  implementation("ch.qos.logback:logback-classic:1.3.0-beta0")
  implementation("org.slf4j:jcl-over-slf4j:2.0.0-alpha1")
  implementation("org.slf4j:jul-to-slf4j:2.0.0-alpha1")
  implementation("org.slf4j:log4j-over-slf4j:2.0.0-alpha1")
}

application {
  mainModule.set("Wishes")
  mainClass.set("cn.varsa.idea.pde.partial.launcher.MainLauncherKt")
}

javafx {
  version = "17.0.7"
  modules("javafx.controls", "javafx.fxml", "javafx.web")
}

jlink {
  addExtraDependencies("javafx")
  addOptions("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages")

  launcher {
    noConsole = false
    imageName.set("wishes-launcher")
  }

  mergedModule {
    additive = true
    requires("org.slf4j")
  }

  val targetPlatform: String by rootProject.extra
  val jdkHome: String by rootProject.extra
  val javaFxModulePath: String by rootProject.extra

  targetPlatform(targetPlatform) {
    setJdkHome(jdkHome)
    addExtraModulePath(javaFxModulePath)
  }
}

java {
  modularity.inferModulePath.set(true)
}

plugins.withType<JavaPlugin>().configureEach {
  configure<JavaPluginExtension> {
    modularity.inferModulePath.set(true)
  }
}

tasks {
  compileJava {
    sourceCompatibility = "17"
    targetCompatibility = "17"
    inputs.property("moduleName", "Wishes")
  }

  compileKotlin {
    kotlinOptions {
      freeCompilerArgs = listOf("-Xjsr305=strict")
      jvmTarget = "17"
      apiVersion = "1.8"
      languageVersion = "1.8"
    }
  }
}
