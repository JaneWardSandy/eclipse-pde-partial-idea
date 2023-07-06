package cn.varsa.idea.pde.partial.launcher.control

import cn.varsa.idea.pde.partial.common.*
import cn.varsa.idea.pde.partial.common.domain.*
import cn.varsa.idea.pde.partial.common.service.*
import cn.varsa.idea.pde.partial.common.support.*
import javafx.beans.property.*
import tornadofx.*
import java.io.*
import java.nio.charset.*
import java.util.jar.*

class ConfigControl : Controller(), ConfigService {
  val librariesProperty = mutableListOf<String>().asObservable()

  val javaExeProperty = SimpleStringProperty("")
  var javaExe: String by javaExeProperty

  val ideaCharsetProperty = SimpleObjectProperty(Charset.defaultCharset())
  var ideaCharset: Charset by ideaCharsetProperty

  val osCharsetProperty = SimpleObjectProperty(Charset.defaultCharset())
  var osCharset: Charset by osCharsetProperty

  val rmiRunningProperty = SimpleBooleanProperty(false)
  var rmiRunning by rmiRunningProperty

  val portalRunningProperty = SimpleBooleanProperty(false)
  var portalRunning by portalRunningProperty

  val jdwpRunningProperty = SimpleBooleanProperty(false)
  var jdwpRunning by jdwpRunningProperty


  val runtimeDirectoryProperty = SimpleStringProperty("")
  var runtimeDirectory: String by runtimeDirectoryProperty

  val dataDirectoryProperty = SimpleStringProperty("")
  var dataDirectory: String by dataDirectoryProperty

  val projectRootProperty = SimpleStringProperty("")
  var projectRoot: String by projectRootProperty

  val launcherProperty = SimpleStringProperty("")
  var launcher: String by launcherProperty

  val launcherJarProperty = SimpleStringProperty("")
  var launcherJar: String by launcherJarProperty

  val rmiPortProperty = SimpleIntegerProperty(7995)
  var rmiPort by rmiPortProperty

  val rmiNameProperty = SimpleStringProperty("WishesService")
  var rmiName: String by rmiNameProperty

  val rmiUrlProperty = stringBinding(rmiPortProperty, rmiNameProperty) { "rmi://127.0.0.1:$rmiPort/$rmiName" }

  val maxMemProperty = SimpleLongProperty(0)
  var maxMem by maxMemProperty

  val allocatedMemProperty = SimpleLongProperty(0)
  var allocatedMem by allocatedMemProperty

  val usedMemProperty = SimpleLongProperty(0)
  var usedMem by usedMemProperty

  val startupLevels = mutableMapOf<String, Int>()

  override var product: String = ""
  override var application: String = ""

  override val dataPath: File get() = File(dataDirectory)
  override val installArea: File
    get() = (launcher.takeIf(String::isNotBlank)?.toFile() ?: launcherJar.toFile().parentFile).parentFile

  override val projectDirectory: File get() = projectRoot.toFile()

  override val libraries: List<File>
    get() = librariesProperty.asSequence().filter(String::isNotBlank).map(::File).filter(File::exists)
      .filter(File::isDirectory).map { File(it, Plugins).takeIf(File::exists) ?: it }.mapNotNull(File::listFiles)
      .toList().flatMap { it.toList() }

  override val devModules: MutableList<DevModule> = mutableListOf()

  override fun getManifest(jarFileOrDirectory: File): BundleManifest? =
    if (jarFileOrDirectory.isFile && jarFileOrDirectory.extension.lowercase() == "jar") {
      JarFile(jarFileOrDirectory).use { it.manifest?.let(BundleManifest::parse) }
    } else {
      File(jarFileOrDirectory, ManifestPath).takeIf(File::exists)?.inputStream()?.use(::Manifest)
        ?.let(BundleManifest.Companion::parse)
    }

  override fun startUpLevel(bundleSymbolicName: String): Int = startupLevels[bundleSymbolicName] ?: 4
  override fun isAutoStartUp(bundleSymbolicName: String): Boolean = startupLevels.containsKey(bundleSymbolicName)
}
