package cn.varsa.idea.pde.partial.plugin.config

import cn.varsa.idea.pde.partial.plugin.i18n.EclipsePDEPartialBundles.message
import cn.varsa.idea.pde.partial.plugin.listener.*
import com.intellij.icons.*
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.fileChooser.*
import com.intellij.openapi.options.*
import com.intellij.openapi.project.*
import com.intellij.openapi.roots.*
import com.intellij.openapi.ui.*
import com.intellij.ui.*
import com.intellij.ui.components.*
import com.intellij.ui.components.panels.*
import com.intellij.util.ui.*
import com.intellij.util.ui.components.*
import org.jetbrains.kotlin.idea.util.*
import java.awt.*
import java.awt.event.*
import javax.swing.*

class TargetConfigurable(private val project: Project) : SearchableConfigurable, PanelWithAnchor {
    private val service by lazy { TargetDefinitionService.getInstance(project) }
    private var myAnchor: JComponent? = null
    private val locationModified = mutableSetOf<Pair<TargetLocationDefinition?, TargetLocationDefinition?>>()

    private val panel = JBTabbedPane()

    private val launcherJarCombo = ComboBox<String>()
    private val launcherCombo = ComboBox<String>()
    private val launcherJar =
        LabeledComponent.create(launcherJarCombo, message("config.target.launcherJar"), BorderLayout.WEST)
    private val launcher = LabeledComponent.create(launcherCombo, message("config.target.launcher"), BorderLayout.WEST)

    private val locationModel = DefaultListModel<TargetLocationDefinition>()
    private val locationList = JBList(locationModel).apply {
        setEmptyText(message("config.target.empty"))
        cellRenderer = object : ColoredListCellRenderer<TargetLocationDefinition>() {
            override fun customizeCellRenderer(
                list: JList<out TargetLocationDefinition>,
                value: TargetLocationDefinition?,
                index: Int,
                selected: Boolean,
                hasFocus: Boolean
            ) {
                value?.also {
                    append(it.location)
                    append(
                        message("config.target.locationInfoInfix", it.bundles.size, it.dependency),
                        SimpleTextAttributes.GRAY_ATTRIBUTES
                    )
                }
            }
        }
        object : DoubleClickListener() {
            override fun onDoubleClick(event: MouseEvent): Boolean {
                val selection = !isSelectionEmpty
                if (selection) editLocation()
                return selection
            }
        }.installOn(this)
    }

    private val startupModel = DefaultListModel<Pair<String, Int>>()
    private val startupList = JBList(startupModel).apply {
        setEmptyText(message("config.startup.empty"))
        cellRenderer = object : ColoredListCellRenderer<Pair<String, Int>>() {
            override fun customizeCellRenderer(
                list: JList<out Pair<String, Int>>,
                value: Pair<String, Int>?,
                index: Int,
                selected: Boolean,
                hasFocus: Boolean
            ) {
                value?.also {
                    append(it.first)
                    append(" -> ", SimpleTextAttributes.GRAY_ATTRIBUTES)
                    append(it.second.toString())
                }
            }
        }
        object : DoubleClickListener() {
            override fun onDoubleClick(event: MouseEvent): Boolean {
                val selection = !isSelectionEmpty
                if (selection) editStartup()
                return selection
            }
        }.installOn(this)
    }

    init {
        // Target tab
        val reloadActionButton = object : AnActionButton(message("config.target.reload"), AllIcons.Actions.Refresh) {
            override fun actionPerformed(e: AnActionEvent) = locationList.selectedValue.backgroundResolve(project)
        }.apply {
            isEnabled = false
            locationList.addListSelectionListener { isEnabled = locationList.isSelectionEmpty.not() }
        }

        val launcherPanel = VerticalBox().apply {
            add(launcherJar)
            add(launcher)
        }
        val locationsPanel = BorderLayoutPanel().withBorder(
            IdeBorderFactory.createTitledBorder(
                message("config.target.borderHint"), false, JBUI.insetsTop(8)
            ).setShowLine(true)
        ).addToCenter(ToolbarDecorator.createDecorator(locationList).setAddAction { addLocation() }
                          .setRemoveAction { removeLocation() }.setEditAction { editLocation() }
                          .addExtraAction(reloadActionButton).createPanel()).addToBottom(launcherPanel)

        panel.addTab(message("config.target.tab"), locationsPanel)

        // Startup tab
        val startupPanel = BorderLayoutPanel().withBorder(
            IdeBorderFactory.createTitledBorder(
                message("config.startup.borderHint"), false, JBUI.insetsTop(8)
            ).setShowLine(true)
        ).addToCenter(ToolbarDecorator.createDecorator(startupList).setAddAction { addStartup() }
                          .setRemoveAction { removeStartup() }.setEditAction { editStartup() }.createPanel())

        panel.addTab(message("config.startup.tab"), startupPanel)

        // Anchor
        myAnchor = UIUtil.mergeComponentsWithAnchor(launcherJar, launcher)
    }

    override fun createComponent(): JComponent = panel
    override fun getDisplayName(): String = message("config.displayName")
    override fun getId(): String = "cn.varsa.idea.pde.partial.plugin.config.TargetConfigurable"
    override fun getHelpTopic(): String = id

    override fun getAnchor(): JComponent? = myAnchor
    override fun setAnchor(anchor: JComponent?) {
        myAnchor = anchor

        launcherJar.anchor = anchor
        launcher.anchor = anchor
    }

    override fun isModified(): Boolean {
        locationModified.isNotEmpty().ifTrue { return true }
        if (launcherJarCombo.item != service.launcherJar || launcherCombo.item != service.launcher) return true

        val locations = locationModel.elements().toList()
        if (locations.size != service.locations.size) return true
        locations.run { mapIndexed { index, def -> def != service.locations[index] }.any { it } }.ifTrue { return true }

        val startups = startupModel.elements().toList()
        if (startups.size != service.startupLevels.size) return true
        service.startupLevels.entries.run { mapIndexed { index, entry -> startups[index].run { first != entry.key || second != entry.value } } }
            .any { it }.ifTrue { return true }

        return false
    }

    override fun apply() {
        service.launcherJar = launcherJarCombo.item
        service.launcher = launcherCombo.item

        service.locations.also {
            it.clear()
            it += locationModel.elements().toList()
        }

        service.startupLevels.also {
            it.clear()
            it += startupModel.elements().toList()
        }

        TargetDefinitionChangeListener.notifyLocationsChanged(project)
        locationModified.clear()
    }

    override fun reset() {
        locationModel.clear()
        locationModified.clear()
        startupModel.clear()

        launcherJarCombo.item = service.launcherJar
        launcherCombo.item = service.launcher

        service.locations.forEach(locationModel::addElement)
        service.startupLevels.forEach { startupModel.addElement(it.toPair()) }

        updateComboBox()
    }

    private fun updateComboBox() {
        launcherJarCombo.also { comboBox ->
            comboBox.removeAllItems()
            locationModel.elements().toList().mapNotNull(TargetLocationDefinition::launcherJar).distinct()
                .forEach(comboBox::addItem)
        }

        launcherCombo.also { comboBox ->
            comboBox.removeAllItems()
            locationModel.elements().toList().mapNotNull(TargetLocationDefinition::launcher).distinct()
                .forEach(comboBox::addItem)
        }
    }

    private fun addLocation() {
        val dialog = EditLocationDialog()
        if (dialog.showAndGet()) {
            val location = dialog.getNewLocation()

            locationModel.addElement(location)
            locationList.setSelectedValue(location, true)
            locationModified += Pair(null, location)

            location.backgroundResolve(project, onFinished = { updateComboBox() })
        }
    }

    private fun removeLocation() {
        if (locationList.isSelectionEmpty.not()) {
            val location = locationList.selectedValue
            locationModel.removeElement(location)
            locationModified += Pair(location, null)
            updateComboBox()
        }
    }

    private fun editLocation() {
        if (locationList.isSelectionEmpty.not()) {
            val index = locationList.selectedIndex
            val value = locationList.selectedValue

            val dialog = EditLocationDialog(defaultPath = value.location)
            if (dialog.showAndGet()) {
                val location = dialog.getNewLocation()

                locationModel.set(index, location)
                locationList.setSelectedValue(location, true)
                locationModified += Pair(value, location)

                location.backgroundResolve(project, onFinished = { updateComboBox() })
            }
        }
    }

    private fun addStartup() {
        val dialog = EditStartupDialog()
        if (dialog.showAndGet()) {
            val level = dialog.getNewLevel()

            val index = startupModel.elements().toList().indexOfFirst { it.first == level.first }
            if (index > -1) {
                startupModel.set(index, level)
            } else {
                startupModel.addElement(level)
            }

            startupList.setSelectedValue(level, true)
        }
    }

    private fun removeStartup() {
        if (startupList.isSelectionEmpty.not()) {
            startupModel.removeElement(startupList.selectedValue)
        }
    }

    private fun editStartup() {
        if (startupList.isSelectionEmpty.not()) {
            val index = startupList.selectedIndex
            val value = startupList.selectedValue

            val dialog = EditStartupDialog(symbolicName = value.first, level = value.second)
            if (dialog.showAndGet()) {
                val level = dialog.getNewLevel()

                startupModel.set(index, level)
                startupList.setSelectedValue(level, true)
            }
        }
    }

    inner class EditLocationDialog(
        title: String = message("config.target.locationDialog.title"),
        private val description: String = message("config.target.locationDialog.description"),
        private val defaultPath: String = "",
    ) : DialogWrapper(project), PanelWithAnchor {
        private val fileDescription = FileChooserDescriptorFactory.createSingleFolderDescriptor()
        private var myAnchor: JComponent? = null

        private val pathField =
            FileChooserFactory.getInstance().createFileTextField(fileDescription, myDisposable).field.apply {
                columns = 25
            }
        private val pathComponent = TextFieldWithBrowseButton(pathField).apply {
            addBrowseFolderListener(title, description, project, fileDescription)
            text = defaultPath
        }.let { LabeledComponent.create(it, description, BorderLayout.WEST) }

        private val dependencyComboBox = ComboBox(DependencyScope.values().map { it.displayName }.toTypedArray())
        private val dependencyComponent = LabeledComponent.create(
            dependencyComboBox, message("config.target.locationDialog.dependency"), BorderLayout.WEST
        )

        init {
            setTitle(title)
            init()

            anchor = UIUtil.mergeComponentsWithAnchor(pathComponent, dependencyComponent)
        }

        override fun createCenterPanel(): JComponent = VerticalBox().apply {
            add(pathComponent)
            add(dependencyComponent)
        }

        override fun getAnchor(): JComponent? = myAnchor
        override fun setAnchor(anchor: JComponent?) {
            myAnchor = anchor

            pathComponent.anchor = anchor
            dependencyComponent.anchor = anchor
        }

        fun getNewLocation(): TargetLocationDefinition = TargetLocationDefinition(pathField.text).apply {
            dependency = dependencyComboBox.item ?: DependencyScope.COMPILE.displayName
        }
    }

    inner class EditStartupDialog(
        title: String = message("config.target.startupDialog.title"),
        description: String = message("config.target.startupDialog.description"),
        symbolicName: String = "",
        level: Int = 4,
    ) : DialogWrapper(project), PanelWithAnchor {
        private var myAnchor: JComponent? = null

        private val nameTextField = JBTextField(symbolicName)
        private val levelSpinner = JBIntSpinner(level, -1, Int.MAX_VALUE, 1)

        private val nameComponent = LabeledComponent.create(nameTextField, description, BorderLayout.WEST)
        private val levelComponent =
            LabeledComponent.create(levelSpinner, message("config.target.startupDialog.level"), BorderLayout.WEST)

        init {
            setTitle(title)
            init()

            anchor = UIUtil.mergeComponentsWithAnchor(nameComponent, levelComponent)
        }

        override fun createCenterPanel(): JComponent = VerticalBox().apply {
            add(nameComponent)
            add(levelComponent)
        }

        override fun getAnchor(): JComponent? = myAnchor
        override fun setAnchor(anchor: JComponent?) {
            myAnchor = anchor

            nameComponent.anchor = anchor
            levelComponent.anchor = anchor
        }

        fun getNewLevel(): Pair<String, Int> = Pair(nameTextField.text, levelSpinner.number)
    }
}
