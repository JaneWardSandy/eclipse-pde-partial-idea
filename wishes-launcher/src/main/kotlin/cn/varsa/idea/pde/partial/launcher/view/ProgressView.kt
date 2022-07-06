package cn.varsa.idea.pde.partial.launcher.view

import cn.varsa.idea.pde.partial.common.*
import cn.varsa.idea.pde.partial.launcher.*
import cn.varsa.idea.pde.partial.launcher.control.*
import cn.varsa.idea.pde.partial.launcher.service.*
import cn.varsa.idea.pde.partial.launcher.support.*
import javafx.beans.binding.*
import javafx.scene.*
import tornadofx.*
import java.net.*
import java.rmi.registry.*
import java.rmi.server.*
import java.util.*
import kotlin.concurrent.*

class ProgressView : View("Run") {
  private val configControl: ConfigControl by inject()
  private val wishesControl: WishesControl by inject()
  private val loggerControl: LoggerControl by inject()

  private val logger = thisLogger()

  private val runningIcon = resources.imageview("/running.png")
  private val stoppedIcon = resources.imageview("/stopped.png")
  private val launchedIcon = resources.imageview("/launched.png")
  private val destroyedIcon = resources.imageview("/destroyed.png")
  private val listeningIcon = resources.imageview("/listening.png")
  private val tuneOutIcon = resources.imageview("/tune-out.png")

  private val hostIPProperty = stringProperty("")
  private var hostIP: String by hostIPProperty

  private var timer: Timer? = null
  private var wishesServiceImpl: WishesServiceImpl? = null

  override val complete: BooleanExpression = booleanProperty(true)
  override val root: Parent = form {
    fieldset("Overview") {
      field("Runtime directory") { label(configControl.runtimeDirectoryProperty) }
      field("Project root") { label(configControl.projectRootProperty) }
      field("RMI naming") { label(configControl.rmiUrlProperty) }
      field("IP address") { label(hostIPProperty) }
    }
    fieldset("Status") {
      field("RMI Service") {
        label(configControl.rmiRunningProperty.stringBinding { if (true == it) "Running" else "Stopped" }) {
          graphicProperty().bind(configControl.rmiRunningProperty.objectBinding { if (true == it) runningIcon else stoppedIcon })
        }
      }
      field("Portal") {
        label(configControl.portalRunningProperty.stringBinding { if (true == it) "Launched" else "Destroyed" }) {
          graphicProperty().bind(configControl.portalRunningProperty.objectBinding { if (true == it) launchedIcon else destroyedIcon })
        }
      }
      field("Java Debug Wire Protocol") {
        label(configControl.jdwpRunningProperty.stringBinding { if (true == it) "Listening" else "Tune-out" }) {
          graphicProperty().bind(configControl.jdwpRunningProperty.objectBinding { if (true == it) listeningIcon else tuneOutIcon })
        }
      }
    }
    fieldset("Memory Usage") {
      field("Max Heap Size") { label(configControl.maxMemProperty.asString("%,dM")) }
      field("Allocated") { label(configControl.allocatedMemProperty.asString("%,dM")) }
      field("Used") { label(configControl.usedMemProperty.asString("%,dM")) }
    }
  }

  override fun onDock() {
    super.onDock()

    hostIP = NetworkInterface.getNetworkInterfaces().asSequence().flatMap { it.inetAddresses.asSequence() }
      .mapNotNull { it as? Inet4Address }.mapNotNull { it.hostAddress }
      .filterNot { it == "127.0.0.1" || it == "0.0.0.0" }.joinToString()

    subscribe<LauncherStartEvent> {
      loggerControl.initialAppender()

      try {
        wishesServiceImpl = WishesServiceImpl(wishesControl, configControl)
        val registry: Registry = try {
          LocateRegistry.createRegistry(configControl.rmiPort)
        } catch (e: Exception) {
          LocateRegistry.getRegistry(configControl.rmiPort)
        }

        registry.rebind(configControl.rmiName, wishesServiceImpl)
        configControl.rmiRunning = true
      } catch (e: Exception) {
        logger.error(e.message, e)
        error("Exception", "Binding RMI Exception: ${e.message}")
      }
    }
    subscribe<LauncherStopEvent> {
      loggerControl.destroyAppender()

      try {
        wishesControl.destroy()
        configControl.portalRunning = false
      } catch (e: Exception) {
        logger.error(e.message, e)
        error("Exception", "Destroy Portal Exception: ${e.message}")
      }

      if (wishesServiceImpl != null) {
        try {
          LocateRegistry.getRegistry(configControl.rmiPort).unbind(configControl.rmiName)
          logger.info("Unbind ${configControl.rmiName}")
        } catch (e: Exception) {
          logger.warn("Unbind: ${e.message}", e)
        }
        try {
          UnicastRemoteObject.unexportObject(wishesServiceImpl, true)
          logger.info("UnExportObject WishesService")
        } catch (e: Exception) {
          logger.warn("UnExportObject: ${e.message}", e)
        }
        wishesServiceImpl = null
      }
      configControl.rmiRunning = false
    }

    timer = fixedRateTimer("MemoryUsageUpdater", true, 100, 1000) {
      Runtime.getRuntime().run {
        val maxMen = maxMemory() / MEGABYTE
        val allocatedMen = totalMemory() / MEGABYTE
        val usedMen = allocatedMen - freeMemory() / MEGABYTE

        runLater {
          configControl.maxMem = maxMen
          configControl.allocatedMem = allocatedMen
          configControl.usedMem = usedMen
        }
      }
    }
  }

  override fun onUndock() {
    super.onUndock()

    unsubscribe<LauncherStartEvent> {}
    unsubscribe<LauncherStopEvent> {}

    timer?.cancel()
  }
}
