package cn.varsa.idea.pde.partial.launcher.support

import javafx.beans.value.*
import javafx.event.*
import javafx.scene.*
import javafx.scene.input.*
import javafx.stage.*
import tornadofx.*
import java.io.*

fun Node.fileDND(predicate: (File) -> Boolean, onDropped: (File) -> Unit) {
    onDragOver = EventHandler {
        if (it.gestureSource !== this && it.dragboard.hasFiles() && it.dragboard.files.filter(predicate)
                .any(File::exists)
        ) {
            it.acceptTransferModes(*TransferMode.COPY_OR_MOVE)
        }
        it.consume()
    }
    onDragDropped = EventHandler {
        if (it.dragboard.hasFiles()) {
            it.dragboard.files.filter(predicate).firstOrNull(File::exists)?.apply {
                onDropped(this)
                it.isDropCompleted = true
            }
        }
        it.consume()
    }
}

fun EventTarget.directoryField(
    label: String? = null,
    property: ObservableValue<String>,
    validationContext: ValidationContext? = null,
    op: ValidationContext.(String) -> ValidationMessage? = { null },
) = field(label) {
    val validator: ValidationContext.(String?) -> ValidationMessage? = {
        when {
            it.isNullOrBlank() -> error("Path is required")
            else -> {
                val file = File(it)
                when {
                    !file.isDirectory -> error("Path must be directory")
                    !file.exists() -> error("Directory must existed")
                    else -> op(this, it)
                }
            }
        }
    }

    val textField = textfield(property) {
        validationContext?.addValidator(node = this, property = property, validator = validator)?.validate(false)
        fileDND(File::isDirectory) { text = it.canonicalPath }
    }
    button("Browse").action {
        textField.text = chooseDirectory("Choose directory")?.absolutePath
    }
}

fun EventTarget.fileField(
    label: String? = null,
    property: ObservableValue<String>,
    extensionFilter: Array<FileChooser.ExtensionFilter> = emptyArray(),
    validationContext: ValidationContext? = null,
    op: ValidationContext.(String) -> ValidationMessage? = { null },
) = field(label) {
    val validator: ValidationContext.(String?) -> ValidationMessage? = {
        when {
            it.isNullOrBlank() -> error("Path is required")
            else -> {
                val file = File(it)
                when {
                    !file.isFile -> error("Path must be file")
                    !file.exists() -> error("File must existed")
                    file.name.toLowerCase().let { name ->
                        extensionFilter.flatMap { e -> e.extensions }.map { e -> e.toLowerCase() }
                            .none { e -> name.endsWith(e) }
                    } -> error("File must be ends with ${extensionFilter.flatMap { e -> e.extensions }}")
                    else -> op(this, it)
                }
            }
        }
    }

    val textField = textfield(property) {
        validationContext?.addValidator(node = this, property = property, validator = validator)?.validate(false)
        fileDND(File::isFile) { text = it.canonicalPath }
    }
    button("Browse").action {
        textField.text = chooseFile("Choose file", extensionFilter).firstOrNull()?.absolutePath
    }
}

fun EventTarget.stringField(
    label: String? = null,
    property: ObservableValue<String>,
    validationContext: ValidationContext? = null,
    op: ValidationContext.(String) -> ValidationMessage? = { null },
) = field(label) {
    val validator: ValidationContext.(String?) -> ValidationMessage? = {
        when {
            it.isNullOrBlank() -> error("String is required")
            else -> op(this, it)
        }
    }

    val textField = textfield(property) {
        validationContext?.addValidator(node = this, property = property, validator = validator)?.validate(false)
    }

    onDragOver = EventHandler {
        if (it.gestureSource !== this && it.dragboard.hasString() && it.dragboard.string.isNotBlank()) {
            it.acceptTransferModes(*TransferMode.COPY_OR_MOVE)
        }
        it.consume()
    }
    onDragDropped = EventHandler {
        if (it.dragboard.hasString() && it.dragboard.string.isNotBlank()) {
            textField.text = it.dragboard.string
            it.isDropCompleted = true
        }
        it.consume()
    }
}


fun EventTarget.intField(
    label: String? = null,
    property: ObservableValue<Number>,
    validationContext: ValidationContext? = null,
    op: ValidationContext.(Number) -> ValidationMessage? = { null },
) = field(label) {
    val validator: ValidationContext.(Number?) -> ValidationMessage? = {
        when {
            it == null -> error("Integer is required")
            it.toInt() <= 0 -> error("Integer must great than 0")
            else -> op(this, it)
        }
    }

    textfield(property) {
        validationContext?.addValidator(node = this, property = property, validator = validator)?.validate(false)
    }
}
