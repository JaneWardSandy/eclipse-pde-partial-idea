package cn.varsa.idea.pde.partial.launcher.view

import cn.varsa.idea.pde.partial.launcher.control.*
import cn.varsa.idea.pde.partial.launcher.support.*
import javafx.scene.*
import tornadofx.*

class NamingView : View("RMI Naming") {
    private val configControl: ConfigControl by inject()
    private val validationContext = ValidationContext()

    override val root: Parent = form {
        fieldset("Naming binding") {
            intField("RMI Port", configControl.rmiPortProperty, validationContext)
            stringField("RMI Name", configControl.rmiNameProperty, validationContext)
        }
        fieldset("Preview") {
            field("Name") { label(configControl.rmiUrlProperty) }
        }

        completeWhen(validationContext::valid)
    }

    override fun onDock() {
        super.onDock()
        configControl.rmiPort = config.int("rmiPort", 7995)
        configControl.rmiName = config.string("rmiName", "WishesService")
    }

    override fun onSave() {
        super.onSave()
        config["rmiPort"] = configControl.rmiPort.toString()
        config["rmiName"] = configControl.rmiName
        config.save()
    }
}
