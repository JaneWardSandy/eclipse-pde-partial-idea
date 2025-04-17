package cn.varsa.idea.pde.partial.plugin.run

import cn.varsa.idea.pde.partial.plugin.dom.config.*
import cn.varsa.idea.pde.partial.plugin.facet.*
import cn.varsa.idea.pde.partial.plugin.i18n.EclipsePDEPartialBundles.message
import cn.varsa.idea.pde.partial.plugin.support.*
import com.intellij.execution.ui.*
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.options.*
import com.intellij.openapi.ui.*
import com.intellij.ui.*
import com.intellij.ui.components.*
import com.intellij.util.execution.*
import com.intellij.util.ui.*
import java.awt.*
import javax.swing.*

import com.intellij.ui.dsl.builder.panel

class PDETargetRunConfigurationEditor(configuration: PDETargetRunConfiguration) :
  SettingsEditor<PDETargetRunConfiguration>(), PanelWithAnchor {
  private var myAnchor: JComponent? = null

  private val panel = JPanel(VerticalFlowLayout(VerticalFlowLayout.TOP, 0, 5, true, false))

  private val productField = ComboBox<String>().apply {
    renderer = ColoredListCellRendererWithSpeedSearch.stringRender()
    ComboboxSpeedSearch.installOn(this).setClearSearchOnNavigateNoMatch(true)
  }
  private val applicationField = ComboBox<String>().apply {
    renderer = ColoredListCellRendererWithSpeedSearch.stringRender()
    ComboboxSpeedSearch.installOn(this).setClearSearchOnNavigateNoMatch(true)
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

  private val jrePath = JrePathEditor(DefaultJreSelector.projectSdk(configuration.project))
  private val javaParameters = CommonJavaParametersPanel().apply { preferredSize = null }

  private val additionalClasspath =
    RawCommandLineEditor(ParametersListUtil.COLON_LINE_PARSER, ParametersListUtil.COLON_LINE_JOINER)
  private val additionalClasspathComponent = LabeledComponent.create(
    additionalClasspath, message("run.local.config.tab.configuration.classpath"), BorderLayout.WEST
  )

  private val moduleList = CheckBoxList<String>()

  private val cleanRuntimeDir = JBCheckBox(message("run.remote.config.tab.wishes.cleanRuntimeDir"))

  init {
    panel.add(productComponent)
    panel.add(applicationComponent)
    panel.add(dataDirectoryComponent)
    panel.add(JSeparator())
    panel.add(jrePath)
    panel.add(javaParameters)
    panel.add(JSeparator())
    panel.add(additionalClasspathComponent)

    panel.add(targetModulePanel())
    panel.add(cleanRuntimeDir)

    additionalClasspath.attachLabel(additionalClasspathComponent.label)
    CommonJavaParametersPanel.addMacroSupport(additionalClasspath.editorField)

    val pdeModules = configuration.project.allPDEModules().filter { PDEFacet.getInstance(it) != null }
    moduleList.selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
    moduleList.setItems(pdeModules.map { module -> module.name }, null)
    if (moduleList.itemsCount > 0)
      moduleList.allItems.forEach{ moduleList.setItemSelected(it, configuration.targetModules == null || it in configuration.targetModules!!) }

    panel.updateUI()

    myAnchor = UIUtil.mergeComponentsWithAnchor(
      productComponent,
      applicationComponent,
      dataDirectoryComponent,
      jrePath,
      javaParameters,
      additionalClasspathComponent
    )
  }

  private fun targetModulePanel(): DialogPanel {
    val selectAllAction = object : AnAction(
      message("run.local.config.tab.configuration.targetModules.selectAll"),
      message("run.local.config.tab.configuration.targetModules.selectAllModules"), AllIcons.Actions.Selectall // Use standard icon
    ) {
      override fun actionPerformed(e: AnActionEvent) {
        if (moduleList.itemsCount > 0) moduleList.allItems.forEach{ moduleList.setItemSelected(it, true) }
      }

      override fun update(e: AnActionEvent) {
        // Enable only if there are items and not all are already selected
        e.presentation.isEnabled = moduleList.allItems.any { !moduleList.isItemSelected(it) }
      }

      override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
      }
    }

    val unselectAllAction = object : AnAction(
      message("run.local.config.tab.configuration.targetModules.unselectAll"),
      message("run.local.config.tab.configuration.targetModules.unselectAllModules"), AllIcons.Actions.Unselectall // Use standard icon
    ) {
      override fun actionPerformed(e: AnActionEvent) {
        moduleList.allItems.forEach{ moduleList.setItemSelected(it, false) }
      }

      override fun update(e: AnActionEvent) {
        // Enable only if there is at least one item selected
        e.presentation.isEnabled = moduleList.allItems.any { moduleList.isItemSelected(it) }
      }

      override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
      }
    }

    val actionGroup = DefaultActionGroup().apply {
      add(selectAllAction)
      add(unselectAllAction)
    }
    val actionToolbar = ActionManager.getInstance().createActionToolbar(
      ActionPlaces.TOOLBAR,
      actionGroup,
      true // true for horizontal toolbar
    )
    actionToolbar.targetComponent = moduleList
    return panel {
      collapsibleGroup(message("run.local.config.tab.configuration.targetModules")) {
        row {
          cell(actionToolbar.component)
        }

        row {
          resizableRow().scrollCell(moduleList).align(com.intellij.ui.dsl.builder.Align.FILL)
        }
      }
    }
  }

  override fun resetEditorFrom(configuration: PDETargetRunConfiguration) {
    javaParameters.reset(configuration)
    jrePath.setPathOrName(configuration.alternativeJrePath, configuration.isAlternativeJrePathEnabled)

    val managementService = ExtensionPointManagementService.getInstance(configuration.project)
    productField.apply {
      removeAllItems()
      addItem("")
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
    additionalClasspath.text = configuration.additionalClasspath

    val pdeModules = configuration.project.allPDEModules().filter { PDEFacet.getInstance(it) != null }
    moduleList.clear()
    moduleList.setItems(pdeModules.map { module -> module.name }, null)
    if (moduleList.itemsCount > 0)
      moduleList.allItems.forEach{ moduleList.setItemSelected(it, configuration.targetModules == null || it in configuration.targetModules!!) }
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
    configuration.additionalClasspath = additionalClasspath.text

    configuration.targetModules =  moduleList.allItems.filter {
      moduleList.isItemSelected(it)
    }.toSet()
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
    additionalClasspathComponent.anchor = anchor
  }
}
