package cn.varsa.idea.pde.partial.launcher.view

import cn.varsa.idea.pde.partial.launcher.control.*
import cn.varsa.idea.pde.partial.launcher.support.*
import javafx.scene.*
import javafx.stage.*
import tornadofx.*
import java.io.*
import java.nio.charset.*

class RunConfigView : View("Basic Configuration") {
    private val configControl: ConfigControl by inject()
    private val validationContext = ValidationContext()

    override val root: Parent = form {
        fieldset("Run Configuration") {
            fileField(
                "Java Executor",
                configControl.javaExeProperty,
                arrayOf(FileChooser.ExtensionFilter("Java.exe", "java.exe", "Java.exe")),
                validationContext
            )
            directoryField("Runtime directory", configControl.runtimeDirectoryProperty, validationContext)
            directoryField("Project root", configControl.projectRootProperty, validationContext) {
                when {
                    File(it, ".idea").exists().not() -> error("Only support IntelliJ IDEA project")
                    else -> null
                }
            }
            field("IDEA Charset") {
                combobox(configControl.ideaCharsetProperty, Charset.availableCharsets().values.toList()) {
                    cellFormat { text = it.name() }
                }
            }
        }

        completeWhen(validationContext::valid)
    }

    override fun onDock() {
        super.onDock()
        configControl.javaExe = config.string("javaExe", "")
        configControl.runtimeDirectory = config.string("runtimeDirectory", "")
        configControl.projectRoot = config.string("projectRoot", "")

        configControl.ideaCharset = config.string("charset")?.let {
            try {
                Charset.forName(it)
            } catch (e: UnsupportedCharsetException) {
                null
            }
        } ?: Charset.defaultCharset()
    }

    override fun onSave() {
        super.onSave()
        config["javaExe"] = configControl.javaExe
        config["runtimeDirectory"] = configControl.runtimeDirectory
        config["projectRoot"] = configControl.projectRoot
        config["charset"] = configControl.ideaCharset.name()
        config.save()
    }
}
