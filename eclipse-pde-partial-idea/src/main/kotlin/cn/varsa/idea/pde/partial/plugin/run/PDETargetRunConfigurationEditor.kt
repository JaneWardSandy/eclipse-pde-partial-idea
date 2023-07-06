package cn.varsa.idea.pde.partial.plugin.run

import cn.varsa.idea.pde.partial.plugin.dom.config.*
import cn.varsa.idea.pde.partial.plugin.i18n.EclipsePDEPartialBundles.message
import cn.varsa.idea.pde.partial.plugin.support.*
import com.intellij.execution.ui.*
import com.intellij.openapi.options.*
import com.intellij.openapi.project.*
import com.intellij.openapi.ui.*
import com.intellij.ui.*
import com.intellij.ui.components.*
import com.intellij.util.ui.*
import java.awt.*
import javax.swing.*

class PDETargetRunConfigurationEditor(project: Project) : SettingsEditor<PDETargetRunConfiguration>(), PanelWithAnchor {
  private var myAnchor: JComponent? = null

  private val panel = JPanel(VerticalFlowLayout(VerticalFlowLayout.TOP, 0, 5, true, false))

  private val productField = ComboBox<String>().apply {
    renderer = ColoredListCellRendererWithSpeedSearch.stringRender()
    ComboboxSpeedSearch(this).setClearSearchOnNavigateNoMatch(true)
  }
  private val applicationField = ComboBox<String>().apply {
    renderer = ColoredListCellRendererWithSpeedSearch.stringRender()
    ComboboxSpeedSearch(this).setClearSearchOnNavigateNoMatch(true)
  }
  private val dataDirectoryField = JBTextField()

  private val productComponent =
    LabeledComponent.create(productField, message("run.local.config.tab.configuration.product"), BorderLayout.WEST)
  private val applicationComponent = LabeledComponent.create(
    applicationField, message("run.local.config.tab.configuration.application"), BorderLayout.WEST
  )
  private val dataDirectoryComponent = LabeledComponent.create(
    dataDirectoryField, message("run.local.config.tab.configuration.dataDirectory"), BorderLayout.WEST
  )

  private val jrePath = JrePathEditor(DefaultJreSelector.projectSdk(project))
  private val javaParameters = CommonJavaParametersPanel().apply { preferredSize = null }

  private val cleanRuntimeDir = JBCheckBox(message("run.remote.config.tab.wishes.cleanRuntimeDir"))

  init {
    panel.add(productComponent)
    panel.add(applicationComponent)
    panel.add(dataDirectoryComponent)
    panel.add(JSeparator())
    panel.add(jrePath)
    panel.add(javaParameters)
    panel.add(JSeparator())
    panel.add(cleanRuntimeDir)

    panel.updateUI()

    myAnchor = UIUtil.mergeComponentsWithAnchor(productComponent, applicationComponent, dataDirectoryComponent, jrePath, javaParameters)
  }

  override fun resetEditorFrom(configuration: PDETargetRunConfiguration) {
    javaParameters.reset(configuration)
    jrePath.setPathOrName(configuration.alternativeJrePath, configuration.isAlternativeJrePathEnabled)

    val managementService = ExtensionPointManagementService.getInstance(configuration.project)
    productField.apply {
      removeAllItems()
      managementService.getProducts().sorted().forEach(this::addItem)
      item = configuration.product
    }
    applicationField.apply {
      removeAllItems()
      managementService.getApplications().sorted().forEach(this::addItem)
      item = configuration.application
    }
    dataDirectoryField.text = configuration.dataDirectory

    configuration.mainClassName = "org.eclipse.equinox.launcher.Main"
    cleanRuntimeDir.isSelected = configuration.cleanRuntimeDir
  }

  override fun applyEditorTo(configuration: PDETargetRunConfiguration) {
    javaParameters.applyTo(configuration)
    configuration.alternativeJrePath = jrePath.jrePathOrName
    configuration.isAlternativeJrePathEnabled = jrePath.isAlternativeJreSelected

    configuration.product = productField.item
    configuration.application = applicationField.item
    configuration.dataDirectory = dataDirectoryField.text

    configuration.mainClassName = "org.eclipse.equinox.launcher.Main"
    configuration.cleanRuntimeDir = cleanRuntimeDir.isSelected
  }

  override fun createEditor(): JComponent = panel
  override fun getAnchor(): JComponent? = myAnchor
  override fun setAnchor(anchor: JComponent?) {
    myAnchor = anchor

    productComponent.anchor = anchor
    applicationComponent.anchor = anchor
    dataDirectoryComponent.anchor = anchor
    jrePath.anchor = anchor
    javaParameters.anchor = anchor
  }
}
