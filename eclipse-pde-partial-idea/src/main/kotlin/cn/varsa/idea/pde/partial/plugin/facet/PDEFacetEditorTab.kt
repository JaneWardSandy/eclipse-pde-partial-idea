package cn.varsa.idea.pde.partial.plugin.facet

import cn.varsa.idea.pde.partial.common.support.*
import cn.varsa.idea.pde.partial.plugin.i18n.EclipsePDEPartialBundles.message
import cn.varsa.idea.pde.partial.plugin.listener.*
import com.intellij.facet.ui.*
import com.intellij.openapi.ui.*
import com.intellij.ui.*
import com.intellij.ui.components.*
import com.intellij.util.ui.*
import com.intellij.util.ui.components.*
import org.jetbrains.kotlin.idea.core.util.*
import cn.varsa.idea.pde.partial.plugin.support.*
import java.awt.*
import javax.swing.*

class PDEFacetEditorTab(
    private val configuration: PDEFacetConfiguration,
    private val context: FacetEditorContext,
    private val validatorsManager: FacetValidatorsManager
) : FacetEditorTab(), PanelWithAnchor {
    private val oldCheckList = hashSetOf<String>()
    private var myAnchor: JComponent? = null
    private var isInitialized = false
    private var enableValidation = false

    private val panel = BorderLayoutPanel()

    private val compileDirectoryTextField = JBTextField()
    private val compileDirectoryComponent =
        LabeledComponent.create(compileDirectoryTextField, message("facet.tab.compileDirectory"), BorderLayout.WEST)

    private val binaryOutputList = CheckBoxList<String>().apply {
        border = IdeBorderFactory.createTitledBorder(
            message("facet.tab.binaryList"), false, JBUI.insetsTop(8)
        ).setShowLine(true)
    }

    init {
        panel.addToTop(compileDirectoryComponent)
        panel.addToCenter(binaryOutputList)

        myAnchor = UIUtil.mergeComponentsWithAnchor(compileDirectoryComponent)
    }

    override fun apply() {
        initializeIfNeeded()
        validateOnce {
            if (configuration.compilerOutputDirectory != compileDirectoryTextField.text) {
                configuration.compilerOutputDirectory = compileDirectoryTextField.text

                FacetChangeListener.notifyCompileOutputPathChanged(
                    context.module, configuration.compilerOutputDirectory, compileDirectoryTextField.text
                )
            }

            val checkedItems = binaryOutputList.checkedItems()
            if (!checkedItems.let { it.containsAll(oldCheckList) && oldCheckList.containsAll(it) }) {
                configuration.binaryOutput.clear()
                configuration.binaryOutput += checkedItems

                FacetChangeListener.notifyBinaryOutputChanged(context.module, oldCheckList, checkedItems)

                oldCheckList.clear()
                oldCheckList += configuration.binaryOutput
            }
        }
    }

    override fun reset() {
        initializeIfNeeded()
        validateOnce {
            compileDirectoryTextField.text = configuration.compilerOutputDirectory
            binaryOutputList.apply {
                reloadCheckList()
                configuration.binaryOutput.forEach { setItemSelected(it, true) }
            }

            oldCheckList.clear()
            oldCheckList += configuration.binaryOutput
        }
    }

    override fun getDisplayName(): String = message("facet.tab.displayName")
    override fun createComponent(): JComponent = panel

    override fun isModified(): Boolean =
        isInitialized && (configuration.compilerOutputDirectory != compileDirectoryTextField.text || !binaryOutputList.checkedItems()
            .let { it.containsAll(oldCheckList) && oldCheckList.containsAll(it) })

    override fun getAnchor(): JComponent? = myAnchor
    override fun setAnchor(anchor: JComponent?) {
        this.myAnchor = anchor

        compileDirectoryComponent.anchor = anchor
    }

    override fun onTabEntering() {
        initializeIfNeeded()
    }

    private fun reloadCheckList() {
        binaryOutputList.apply {
            clear()
            context.module.getModuleDir().toFile().listFiles()?.map { it.name }.also { setItems(it, null) }
        }
    }

    private fun initializeIfNeeded() {
        if (isInitialized) return

        validatorsManager.registerValidator(ArgumentConsistencyValidator())
        compileDirectoryTextField.validateOnChange()

        isInitialized = true
        reset()
    }

    private fun validateOnce(body: () -> Unit) {
        enableValidation = false
        body()
        enableValidation = true
        doValidate()
    }

    private fun doValidate() {
        if (enableValidation) {
            validatorsManager.validate()
        }
    }

    private fun JTextField.validateOnChange() {
        onTextChange { doValidate() }
    }

    private fun CheckBoxList<String>.checkedItems() =
        (0 until itemsCount).mapNotNull(this::getItemAt).filter(this::isItemSelected).distinct().toSet()

    inner class ArgumentConsistencyValidator : FacetEditorValidator() {

        override fun check(): ValidationResult = with(compileDirectoryTextField.text) {
            if (isBlank()) return ValidationResult(message("facet.tab.valid.notBlank"))
            if (startsWith('/')) return ValidationResult(message("facet.tab.valid.notRelative"))
            if (startsWith('.') || startsWith('~')) return ValidationResult(message("facet.tab.valid.startString"))

            val sequence = splitToSequence('\\', '/')
            if (sequence.contains(".") || sequence.contains("..") || sequence.contains("~")) return ValidationResult(
                message("facet.tab.valid.notDirectory")
            )

            return ValidationResult.OK
        }
    }
}
