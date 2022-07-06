import org.jetbrains.kotlin.gradle.tasks.*

plugins {
  idea
  application
  id("org.openjfx.javafxplugin")
  id("org.beryx.jlink")
  kotlin("jvm")
  kotlin("kapt")
}

val compileKotlin: KotlinCompile by tasks
val compileJava: JavaCompile by tasks
compileJava.destinationDir = compileKotlin.destinationDir

modularity.disableEffectiveArgumentsAdjustment() // https://github.com/java9-modularity/gradle-modules-plugin/issues/165

dependencies {
  implementation(kotlin("reflect"))
  implementation(kotlin("stdlib"))
  implementation(project(":common"))

  implementation("no.tornado:tornadofx:1.7.20") {
    exclude("org.jetbrains.kotlin")
  }

  implementation("org.slf4j:slf4j-api:2.0.0-alpha1")
  implementation("ch.qos.logback:logback-classic:1.3.0-alpha4")
  implementation("org.slf4j:jcl-over-slf4j:2.0.0-alpha1")
  implementation("org.slf4j:jul-to-slf4j:2.0.0-alpha1")
  implementation("org.slf4j:log4j-over-slf4j:2.0.0-alpha1")
}

application {
  mainModule.set("Wishes")
  mainClass.set("cn.varsa.idea.pde.partial.launcher.MainLauncherKt")
}

javafx {
  version = "11"
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
    sourceCompatibility = "11"
    targetCompatibility = "11"
    inputs.property("moduleName", "Wishes")
  }

  compileKotlin {
    kotlinOptions {
      freeCompilerArgs = listOf("-Xjsr305=strict")
      jvmTarget = "11"
      apiVersion = "1.6"
      languageVersion = "1.6"
    }
  }
}
