package cn.varsa.idea.pde.partial.launcher.view

import cn.varsa.idea.pde.partial.common.*
import cn.varsa.idea.pde.partial.launcher.control.*
import cn.varsa.idea.pde.partial.launcher.support.*
import javafx.geometry.*
import javafx.scene.*
import javafx.scene.control.*
import tornadofx.*
import java.io.*

class LibraryView : View("Libraries") {
    private val configControl: ConfigControl by inject()

    private val launcherProperty = mutableListOf<String>().asObservable()
    private val launcherJarProperty = mutableListOf<String>().asObservable()

    override val root: Parent = borderpane {
        val listview = listview(configControl.librariesProperty) {
            borderpaneConstraints {
                margin = Insets(5.0)
                prefHeight = 250.0
                prefWidth = 400.0
            }
            selectionModel.selectionMode = SelectionMode.SINGLE
            onDoubleClick { editLocation(selectedItem) }
            fileDND(File::isDirectory) { addLocation(it) }
        }
        center = listview
        right = vbox(spacing = 5, alignment = Pos.TOP_CENTER) {
            borderpaneConstraints { margin = Insets(5.0) }
            button("+").action { editLocation() }
            button("-") {
                enableWhen { listview.selectionModel.selectedItemProperty().isNotNull }
                action { listview.selectedItem?.also { configControl.librariesProperty.remove(it) } }
            }
        }
        bottom = form {
            fieldset("Launcher") {
                field("Launcher") { combobox(configControl.launcherProperty, launcherProperty) }
                field("Launcher Jar") { combobox(configControl.launcherJarProperty, launcherJarProperty) }
            }
        }

        completeWhen {
            configControl.run {
                librariesProperty.sizeProperty.greaterThan(0) and launcherProperty.isNotEmpty and launcherJarProperty.isNotEmpty
            }
        }
    }

    override fun onDock() {
        super.onDock()
        configControl.librariesProperty.clear()
        configControl.librariesProperty += config.string("libraries", "").split(",").filterNot { it.isBlank() }

        launcherProperty.clear()
        launcherJarProperty.clear()
        configControl.librariesProperty.map(::File).filter(File::exists).filter(File::isDirectory)
            .map { File(it, Plugins).takeIf(File::exists) ?: it }.run {
                launcherProperty += map(File::getParentFile).distinct().flatMap {
                    listOf(
                        File(it, "Teamcenter.exe"), File(it, "eclipse.exe"), File(it.parentFile, "MacOS/eclipse")
                    )
                }.filter(File::exists).map(File::getCanonicalPath).distinct()

                launcherJarProperty += mapNotNull(File::listFiles).flatMap { it.toList() }
                    .filter { it.extension.lowercase() == "jar" && it.name.startsWith("org.eclipse.equinox.launcher_") }
                    .map(File::getCanonicalPath).distinct()
            }

        configControl.launcher = config.string("launcher", "")
        configControl.launcherJar = config.string("launcherJar", "")
    }

    override fun onSave() {
        super.onSave()
        config["libraries"] = configControl.librariesProperty.joinToString(",")
        config["launcher"] = configControl.launcher
        config["launcherJar"] = configControl.launcherJar
        config.save()
    }

    private fun editLocation(path: String? = null) {
        chooseDirectory("Select directory", path?.let(::File), currentWindow)?.also { file ->
            path?.also { configControl.librariesProperty.remove(it) }
            addLocation(file)
        }
    }

    private fun addLocation(file: File) {
        configControl.librariesProperty += file.canonicalPath

        val directory = File(file, Plugins).takeIf(File::exists) ?: file
        launcherProperty += directory.parentFile.let { listOf(File(it, "Teamcenter.exe"), File(it, "eclipse.exe")) }
            .filter(File::exists).map(File::getCanonicalPath).distinct().filterNot(launcherProperty::contains)
        directory.listFiles()
            ?.filter { it.extension.lowercase() == "jar" && it.name.startsWith("org.eclipse.equinox.launcher_") }
            ?.map(File::getCanonicalPath)?.distinct()?.filterNot(launcherJarProperty::contains)
            ?.also { launcherJarProperty += it }
    }
}
