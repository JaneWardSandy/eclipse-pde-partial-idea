package cn.varsa.idea.pde.partial.common.service

import cn.varsa.idea.pde.partial.common.domain.*
import java.rmi.*

interface WishesService : Remote {
  @Throws(RemoteException::class) fun generateData(parameters: JavaCommandParameters)

  @Throws(RemoteException::class) fun clean()

  @Throws(RemoteException::class) fun launch()

  @Throws(RemoteException::class) fun destroy()

  @Throws(RemoteException::class) fun isPortalLaunched(): Boolean

  @Throws(RemoteException::class) fun isJDWPRunning(): Boolean

  @Throws(RemoteException::class) fun setupStartupLevels(startupLevels: Map<String, Int>)

  @Throws(RemoteException::class) fun setupTargetProgram(product: String, application: String)

  @Throws(RemoteException::class) fun setupModules(modules: List<DevModule>)
}
