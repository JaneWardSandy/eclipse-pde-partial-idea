package cn.varsa.idea.pde.partial.core.manifest

import cn.varsa.idea.pde.partial.common.Constants
import cn.varsa.idea.pde.partial.common.manifest.BundleManifest
import cn.varsa.idea.pde.partial.message.ToolWindowBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.*
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.ui.*
import com.intellij.ui.components.*
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.speedSearch.SpeedSearchUtil
import com.intellij.util.indexing.FileBasedIndex
import org.jetbrains.annotations.Nls
import java.awt.event.MouseEvent
import javax.swing.*

class BundlesToolWindowFactory : ToolWindowFactory {

  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    val toolWindowContent = BundlesToolWindowContent(project)
    val content = ContentFactory.getInstance().createContent(toolWindowContent.content, "", false)

    toolWindow.contentManager.addContent(content)
    toolWindow.setTitleActions(listOf(object : AnAction(
      ToolWindowBundle.message("toolwindow.actions.refreshTitle"),
      ToolWindowBundle.message("toolwindow.actions.refreshDescription"),
      AllIcons.Actions.Refresh
    ) {
      override fun actionPerformed(e: AnActionEvent) = toolWindowContent.buildList()
    }, object : AnAction(
      ToolWindowBundle.message("toolwindow.actions.reIndexTitle"),
      ToolWindowBundle.message("toolwindow.actions.reIndexDescription"),
      AllIcons.Actions.ForceRefresh
    ) {
      override fun actionPerformed(e: AnActionEvent) {
        BundleManifestIndex.requireReIndexes()
        toolWindowContent.buildList()
      }
    }))
  }

  private class BundlesToolWindowContent(private val project: Project) {
    private val dumbService = DumbService.getInstance(project)
    private val fileIndex = ProjectFileIndex.getInstance(project)

    private val model = CollectionListModel<Pair<VirtualFile, BundleManifest>>()
    private val list = JBList(model)
    val content = DumbUnawareHider(JBScrollPane(list))

    init {
      list.selectionMode = ListSelectionModel.SINGLE_SELECTION
      list.cellRenderer = object : ColoredListCellRenderer<Pair<VirtualFile, BundleManifest>>() {
        override fun customizeCellRenderer(
          list: JList<out Pair<VirtualFile, BundleManifest>>,
          value: Pair<VirtualFile, BundleManifest>?,
          index: Int,
          selected: Boolean,
          hasFocus: Boolean
        ) {
          val (file, manifest) = value ?: return

          toolTipText = file.path

          icon = if (fileIndex.getModuleForFile(file) != null) AllIcons.Nodes.Module else AllIcons.Nodes.Plugin

          manifest.bundleSymbolicName?.key?.also<@Nls String>(::append)
          manifest.attribute[Constants.OSGI.Header.BUNDLE_VERSION]?.also {
            append(":")
            append(it)
          }

          SpeedSearchUtil.applySpeedSearchHighlighting(list, this, false, selected)
        }
      }
      object : DoubleClickListener() {
        override fun onDoubleClick(event: MouseEvent): Boolean {
          val selection = !list.isSelectionEmpty
          if (selection) {
            val file = list.selectedValue.first
            FileEditorManager.getInstance(project).openFile(file)
          }
          return selection
        }
      }.installOn(list)

      TreeUIHelper.getInstance().installListSpeedSearch(list) { (_, manifest) ->
        "${manifest.bundleSymbolicName?.key}:${manifest.attribute[Constants.OSGI.Header.BUNDLE_VERSION]}"
      }

      buildList()
    }

    fun buildList() {
      dumbService.smartInvokeLater {
        model.removeAll()

        val index = FileBasedIndex.getInstance()
        index.processAllKeys(BundleManifestIndex.NAME, { key ->
          index.processValues(BundleManifestIndex.NAME, key, null, { file, manifest ->
            model.add(file to manifest)
            true
          }, GlobalSearchScope.allScope(project))
          true
        }, project)

        model.sort { o1, o2 ->
          val (f1, m1) = o1
          val (f2, m2) = o2

          val mn1 = fileIndex.getModuleForFile(f1)?.name
          val mn2 = fileIndex.getModuleForFile(f2)?.name

          var diff = when {
            mn1 == mn2 -> 0
            mn1 == null -> 1
            mn2 == null -> -1
            else -> 0
          }
          if (diff == 0) diff = compareValues(mn1, mn2)
          if (diff == 0) diff = compareValues(m1.bundleSymbolicName?.key, m2.bundleSymbolicName?.key)

          diff
        }
      }
    }
  }
}