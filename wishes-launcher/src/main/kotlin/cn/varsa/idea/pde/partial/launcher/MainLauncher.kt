package cn.varsa.idea.pde.partial.launcher

import javafx.application.*
import org.slf4j.bridge.*
import tornadofx.*

fun main(args: Array<String>) {
    SLF4JBridgeHandler.removeHandlersForRootLogger()
    SLF4JBridgeHandler.install()

    Application.launch(MainLauncherApp::class.java, *args)
}

class MainLauncherApp : App(WishesWizard::class) {
    init {
        setStageIcon(resources.image("/icon.png"))
    }
}
