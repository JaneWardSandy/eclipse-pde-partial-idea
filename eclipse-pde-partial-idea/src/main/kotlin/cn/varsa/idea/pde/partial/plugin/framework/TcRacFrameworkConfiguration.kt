package cn.varsa.idea.pde.partial.plugin.framework

import cn.varsa.idea.pde.partial.plugin.i18n.EclipsePDEPartialBundles.message
import com.intellij.ide.util.frameworkSupport.*
import com.intellij.openapi.module.*
import com.intellij.openapi.roots.*
import com.intellij.openapi.roots.libraries.*
import com.intellij.ui.*
import com.intellij.ui.components.*
import com.intellij.ui.components.panels.*
import com.intellij.util.ui.*
import com.intellij.util.ui.components.*
import javax.swing.*

class TcRacFrameworkConfiguration(private val provider: TcRacFrameworkSupportProvider) :
  FrameworkSupportConfigurable() {
  private val panel = BorderLayoutPanel()

  private val updateArtifactsCheckbox = JBCheckBox(message("facet.tab.updateArtifacts"), true)
  private val updateCompilerOutputCheckbox = JBCheckBox(message("facet.tab.updateCompilerOutput"), true)

  init {
    panel.addToTop(VerticalBox().apply {
      border = IdeBorderFactory.createTitledBorder(message("facet.tab.compilerOutput"), false, JBUI.insetsTop(8))
        .setShowLine(true)

      add(updateArtifactsCheckbox)
      add(updateCompilerOutputCheckbox)
    })
  }

  override fun getComponent(): JComponent = panel
  override fun addSupport(module: Module, model: ModifiableRootModel, library: Library?) {
    provider.addSupport(
      module,
      updateArtifactsCheckbox.isSelected,
      updateCompilerOutputCheckbox.isSelected,
    )
  }
}