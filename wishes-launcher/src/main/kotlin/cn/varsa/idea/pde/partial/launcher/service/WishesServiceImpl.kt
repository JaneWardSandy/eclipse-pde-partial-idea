package cn.varsa.idea.pde.partial.launcher.service

import cn.varsa.idea.pde.partial.common.domain.*
import cn.varsa.idea.pde.partial.common.service.*
import cn.varsa.idea.pde.partial.launcher.control.*
import cn.varsa.idea.pde.partial.launcher.support.*
import java.rmi.*
import java.rmi.server.*

class WishesServiceImpl(private val wishesControl: WishesControl, private val configControl: ConfigControl) :
  UnicastRemoteObject(), WishesService {
  private val logger = thisLogger()

  override fun generateData(parameters: JavaCommandParameters) {
    try {
      logger.info("Generating Data...")
      wishesControl.generateData(parameters)
    } catch (e: Exception) {
      logger.error(e.message, e)
      throw RemoteException(e.message, e)
    }
  }

  override fun clean() {
    try {
      logger.info("Cleaning Data...")
      wishesControl.clean()
    } catch (e: Exception) {
      logger.error(e.message, e)
      throw RemoteException(e.message, e)
    }
  }

  override fun launch() {
    try {
      logger.info("Launching...")
      wishesControl.launch()
    } catch (e: Exception) {
      logger.error(e.message, e)
      throw RemoteException(e.message, e)
    }
  }

  override fun destroy() {
    try {
      logger.info("Destroying...")
      wishesControl.destroy()
    } catch (e: Exception) {
      logger.error(e.message, e)
      throw RemoteException(e.message, e)
    }
  }

  override fun isPortalLaunched(): Boolean = configControl.portalRunning
  override fun isJDWPRunning(): Boolean = configControl.jdwpRunning

  override fun setupStartupLevels(startupLevels: Map<String, Int>) {
    configControl.startupLevels.clear()
    configControl.startupLevels += startupLevels
  }

  override fun setupTargetProgram(product: String, application: String) {
    configControl.product = product
    configControl.application = application
  }

  override fun setupModules(modules: List<DevModule>) {
    configControl.devModules.clear()
    configControl.devModules += modules
  }
}
