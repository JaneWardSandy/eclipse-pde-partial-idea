package cn.varsa.idea.pde.partial.plugin.config

import cn.varsa.idea.pde.partial.common.*
import cn.varsa.idea.pde.partial.plugin.domain.*
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
import com.intellij.ui.table.*
import com.intellij.util.ui.*
import com.intellij.util.ui.components.*
import com.jetbrains.rd.util.*
import org.jetbrains.kotlin.idea.util.*
import java.awt.*
import java.awt.event.*
import javax.swing.*
import javax.swing.table.*

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

    private val contentList = arrayListOf<BundleVersionRow>()
    private val contentModel = ListTableModel(arrayOf(object : ColumnInfo<BundleVersionRow, Boolean>(
        message("config.content.table.column.checked")
    ) {
        override fun getWidth(table: JTable?): Int = 50
        override fun getColumnClass(): Class<*> = Boolean::class.java
        override fun valueOf(item: BundleVersionRow?): Boolean? = item?.checked
        override fun getComparator(): Comparator<BundleVersionRow>? = Comparator.comparing(BundleVersionRow::checked)
        override fun getRenderer(item: BundleVersionRow?): TableCellRenderer = BooleanTableCellRenderer()
        override fun isCellEditable(item: BundleVersionRow?): Boolean = true
        override fun getEditor(item: BundleVersionRow?): TableCellEditor = BooleanTableCellEditor()
        override fun setValue(item: BundleVersionRow?, value: Boolean?) {
            value?.also { item?.checked = it }
        }
    }, object : ColumnInfo<BundleVersionRow, String>(message("config.content.table.column.symbolName")) {
        override fun valueOf(item: BundleVersionRow?): String? = item?.symbolicName
        override fun getComparator(): Comparator<BundleVersionRow>? =
            Comparator.comparing(BundleVersionRow::symbolicName)
    }, object : ColumnInfo<BundleVersionRow, String>(message("config.content.table.column.version")) {
        override fun valueOf(item: BundleVersionRow?): String? = item?.version
        override fun getComparator(): Comparator<BundleVersionRow>? = Comparator.comparing(BundleVersionRow::version)
        override fun isCellEditable(item: BundleVersionRow?): Boolean = item?.availableVersions?.isNotEmpty() ?: false
        override fun getEditor(item: BundleVersionRow?): TableCellEditor =
            ComboBoxTableRenderer(item?.availableVersions?.toTypedArray() ?: arrayOf(""))

        override fun setValue(item: BundleVersionRow?, value: String?) {
            value?.also { item?.version = it }
        }
    }, object : ColumnInfo<BundleVersionRow, String>(message("config.content.table.column.sourceVersion")) {
        override fun valueOf(item: BundleVersionRow?): String? = item?.sourceVersion
        override fun getComparator(): Comparator<BundleVersionRow>? =
            Comparator.comparing(BundleVersionRow::sourceVersion)

        override fun isCellEditable(item: BundleVersionRow?): Boolean =
            item?.availableSourceVersions?.isNotEmpty() ?: false

        override fun getEditor(item: BundleVersionRow?): TableCellEditor =
            ComboBoxTableRenderer(item?.availableSourceVersions?.toTypedArray() ?: arrayOf(""))

        override fun setValue(item: BundleVersionRow?, value: String?) {
            value?.also { item?.sourceVersion = it }
        }
    }), contentList, 1)
    private val contentTable = TableView(contentModel)

    init {
        // Target tab
        val reloadActionButton = object : AnActionButton(message("config.target.reload"), AllIcons.Actions.Refresh) {
            override fun actionPerformed(e: AnActionEvent) =
                locationList.selectedValue.backgroundResolve(project, onFinished = { updateComboBox() })
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


        // Content tab
        val contentPanel = BorderLayoutPanel().withBorder(
            IdeBorderFactory.createTitledBorder(
                message("config.content.borderHint"), false, JBUI.insetsTop(8)
            ).setShowLine(true)
        ).addToCenter(
            ToolbarDecorator.createDecorator(contentTable).disableAddAction().disableRemoveAction()
                .disableUpDownActions().addExtraActions(object : AnActionButton(
                    message("config.target.reload"), AllIcons.Actions.Refresh
                ) {
                    override fun actionPerformed(e: AnActionEvent) = reloadContentList(
                        locationModel.elements().toList().flatMap { it.bundles }, service.bundleVersionSelection
                    )
                }, object : AnActionButton(message("config.content.reload"), AllIcons.Actions.ForceRefresh) {
                    override fun actionPerformed(e: AnActionEvent) =
                        reloadContentList(locationModel.elements().toList().flatMap { it.bundles }, hashMapOf())
                }).createPanel()
        )

        panel.addTab(message("config.content.tab"), contentPanel)


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

        val versionMap = contentList.filter { it.checked }.flatMap {
            listOf(
                it.symbolicName to it.version, "${it.symbolicName}$BundleSymbolNameSourcePostFix" to it.sourceVersion
            )
        }.filterNot { it.first.isBlank() || it.second.isBlank() }.toMap()
        if (versionMap.size != service.bundleVersionSelection.size) return true
        service.bundleVersionSelection.run { !keys.containsAll(versionMap.keys) || !versionMap.keys.containsAll(keys) || any { versionMap[it.key] != it.value } }
            .ifTrue { return true }

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

        service.bundleVersionSelection.also { map ->
            map.clear()
            map += contentList.filter { it.checked }.flatMap {
                listOf(
                    it.symbolicName to it.version,
                    "${it.symbolicName}$BundleSymbolNameSourcePostFix" to it.sourceVersion
                )
            }.filterNot { it.first.isBlank() || it.second.isBlank() }
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
        reloadContentList(service.locations.flatMap { it.bundles }, service.bundleVersionSelection)
    }

    private fun reloadContentList(bundles: List<BundleDefinition>, versionMap: HashMap<String, String>) {
        val map = ConcurrentHashMap<String, BundleVersionRow>(bundles.size)
        bundles.forEach { bundle ->
            bundle.manifest?.also { manifest ->
                val eclipseSourceBundle = manifest.eclipseSourceBundle
                if (eclipseSourceBundle != null) {
                    map.computeIfAbsent(eclipseSourceBundle.key) {
                        BundleVersionRow(it).apply {
                            checked =
                                versionMap.isEmpty() || versionMap.containsKey(it) || versionMap.containsKey("$it$BundleSymbolNameSourcePostFix")
                        }
                    }.apply {
                        manifest.bundleVersion?.toString()?.also {
                            sourceVersion.isBlank().ifTrue {
                                sourceVersion = versionMap["$symbolicName$BundleSymbolNameSourcePostFix"] ?: it
                            }
                            availableSourceVersions += it
                        }
                    }
                } else {
                    map.computeIfAbsent(bundle.bundleSymbolicName) {
                        BundleVersionRow(it).apply { checked = versionMap.isEmpty() || versionMap.containsKey(it) }
                    }.apply {
                        manifest.bundleVersion?.toString()?.also {
                            version.isBlank().ifTrue { version = versionMap[bundle.bundleSymbolicName] ?: it }
                            availableVersions += it
                        }
                    }
                }
            }
        }

        contentList.clear()
        contentList += map.values
        contentModel.items = contentList
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

    private data class BundleVersionRow(val symbolicName: String) {
        var checked: Boolean = true
        var version: String = ""
        var sourceVersion: String = ""

        val availableVersions = hashSetOf<String>()
        val availableSourceVersions = hashSetOf<String>()
    }

}
