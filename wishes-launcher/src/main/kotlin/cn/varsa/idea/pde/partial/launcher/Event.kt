package cn.varsa.idea.pde.partial.launcher

import tornadofx.*

object LauncherStartEvent : FXEvent()
object LauncherStopEvent : FXEvent()

object ProcessStartEvent : FXEvent()
class ProcessWillTerminateEvent(val willBeDestroyed: Boolean) : FXEvent()
class ProcessTerminatedEvent(val exitCode: Int) : FXEvent()
