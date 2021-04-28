package cn.varsa.idea.pde.partial.plugin.run

import cn.varsa.idea.pde.partial.plugin.i18n.EclipsePDEPartialBundles.message
import com.intellij.execution.ui.*
import com.intellij.openapi.options.*
import com.intellij.openapi.project.*
import com.intellij.openapi.ui.*
import com.intellij.ui.*
import com.intellij.ui.components.*
import com.intellij.ui.components.panels.*
import com.intellij.util.ui.*
import java.awt.*
import javax.swing.*

class PDETargetRunConfigurationEditor(project: Project) : SettingsEditor<PDETargetRunConfiguration>(), PanelWithAnchor {
    private var myAnchor: JComponent? = null

    private val panel = VerticalBox()

    // TODO: 2021/3/14 Combo select existed product & application
    private val productField = JBTextField()
    private val applicationField = JBTextField()

    private val productComponent =
        LabeledComponent.create(productField, message("run.local.config.tab.configuration.product"), BorderLayout.WEST)
    private val applicationComponent = LabeledComponent.create(
        applicationField, message("run.local.config.tab.configuration.application"), BorderLayout.WEST
    )

    private val jrePath = JrePathEditor(DefaultJreSelector.projectSdk(project))
    private val javaParameters = CommonJavaParametersPanel()

    init {
        panel.add(productComponent)
        panel.add(applicationComponent)
        panel.add(JSeparator())
        panel.add(jrePath)
        panel.add(javaParameters)

        myAnchor = UIUtil.mergeComponentsWithAnchor(productComponent, applicationComponent, jrePath, javaParameters)
    }

    override fun resetEditorFrom(configuration: PDETargetRunConfiguration) {
        javaParameters.reset(configuration)
        jrePath.setPathOrName(configuration.alternativeJrePath, configuration.isAlternativeJrePathEnabled)

        productField.text = configuration.product
        applicationField.text = configuration.application

        configuration.mainClassName = "org.eclipse.equinox.launcher.Main"
    }

    override fun applyEditorTo(configuration: PDETargetRunConfiguration) {
        javaParameters.applyTo(configuration)
        configuration.alternativeJrePath = jrePath.jrePathOrName
        configuration.isAlternativeJrePathEnabled = jrePath.isAlternativeJreSelected

        configuration.product = productField.text
        configuration.application = applicationField.text

        configuration.mainClassName = "org.eclipse.equinox.launcher.Main"
    }

    override fun createEditor(): JComponent = panel
    override fun getAnchor(): JComponent? = myAnchor
    override fun setAnchor(anchor: JComponent?) {
        myAnchor = anchor

        productComponent.anchor = anchor
        applicationComponent.anchor = anchor
        jrePath.anchor = anchor
        javaParameters.anchor = anchor
    }
}
