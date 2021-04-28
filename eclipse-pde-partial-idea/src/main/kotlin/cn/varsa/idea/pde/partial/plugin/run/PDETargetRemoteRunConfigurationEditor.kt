package cn.varsa.idea.pde.partial.plugin.run

import cn.varsa.idea.pde.partial.plugin.i18n.EclipsePDEPartialBundles.message
import com.intellij.execution.configuration.*
import com.intellij.execution.configurations.*
import com.intellij.openapi.options.*
import com.intellij.openapi.ui.*
import com.intellij.ui.*
import com.intellij.ui.components.*
import com.intellij.ui.components.panels.*
import com.intellij.util.ui.*
import java.awt.*
import javax.swing.*

class PDETargetRemoteRunConfigurationEditor : SettingsEditor<PDETargetRemoteRunConfiguration>(), PanelWithAnchor {
    private var myAnchor: JComponent? = null

    private val panel = VerticalBox()

    private val productTextField = JBTextField()
    private val applicationTextField = JBTextField()
    private val remoteHostTextField = JBTextField()
    private val rmiPortSpinner = JBIntSpinner(7995, 1, Int.MAX_VALUE)
    private val rmiNameTextField = JBTextField()
    private val remotePortSpinner = JBIntSpinner(5005, 1, Int.MAX_VALUE)
    private val vmParametersEditor = RawCommandLineEditor()
    private val programParametersEditor = RawCommandLineEditor()
    private val listeningTearDown = JBCheckBox(message("run.remote.config.tab.wishes.listenTearDown"))
    private val cleanRuntimeDir = JBCheckBox(message("run.remote.config.tab.wishes.cleanRuntimeDir"))


    private val productComponent =
        LabeledComponent.create(productTextField, message("run.remote.config.tab.wishes.product"), BorderLayout.WEST)
    private val applicationComponent = LabeledComponent.create(
        applicationTextField, message("run.remote.config.tab.wishes.application"), BorderLayout.WEST
    )

    private val remoteHostComponent = LabeledComponent.create(
        remoteHostTextField, message("run.remote.config.tab.wishes.remoteHost"), BorderLayout.WEST
    )
    private val remotePortComponent = LabeledComponent.create(
        remotePortSpinner, message("run.remote.config.tab.wishes.remotePort"), BorderLayout.WEST
    )
    private val rmiNameComponent =
        LabeledComponent.create(rmiNameTextField, message("run.remote.config.tab.wishes.rmiName"), BorderLayout.WEST)
    private val rmiPortComponent =
        LabeledComponent.create(rmiPortSpinner, message("run.remote.config.tab.wishes.rmiPort"), BorderLayout.WEST)
    private val jdkVersion = ComboBox(JDKVersionItem.values())

    private val vmParametersComponent = LabeledComponent.create(
        vmParametersEditor, message("run.remote.config.tab.wishes.vmOptions"), BorderLayout.WEST
    )
    private val programParametersComponent = LabeledComponent.create(
        programParametersEditor, message("run.remote.config.tab.wishes.programArguments"), BorderLayout.WEST
    )

    private val envVariablesComponent = EnvironmentVariablesComponent().apply { labelLocation = BorderLayout.WEST }

    init {
        panel.add(productComponent)
        panel.add(applicationComponent)
        panel.add(JSeparator())
        panel.add(remoteHostComponent)
        panel.add(HorizontalBox().apply {
            add(remotePortComponent)
            add(jdkVersion)
        })
        panel.add(rmiNameComponent)
        panel.add(rmiPortComponent)
        panel.add(JSeparator())
        panel.add(vmParametersComponent)
        panel.add(programParametersComponent)
        panel.add(envVariablesComponent)
        panel.add(JSeparator())
        panel.add(listeningTearDown)
        panel.add(cleanRuntimeDir)

        myAnchor = UIUtil.mergeComponentsWithAnchor(
            productComponent,
            applicationComponent,
            remoteHostComponent,
            rmiPortComponent,
            rmiNameComponent,
            remotePortComponent,
            vmParametersComponent,
            programParametersComponent,
            envVariablesComponent
        )
    }

    override fun resetEditorFrom(configuration: PDETargetRemoteRunConfiguration) {
        productTextField.text = configuration.product
        applicationTextField.text = configuration.application

        remoteHostTextField.text = configuration.remoteHost
        rmiPortSpinner.number = configuration.rmiPort
        rmiNameTextField.text = configuration.rmiName
        remotePortSpinner.number = configuration.remotePort
        jdkVersion.item = configuration.jdkVersion

        vmParametersEditor.text = configuration.vmParameters
        programParametersEditor.text = configuration.programParameters
        envVariablesComponent.envs = configuration.envVariables
        envVariablesComponent.isPassParentEnvs = configuration.passParentEnvs

        listeningTearDown.isSelected = configuration.listeningTeardown
        cleanRuntimeDir.isSelected = configuration.cleanRuntimeDir
    }

    override fun applyEditorTo(configuration: PDETargetRemoteRunConfiguration) {
        configuration.product = productTextField.text
        configuration.application = applicationTextField.text

        configuration.remoteHost = remoteHostTextField.text
        configuration.rmiPort = rmiPortSpinner.number
        configuration.rmiName = rmiNameTextField.text
        configuration.remotePort = remotePortSpinner.number
        configuration.jdkVersion = jdkVersion.item ?: JDKVersionItem.JDK5to8

        configuration.vmParameters = vmParametersEditor.text
        configuration.programParameters = programParametersEditor.text

        configuration.envVariables.clear()
        configuration.envVariables += envVariablesComponent.envs
        configuration.passParentEnvs = envVariablesComponent.isPassParentEnvs

        configuration.listeningTeardown = listeningTearDown.isSelected
        configuration.cleanRuntimeDir = cleanRuntimeDir.isSelected
    }

    override fun createEditor(): JComponent = panel
    override fun getAnchor(): JComponent? = myAnchor
    override fun setAnchor(anchor: JComponent?) {
        myAnchor = anchor

        productComponent.anchor = anchor
        applicationComponent.anchor = anchor
        remoteHostComponent.anchor = anchor
        rmiPortComponent.anchor = anchor
        rmiNameComponent.anchor = anchor
        remotePortComponent.anchor = anchor
        vmParametersComponent.anchor = anchor
        programParametersComponent.anchor = anchor
        envVariablesComponent.anchor = anchor
    }

    enum class JDKVersionItem {
        JDK9 {
            override fun toString(): String = "JDK 9 or later"
            override fun getLaunchCommandLine(connection: RemoteConnection): String =
                JDK5to8.getLaunchCommandLine(connection)
                    .replace(connection.applicationAddress, "*:" + connection.applicationAddress)
        },
        JDK5to8 {
            override fun toString(): String = "JDK 5 - 8"
            override fun getLaunchCommandLine(connection: RemoteConnection): String =
                connection.launchCommandLine.replace("-Xdebug", "").replace("-Xrunjdwp:", "-agentlib:jdwp=")
                    .trim { it <= ' ' }
        },
        JDK1_4 {
            override fun toString(): String = "JDK 1.4.x"
            override fun getLaunchCommandLine(connection: RemoteConnection): String = connection.launchCommandLine
        },
        JDK1_3 {
            override fun toString(): String = "JDK 1.3.x or earlier"
            override fun getLaunchCommandLine(connection: RemoteConnection): String =
                "-Xnoagent -Djava.compiler=NONE " + connection.launchCommandLine
        };

        abstract fun getLaunchCommandLine(connection: RemoteConnection): String
    }
}
