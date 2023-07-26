package cn.varsa.idea.pde.partial.plugin.config

import cn.varsa.idea.pde.partial.common.*
import cn.varsa.idea.pde.partial.common.support.*
import cn.varsa.idea.pde.partial.plugin.domain.*
import cn.varsa.idea.pde.partial.plugin.i18n.EclipsePDEPartialBundles.message
import cn.varsa.idea.pde.partial.plugin.listener.*
import cn.varsa.idea.pde.partial.plugin.support.*
import com.intellij.icons.*
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.fileChooser.*
import com.intellij.openapi.options.*
import com.intellij.openapi.project.*
import com.intellij.openapi.roots.*
import com.intellij.openapi.ui.*
import com.intellij.psi.*
import com.intellij.ui.*
import com.intellij.ui.components.*
import com.intellij.ui.components.panels.*
import com.intellij.ui.speedSearch.*
import com.intellij.util.ui.*
import com.intellij.util.ui.components.*
import com.jetbrains.rd.swing.*
import com.jetbrains.rd.util.reactive.*
import org.osgi.framework.*
import org.osgi.framework.Constants.*
import java.awt.*
import java.awt.event.*
import java.util.*
import javax.swing.*
import javax.swing.tree.*

class TargetConfigurable(private val project: Project) : SearchableConfigurable, PanelWithAnchor {
  private val service by lazy { TargetDefinitionService.getInstance(project) }
  private var launcherAnchor: JComponent? = null
  private val locationModified = mutableSetOf<Pair<TargetLocationDefinition?, TargetLocationDefinition?>>()

  private val panel = JBTabbedPane()

  private val launcherJarCombo = ComboBox<String>()
  private val launcherCombo = ComboBox<String>()
  private val launcherJar =
    LabeledComponent.create(launcherJarCombo, message("config.target.launcherJar"), BorderLayout.WEST)
  private val launcher = LabeledComponent.create(launcherCombo, message("config.target.launcher"), BorderLayout.WEST)

  private val locationModel = object : DefaultListModel<TargetLocationDefinition>() {
    fun reload(location: TargetLocationDefinition) {
      reload(indexOf(location))
    }

    fun reload(index: Int) {
      fireContentsChanged(this, index, index)
    }
  }
  private val locationList = JBList(locationModel).apply {
    setEmptyText(message("config.target.empty"))
    cellRenderer = ColoredListCellRendererWithSpeedSearch<TargetLocationDefinition> { value ->
      value?.also { location ->
        location.type?.takeIf(String::isNotBlank)?.also {
          append("[$it] ", SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
        }
        location.alias?.takeIf(String::isNotBlank)?.also {
          append(it, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
          append(": ")
        }
        append(location.location)
        append(
          message("config.target.locationInfoInfix", location.bundles.size, location.dependency),
          SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES
        )
      }
    }
    object : DoubleClickListener() {
      override fun onDoubleClick(event: MouseEvent): Boolean {
        val selection = !isSelectionEmpty
        if (selection) editLocation()
        return selection
      }
    }.installOn(this)
    ListSpeedSearch.installOn(this).setClearSearchOnNavigateNoMatch(true)
  }

  private val startupModel = DefaultListModel<Pair<String, Int>>()
  private val startupList = JBList(startupModel).apply {
    setEmptyText(message("config.startup.empty"))
    cellRenderer = ColoredListCellRendererWithSpeedSearch<Pair<String, Int>> { value ->
      value?.also {
        append(it.first)
        append(" -> ", SimpleTextAttributes.GRAY_ATTRIBUTES)
        append(it.second.toString())
      }
    }
    object : DoubleClickListener() {
      override fun onDoubleClick(event: MouseEvent): Boolean {
        val selection = !isSelectionEmpty
        if (selection) editStartup()
        return selection
      }
    }.installOn(this)
    ListSpeedSearch.installOn(this).setClearSearchOnNavigateNoMatch(true)
  }

  private val contentTree = CheckboxTree(object : CheckboxTree.CheckboxTreeCellRenderer(true) {
    override fun customizeRenderer(
      tree: JTree, value: Any?, selected: Boolean, expanded: Boolean, leaf: Boolean, row: Int, hasFocus: Boolean
    ) {
      when (value) {
        is ShadowLocation -> {
          textRenderer.append(value.location.identifier)
          textRenderer.append(
            message("config.content.bundlesInfoInfix", value.bundles.size, value.bundles.count { it.isChecked }),
            SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES
          )
        }

        is ShadowFeature -> {
          textRenderer.append(value.toString())
        }

        is ShadowBundle -> {
          textRenderer.append(value.bundle.canonicalName)

          value.sourceBundle?.bundleVersion?.also {
            textRenderer.append(" / source: [$it]", SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES)
          }
        }
      }
      SpeedSearchUtil.applySpeedSearchHighlighting(tree, textRenderer, false, selected)
    }
  }, ShadowLocationRoot)
  private val contentTreeModel = contentTree.model as DefaultTreeModel

  private val sourceVersionField = ComboBox<BundleDefinition>().apply {
    renderer = ColoredListCellRendererWithSpeedSearch<BundleDefinition> { value ->
      value?.canonicalName?.also { append(it) }
    }
    isEnabled = false
  }
  private val sourceVersionComponent = LabeledComponent.create(
    sourceVersionField, message("config.content.sourceVersion"), BorderLayout.WEST
  )

  init {
    // Target tab
    val reloadActionButton = object : AnAction(message("config.target.reload"), null, AllIcons.Actions.Refresh) {
      override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
      override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = locationList.isSelectionEmpty.not()
      }

      override fun actionPerformed(e: AnActionEvent) = locationList.selectedValue.let {
        it.backgroundResolve(project, onFinished = {
          locationModified += it to it
          updateComboBox()
        })
      }
    }.apply {
      locationList.addListSelectionListener {
        val event = AnActionEvent.createFromDataContext("", null) {}
        update(event)
      }
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
      ToolbarDecorator.createDecorator(contentTree).disableAddAction().disableRemoveAction().disableUpDownActions()
        .addExtraActions(
          object : AnAction(message("config.target.reload"), null, AllIcons.Actions.Refresh) {
            override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
            override fun actionPerformed(e: AnActionEvent) = reloadContentList()
          },
          object : AnAction(message("config.content.reload"), null, AllIcons.Actions.ForceRefresh) {
            override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
            override fun actionPerformed(e: AnActionEvent) = reloadContentListByDefaultRule()
          },
          object : AnAction(message("config.content.validate"), null, AllIcons.Diff.GutterCheckBoxSelected) {
            override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
            override fun actionPerformed(e: AnActionEvent) = ValidateAndResolveBundleDependencies().show()
          },
        ).createPanel()
    ).addToBottom(sourceVersionComponent)

    var selectedShadowBundle: ShadowBundle? = null
    sourceVersionField.selectedItemProperty().adviseEternal {
      selectedShadowBundle?.apply {
        sourceBundle = it
        contentTreeModel.reload(this)
      }
    }
    contentTree.addTreeSelectionListener {
      selectedShadowBundle = null
      sourceVersionField.apply {
        removeAllItems()

        isEnabled = (it.path?.lastPathComponent as? ShadowBundle)?.let { bundle ->
          addItem(null)
          ShadowLocationRoot.sourceVersions[bundle.bundle.bundleSymbolicName]?.forEach(this::addItem)
          item = bundle.sourceBundle
          selectedShadowBundle = bundle
          true
        } ?: false
      }
    }

    panel.addTab(message("config.content.tab"), contentPanel)


    // Anchor
    launcherAnchor = UIUtil.mergeComponentsWithAnchor(launcherJar, launcher)
  }

  override fun createComponent(): JComponent = panel
  override fun getDisplayName(): String = message("config.displayName")
  override fun getId(): String = "cn.varsa.idea.pde.partial.plugin.config.TargetConfigurable"
  override fun getHelpTopic(): String = id

  override fun getAnchor(): JComponent? = launcherAnchor
  override fun setAnchor(anchor: JComponent?) {
    launcherAnchor = anchor

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

    ShadowLocationRoot.locations.any { it.isModify }.ifTrue { return true }

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

    ShadowLocationRoot.locations.forEach(ShadowLocation::apply)

    TargetDefinitionChangeListener.notifyLocationsChanged(project)
    locationModified.clear()
  }

  override fun reset() {
    locationModel.clear()
    locationModified.clear()
    startupModel.clear()

    ShadowLocationRoot.also {
      it.removeAllChildren()
      it.sourceVersions.clear()
    }

    launcherJarCombo.item = service.launcherJar
    launcherCombo.item = service.launcher

    service.locations.forEach {
      locationModel.addElement(it)
      ShadowLocationRoot.addLocation(it)
    }
    service.startupLevels.forEach { startupModel.addElement(it.toPair()) }

    updateComboBox()
    reloadContentList()
    ShadowLocationRoot.sort()
    contentTreeModel.reload()
  }

  private fun reloadContentList() {
    ShadowLocationRoot.locations.forEach { location ->
      location.reset()
      location.bundles.filter { it.sourceBundle == null }.forEach { bundle ->
        bundle.sourceBundle = ShadowLocationRoot.sourceVersions[bundle.bundle.bundleSymbolicName]?.let { set ->
          set.firstOrNull { it.bundleVersion == bundle.bundle.bundleVersion }
        }
      }
      contentTreeModel.reload(location)
    }
  }

  private fun reloadContentListByDefaultRule() {
    ShadowLocationRoot.locations.forEach { location ->
      location.reset()
      location.bundles.forEach { bundle ->
        bundle.sourceBundle = ShadowLocationRoot.sourceVersions[bundle.bundle.bundleSymbolicName]?.let { set ->
          set.firstOrNull { it.bundleVersion == bundle.bundle.bundleVersion }
        }
      }
      contentTreeModel.reload(location)
    }
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

      location.backgroundResolve(project, onFinished = {
        updateComboBox()
        ShadowLocationRoot.addLocation(location)
        ShadowLocationRoot.sort()

        locationModel.reload(location)
        contentTreeModel.reload()
      })
    }
  }

  private fun removeLocation() {
    if (locationList.isSelectionEmpty.not()) {
      val location = locationList.selectedValue
      locationModel.removeElement(location)
      locationModified += Pair(location, null)

      updateComboBox()
      ShadowLocationRoot.removeLocation(location)
    }
  }

  private fun editLocation() {
    if (locationList.isSelectionEmpty.not()) {
      val index = locationList.selectedIndex
      val value = locationList.selectedValue

      val dialog = EditLocationDialog(defaultPath = value.location, defaultAlis = value.alias ?: "")
      if (dialog.showAndGet()) {
        val location = dialog.getNewLocation()

        locationModel.set(index, location)
        locationList.setSelectedValue(location, true)
        locationModified += Pair(value, location)

        location.backgroundResolve(project, onFinished = {
          updateComboBox()
          ShadowLocationRoot.replaceLocation(location, value)
          ShadowLocationRoot.sort()

          locationModel.reload(index)
          contentTreeModel.reload()
        })
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
    defaultPath: String = "",
    defaultAlis: String = ""
  ) : DialogWrapper(project), PanelWithAnchor {
    private val fileDescription = FileChooserDescriptorFactory.createSingleFolderDescriptor()
    private var myAnchor: JComponent? = null

    private val aliasField = JBTextField(defaultAlis)
    private val aliasComponent =
      LabeledComponent.create(aliasField, message("config.target.locationDialog.alias"), BorderLayout.WEST)

    private val pathField =
      FileChooserFactory.getInstance().createFileTextField(fileDescription, myDisposable).field.apply {
        columns = 25
        text = defaultPath
      }
    private val pathComponent = TextFieldWithBrowseButton(pathField).apply {
      addBrowseFolderListener(title, description, project, fileDescription)
    }.let { LabeledComponent.create(it, description, BorderLayout.WEST) }

    private val dependencyComboBox = ComboBox(DependencyScope.values().map { it.displayName }.toTypedArray())
    private val dependencyComponent = LabeledComponent.create(
      dependencyComboBox, message("config.target.locationDialog.dependency"), BorderLayout.WEST
    )

    init {
      setTitle(title)
      init()

      anchor = UIUtil.mergeComponentsWithAnchor(aliasComponent, pathComponent, dependencyComponent)
    }

    override fun createCenterPanel(): JComponent = VerticalBox().apply {
      add(aliasComponent)
      add(pathComponent)
      add(dependencyComponent)
    }

    override fun getAnchor(): JComponent? = myAnchor
    override fun setAnchor(anchor: JComponent?) {
      myAnchor = anchor

      aliasComponent.anchor = anchor
      pathComponent.anchor = anchor
      dependencyComponent.anchor = anchor
    }

    fun getNewLocation(): TargetLocationDefinition = TargetLocationDefinition(pathField.text).apply {
      alias = aliasField.text
      dependency = dependencyComboBox.item ?: DependencyScope.COMPILE.displayName
    }
  }

  inner class EditStartupDialog(
    title: String = message("config.startup.startupDialog.title"),
    description: String = message("config.startup.startupDialog.description"),
    symbolicName: String = "",
    level: Int = 4,
  ) : DialogWrapper(project), PanelWithAnchor {
    private var myAnchor: JComponent? = null

    private val nameTextField = JBTextField(symbolicName)
    private val levelSpinner = JBIntSpinner(level, -1, Int.MAX_VALUE, 1)

    private val nameComponent = LabeledComponent.create(nameTextField, description, BorderLayout.WEST)
    private val levelComponent =
      LabeledComponent.create(levelSpinner, message("config.startup.startupDialog.level"), BorderLayout.WEST)

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

  inner class ValidateAndResolveBundleDependencies : DialogWrapper(project) {
    private val root = CheckedTreeNode()
    private val treeModel = DefaultTreeModel(root)
    private val tree = CheckboxTree(object : CheckboxTree.CheckboxTreeCellRenderer(true) {
      override fun customizeRenderer(
        tree: JTree, value: Any?, selected: Boolean, expanded: Boolean, leaf: Boolean, row: Int, hasFocus: Boolean
      ) {
        value?.toString()?.also { textRenderer.append(it) }
        SpeedSearchUtil.applySpeedSearchHighlighting(tree, textRenderer, false, selected)
      }
    }, null).apply { model = treeModel }

    private val bundle2FixBundle: HashMap<ShadowBundle, HashSet<FixBundle>>

    init {
      title = message("config.content.validateDialog.title")
      init()

      val javaPsiFacade = JavaPsiFacade.getInstance(project)
      val index = ProjectFileIndex.getInstance(project)

      val initialCapacity = ShadowLocationRoot.locations.sumOf { it.bundles.size }
      val bundles = HashMap<String, HashMap<Version, HashSet<ShadowBundle>>>(initialCapacity)
      val exportedPackages = HashMap<String, HashMap<Version, HashSet<ShadowBundle>>>(initialCapacity)

      ShadowLocationRoot.locations.flatMap { it.bundles }.forEach { bundle ->
        bundles.computeIfAbsent(bundle.bundle.bundleSymbolicName) { hashMapOf() }
          .computeIfAbsent(bundle.bundle.bundleVersion) { hashSetOf() } += bundle

        bundle.bundle.manifest?.exportedPackageAndVersion()?.forEach { (packageName, version) ->
          exportedPackages.computeIfAbsent(packageName) { hashMapOf() }
            .computeIfAbsent(version) { hashSetOf() } += bundle
        }
      }

      bundle2FixBundle = HashMap<ShadowBundle, HashSet<FixBundle>>(initialCapacity)
      bundles.values.flatMap { it.values }.flatten().filter { it.isChecked }.sortedBy { it.bundle.canonicalName }
        .forEach { bundle ->
          var problem: ProblemBundle? = null

          bundle.bundle.manifest?.importPackage?.forEach { (packageName, attrs) ->
            if (attrs.directive[RESOLUTION_DIRECTIVE] != RESOLUTION_OPTIONAL && javaPsiFacade.findPackage(
                packageName
              )?.directories?.mapNotNull { it.virtualFile }?.any { it.isBelongJDK(index) } != true
            ) {
              val map = exportedPackages[packageName]
              if (map.isNullOrEmpty()) {
                if (problem == null) problem = ProblemBundle(bundle)
                problem!!.add(MissingImportedPackage(packageName + attrs))
              } else {
                val range = attrs.attribute[VERSION_ATTRIBUTE].parseVersionRange()
                if (map.none { (version, set) -> version in range && set.any { it.isChecked } }) {
                  val missingImportedPackage = MissingImportedPackage(packageName + attrs)
                  map.values.flatten().distinct().forEach { missingImportedPackage.add(FixBundle(it)) }

                  if (problem == null) problem = ProblemBundle(bundle)
                  problem!!.add(missingImportedPackage)
                }
              }
            }
          }

          bundle.bundle.manifest?.requireBundle?.forEach { (requiredBundle, attrs) ->
            if (attrs.directive[RESOLUTION_DIRECTIVE] != RESOLUTION_OPTIONAL) {
              var map = bundles[requiredBundle]
              if (requiredBundle == SystemBundle && map.isNullOrEmpty()) map = bundles[OrgEclipseOSGI]

              if (map.isNullOrEmpty()) {
                if (problem == null) problem = ProblemBundle(bundle)
                problem!!.add(MissingRequiredBundle(requiredBundle + attrs))
              } else {
                val range = attrs.attribute[BUNDLE_VERSION_ATTRIBUTE].parseVersionRange()
                if (map.none { (version, set) -> version in range && set.any { it.isChecked } }) {
                  val missingRequiredBundle = MissingRequiredBundle(requiredBundle + attrs)
                  map.values.flatten().distinct().forEach { missingRequiredBundle.add(FixBundle(it)) }

                  if (problem == null) problem = ProblemBundle(bundle)
                  problem!!.add(missingRequiredBundle)
                }
              }
            }
          }

          problem?.also { root.add(it) }
        }

      treeModel.reload()
    }

    override fun createCenterPanel(): JComponent = BorderLayoutPanel().withBorder(
      IdeBorderFactory.createTitledBorder(
        message("config.content.validateDialog.leadTitle"), false, JBUI.insetsTop(8)
      ).setShowLine(true)
    ).addToCenter(JBScrollPane(tree))

    override fun processDoNotAskOnOk(exitCode: Int) {
      super.processDoNotAskOnOk(exitCode)

      root.children().asSequence().mapNotNull { it as? ProblemBundle }
        .flatMap { problemBundle -> problemBundle.importedPackage.flatMap { it.fixBundle } + problemBundle.requiredBundle.flatMap { it.fixBundle } }
        .map { it.bundle }.distinct().forEach {
          it.isChecked = true
          contentTreeModel.reload(it)
        }
    }

    private inner class ProblemBundle(val bundle: ShadowBundle) : CheckedTreeNode() {
      val importedPackage get() = children?.mapNotNull { it as? MissingImportedPackage } ?: emptyList()
      val requiredBundle get() = children?.mapNotNull { it as? MissingRequiredBundle } ?: emptyList()

      override fun toString(): String = bundle.bundle.canonicalName
      override fun isEnabled(): Boolean = importedPackage.any { it.isEnabled } || requiredBundle.any { it.isEnabled }
    }

    private inner class MissingImportedPackage(val value: String) : CheckedTreeNode() {
      val fixBundle get() = children?.map { it as FixBundle } ?: emptyList()

      override fun toString(): String = "Missing Constraint: $IMPORT_PACKAGE: $value"
      override fun isEnabled(): Boolean = fixBundle.isNotEmpty()
    }

    private inner class MissingRequiredBundle(val value: String) : CheckedTreeNode() {
      val fixBundle get() = children?.map { it as FixBundle } ?: emptyList()

      override fun toString(): String = "Missing Constraint: $REQUIRE_BUNDLE: $value"
      override fun isEnabled(): Boolean = fixBundle.isNotEmpty()
    }

    private inner class FixBundle(val bundle: ShadowBundle) : CheckedTreeNode() {
      init {
        bundle2FixBundle.computeIfAbsent(bundle) { hashSetOf() } += this
      }

      override fun setChecked(checked: Boolean) {
        super.setChecked(checked)
        bundle2FixBundle[bundle]?.filter { it.isChecked != checked }?.forEach {
          it.isChecked = checked
          treeModel.reload(it)
        }
      }

      override fun toString(): String = "${bundle.location.location.identifier} : ${bundle.bundle.canonicalName}"
    }
  }

  private object ShadowLocationRoot : CheckedTreeNode() {
    val sourceVersions = hashMapOf<String, HashSet<BundleDefinition>>()
    val locations get() = children?.map { it as ShadowLocation } ?: emptyList()

    private fun readResolve(): Any = ShadowLocationRoot

    fun sort() = children?.sortBy { it.toString() }

    fun addLocation(location: TargetLocationDefinition): ShadowLocation = ShadowLocation(location).apply {
      val bundleMap = hashMapOf<String, ShadowBundle>()
      location.bundles.sortedBy { it.canonicalName }.forEach {
        val eclipseSourceBundle = it.manifest?.eclipseSourceBundle
        if (eclipseSourceBundle != null) {
          sourceVersions.computeIfAbsent(eclipseSourceBundle.key) { hashSetOf() } += it
        } else {
          bundleMap[it.canonicalName] = ShadowBundle(this, it).apply {
            isChecked = !location.bundleUnSelected.contains(it.canonicalName)
          }
        }
      }

      val featuredBundle = hashSetOf<BundleDefinition>()
      location.features.sortedBy { it.id }.forEach {
        val feature = ShadowFeature(this, it.id, it.version)
        it.plugins.forEach { (id, version) ->
          val bundle = bundleMap["$id-$version"]
          if (bundle != null) {
            feature.add(bundle)
            featuredBundle += bundle.bundle
          }
        }

        add(feature)
      }
      bundleMap.values.filter { it.bundle !in featuredBundle }.takeIf { it.isNotEmpty() }?.also { unFeaturedBundles ->
        val feature = ShadowFeature(this, "Non-featured", Version.emptyVersion)
        unFeaturedBundles.forEach { feature.add(it) }
        add(feature)
      }

      bundles.forEach { bundle ->
        bundle.sourceBundle =
          sourceVersions[bundle.bundle.bundleSymbolicName]?.firstOrNull { it.bundleVersion == bundle.bundle.bundleVersion }
      }
    }.also { ShadowLocationRoot.add(it) }

    fun removeLocation(location: TargetLocationDefinition): ShadowLocation {
      sourceVersions.values.forEach { it -= location.bundles.toSet() }
      return locations.first { it.location == location }.also { ShadowLocationRoot.remove(it) }
    }

    fun replaceLocation(addedLocation: TargetLocationDefinition, removedLocation: TargetLocationDefinition) {
      val oldLocation = removeLocation(removedLocation)
      val newLocation = addLocation(addedLocation)

      val names = addedLocation.bundles.map { it.canonicalName }
      addedLocation.bundleUnSelected += removedLocation.bundleUnSelected.filter { names.contains(it) }

      val oldBundlesMap = oldLocation.bundles.associateBy { it.bundle.canonicalName }
      newLocation.bundles.forEach { bundle ->
        val canonicalName = bundle.bundle.canonicalName

        bundle.isChecked =
          oldBundlesMap[canonicalName]?.isChecked ?: !addedLocation.bundleUnSelected.contains(canonicalName)

        sourceVersions[bundle.bundle.bundleSymbolicName]?.firstOrNull { source ->
          oldBundlesMap[canonicalName]?.sourceBundle?.bundleVersion == source.bundleVersion
        }?.also { bundle.sourceBundle = it }
      }
    }
  }

  private data class ShadowLocation(val location: TargetLocationDefinition) : CheckedTreeNode() {
    val features get() = children?.map { it as ShadowFeature } ?: emptyList()
    val bundles get() = features.flatMap { it.bundles }.distinct()

    val isModify: Boolean
      get() = bundles.any { it.isModify } || location.bundleUnSelected != bundles.filterNot { it.isChecked }
        .map { it.bundle.canonicalName }

    fun reset() {
      bundles.forEach { it.isChecked = !location.bundleUnSelected.contains(it.bundle.canonicalName) }
      bundles.forEach(ShadowBundle::reset)
    }

    fun apply() {
      location.bundleUnSelected.clear()
      location.bundleUnSelected += bundles.filterNot { it.isChecked }.map { it.bundle.canonicalName }

      location.bundleVersionSelection.clear()
      location.bundleVersionSelection += bundles.filterNot { it.sourceBundle == null }
        .associate { it.bundle.canonicalName to it.sourceBundle!!.bundleVersion.toString() }

      bundles.forEach(ShadowBundle::apply)
    }

    override fun toString(): String = location.identifier
  }

  private data class ShadowFeature(val location: ShadowLocation, val id: String, val version: Version) :
    CheckedTreeNode() {
    val bundles get() = children?.map { it as ShadowBundle } ?: emptyList()

    override fun toString(): String = "$id-$version"
  }

  private data class ShadowBundle(val location: ShadowLocation, val bundle: BundleDefinition) : CheckedTreeNode() {
    var sourceBundle: BundleDefinition? = null

    val isModify: Boolean get() = sourceBundle != bundle.sourceBundle

    fun reset() {
      sourceBundle = bundle.sourceBundle
    }

    fun apply() {
      bundle.sourceBundle = sourceBundle
    }

    override fun toString(): String = bundle.canonicalName
  }
}
