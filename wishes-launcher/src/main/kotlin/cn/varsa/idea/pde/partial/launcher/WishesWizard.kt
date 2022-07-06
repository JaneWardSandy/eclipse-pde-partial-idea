package cn.varsa.idea.pde.partial.launcher

import cn.varsa.idea.pde.partial.launcher.control.*
import cn.varsa.idea.pde.partial.launcher.view.*
import javafx.beans.binding.*
import tornadofx.*
import kotlin.system.*

class WishesWizard : Wizard("Wishes", "Eclipse PDE Partial Remote Launcher") {
  private val configControl: ConfigControl by inject()

  override val canGoBack: BooleanExpression = hasPrevious and configControl.rmiRunningProperty.not()
  override val canGoNext: BooleanExpression = hasNext and currentPageComplete and configControl.rmiRunningProperty.not()
  override val canFinish: BooleanExpression = hasNext.not() and allPagesComplete

  init {
    graphic = resources.imageview("/icon.png")

    cancelButtonTextProperty.value = "Close"
    finishButtonTextProperty.bind(configControl.rmiRunningProperty.stringBinding { if (it == true) "Stop" else "Start" })

    Runtime.getRuntime().addShutdownHook(Thread {
      runLater { fire(LauncherStopEvent) }
    })

    add(RunConfigView::class)
    add(LibraryView::class)
    add(NamingView::class)
    add(ProgressView::class)
  }

  // on finish button press
  override fun onSave() {
    super.onSave()

    // stop closing wizard
    isComplete = false

    if (configControl.rmiRunning) fire(LauncherStopEvent)
    else fire(LauncherStartEvent)
  }

  override fun onCancel() {
    fire(LauncherStopEvent)
    super.onCancel()
    exitProcess(0)
  }
}
