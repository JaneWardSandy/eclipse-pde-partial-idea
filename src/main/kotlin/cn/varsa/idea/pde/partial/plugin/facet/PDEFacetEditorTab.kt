package cn.varsa.idea.pde.partial.plugin.facet

import cn.varsa.idea.pde.partial.common.support.*
import cn.varsa.idea.pde.partial.plugin.i18n.EclipsePDEPartialBundles.message
import cn.varsa.idea.pde.partial.plugin.listener.*
import cn.varsa.idea.pde.partial.plugin.support.*
import com.intellij.facet.ui.*
import com.intellij.openapi.ui.*
import com.intellij.ui.*
import com.intellij.ui.components.*
import com.intellij.ui.components.panels.*
import com.intellij.util.ui.*
import com.intellij.util.ui.components.*
import java.awt.*
import javax.swing.*
import javax.swing.event.*

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

  private val updateArtifactsCheckbox = JBCheckBox()
  private val updateArtifactsComponent =
    LabeledComponent.create(updateArtifactsCheckbox, message("facet.tab.updateArtifacts"), BorderLayout.WEST)

  private val updateCompilerOutputCheckbox = JBCheckBox()
  private val updateCompilerOutputComponent =
    LabeledComponent.create(updateCompilerOutputCheckbox, message("facet.tab.updateCompilerOutput"), BorderLayout.WEST)

  private val manifestRelativePathTextField = JBTextField()
  private val manifestRelativePathComponent = LabeledComponent.create(
    manifestRelativePathTextField, message("facet.tab.manifestFileRelativePath"), BorderLayout.WEST
  )

  private val compilerClassesTextField = JBTextField()
  private val compilerClassesComponent = LabeledComponent.create(
    compilerClassesTextField, message("facet.tab.compilerClassesOutputDirectory"), BorderLayout.WEST
  )

  private val compilerTestClassesTextField = JBTextField()
  private val compilerTestClassesComponent = LabeledComponent.create(
    compilerTestClassesTextField, message("facet.tab.compilerTestClassesOutputDirectory"), BorderLayout.WEST
  )

  private val binaryOutputList = CheckBoxList<String>().apply {
    border =
      IdeBorderFactory.createTitledBorder(message("facet.tab.binaryList"), false, JBUI.insetsTop(8)).setShowLine(true)
  }

  init {
    panel.addToTop(VerticalBox().apply {
      border = IdeBorderFactory.createTitledBorder(message("facet.tab.compilerOutput"), false, JBUI.insetsTop(8))
        .setShowLine(true)

      add(updateArtifactsComponent)
      add(updateCompilerOutputComponent)
      add(manifestRelativePathComponent)
      add(compilerClassesComponent)
      add(compilerTestClassesComponent)
    })
    panel.addToCenter(binaryOutputList)

    myAnchor = UIUtil.mergeComponentsWithAnchor(
      updateArtifactsComponent,
      updateCompilerOutputComponent,
      manifestRelativePathComponent,
      compilerClassesComponent,
      compilerTestClassesComponent,
    )
  }

  override fun apply() {
    initializeIfNeeded()
    validateOnce {
      if (configuration.updateArtifacts != updateArtifactsCheckbox.isSelected) {
        configuration.updateArtifacts = updateArtifactsCheckbox.isSelected
      }
      if (configuration.updateCompilerOutput != updateCompilerOutputCheckbox.isSelected) {
        configuration.updateCompilerOutput = updateCompilerOutputCheckbox.isSelected
      }

      if (configuration.manifestRelativePath != manifestRelativePathTextField.text) {
        configuration.manifestRelativePath = manifestRelativePathTextField.text
      }

      if (configuration.compilerClassesOutput != compilerClassesTextField.text) {
        FacetChangeListener.notifyCompileOutputPathChanged(
          context.module, configuration.compilerClassesOutput, compilerClassesTextField.text
        )

        configuration.compilerClassesOutput = compilerClassesTextField.text
      }
      if (configuration.compilerTestClassesOutput != compilerTestClassesTextField.text) {
        FacetChangeListener.notifyCompileTestOutputPathChanged(
          context.module, configuration.compilerTestClassesOutput, compilerTestClassesTextField.text
        )

        configuration.compilerTestClassesOutput = compilerTestClassesTextField.text
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
      updateArtifactsCheckbox.isSelected = configuration.updateArtifacts
      updateCompilerOutputCheckbox.isSelected = configuration.updateCompilerOutput
      manifestRelativePathTextField.text = configuration.manifestRelativePath

      compilerClassesTextField.text = configuration.compilerClassesOutput
      compilerTestClassesTextField.text = configuration.compilerTestClassesOutput

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
    isInitialized && (configuration.updateArtifacts != updateArtifactsCheckbox.isSelected || configuration.updateCompilerOutput != updateCompilerOutputCheckbox.isSelected || configuration.manifestRelativePath != manifestRelativePathTextField.text || configuration.compilerClassesOutput != compilerClassesTextField.text || configuration.compilerTestClassesOutput != compilerTestClassesTextField.text || !binaryOutputList.checkedItems()
      .let { it.containsAll(oldCheckList) && oldCheckList.containsAll(it) })

  override fun getAnchor(): JComponent? = myAnchor
  override fun setAnchor(anchor: JComponent?) {
    this.myAnchor = anchor

    updateArtifactsComponent.anchor = anchor
    updateCompilerOutputComponent.anchor = anchor
    manifestRelativePathComponent.anchor = anchor

    compilerClassesComponent.anchor = anchor
    compilerTestClassesComponent.anchor = anchor
  }

  override fun onTabEntering() {
    initializeIfNeeded()
  }

  private fun reloadCheckList() {
    binaryOutputList.apply {
      clear()
      context.module.getModuleDir()?.toFile()?.listFiles()?.map { it.name }?.also { setItems(it, null) }
    }
  }

  private fun initializeIfNeeded() {
    if (isInitialized) return

    validatorsManager.registerValidator(ArgumentConsistencyValidator(compilerClassesTextField))
    validatorsManager.registerValidator(ArgumentConsistencyValidator(compilerTestClassesTextField))

    compilerClassesTextField.validateOnChange()
    compilerTestClassesTextField.validateOnChange()

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
    document.addDocumentListener(object : DocumentAdapter() {
      override fun textChanged(e: DocumentEvent) = doValidate()
    })
  }

  private fun CheckBoxList<String>.checkedItems() =
    (0 until itemsCount).mapNotNull(this::getItemAt).filter(this::isItemSelected).distinct().toSet()

  inner class ArgumentConsistencyValidator(private val textField: JBTextField) : FacetEditorValidator() {
    override fun check(): ValidationResult = with(textField.text) {
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
