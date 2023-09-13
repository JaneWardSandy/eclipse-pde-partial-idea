package cn.varsa.idea.pde.partial.inspection

import cn.varsa.idea.pde.partial.common.Constants.Eclipse.X_FRIENDS_DIRECTIVE
import cn.varsa.idea.pde.partial.common.Constants.Eclipse.X_INTERNAL_DIRECTIVE
import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.BUNDLE_SYMBOLICNAME_ATTRIBUTE
import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.BUNDLE_VERSION_ATTRIBUTE
import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.USES_DIRECTIVE
import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.VERSION_ATTRIBUTE
import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.VISIBILITY_DIRECTIVE
import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.VISIBILITY_REEXPORT
import cn.varsa.idea.pde.partial.common.Constants.Partial.File.MANIFEST_PATH
import cn.varsa.idea.pde.partial.common.Constants.Partial.JAVA
import cn.varsa.idea.pde.partial.common.Constants.Partial.KOTLIN
import cn.varsa.idea.pde.partial.common.extension.*
import cn.varsa.idea.pde.partial.common.manifest.BundleManifest
import cn.varsa.idea.pde.partial.common.version.VersionRange
import cn.varsa.idea.pde.partial.core.manifest.BundleManifestIndex
import cn.varsa.idea.pde.partial.message.InspectionBundle
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.roots.*
import com.intellij.openapi.util.Ref
import com.intellij.psi.*
import com.intellij.util.containers.CollectionFactory
import org.jetbrains.kotlin.idea.base.util.module

interface PackageAccessibilityInspector : BasicInspector {

  fun checkManifest(element: PsiElement, clazz: PsiClass, holder: ProblemsHolder) {
    val file = clazz.containingFile
    if (file is PsiClassOwner) {
      checkManifest(element, file, holder)
    }
  }

  fun checkManifest(element: PsiElement, classOwner: PsiClassOwner, holder: ProblemsHolder) {
    val project = classOwner.project
    val fileIndex = ProjectFileIndex.getInstance(project)

    val module = element.module ?: return
    val moduleManifestRef = Ref.create<BundleManifest?>()
    BundleManifestIndex.processAllManifests(module.getModuleScope(false)) { _, manifest ->
      moduleManifestRef.set(manifest)
      moduleManifestRef.isNull
    }

    val importerFragment = moduleManifestRef.get() ?: return
    val importerHost = importerFragment.fragmentHost?.let { (fragmentHost, attributes) ->
      val range = attributes.attribute[BUNDLE_VERSION_ATTRIBUTE].parseVersionRange()
      val manifests = BundleManifestIndex.getManifestBySymbolicName(fragmentHost, project).values
      manifests.sortedByDescending { it.bundleVersion }.firstOrNull { it.bundleVersion in range }
    }
    val importer = importerHost ?: importerFragment
    val symbolicName = importer.bundleSymbolicName?.key ?: return


    val isBelongJDK = fileIndex.getOrderEntriesForFile(classOwner.virtualFile).any { it is JdkOrderEntry }
    if (isBelongJDK) return

    val packageName = classOwner.packageName
    if (packageName.isBlank() || packageName.startsWith(JAVA) || packageName.startsWith(KOTLIN)) return

    val rootForFile = fileIndex.getClassRootForFile(classOwner.virtualFile) ?: return
    val manifestFile = rootForFile.findFileByRelativePath(MANIFEST_PATH)
    if (manifestFile == null) {
      holder.problem(element, InspectionBundle.message("inspection.hint.nonBundle", packageName)).register()
      return
    }


    val exporterFragment = BundleManifestIndex.getManifestByFile(project, manifestFile) ?: return
    val exporterHost = exporterFragment.value.fragmentHost?.let { (fragmentHost, attributes) ->
      val range = attributes.attribute[BUNDLE_VERSION_ATTRIBUTE].parseVersionRange()
      val manifests = BundleManifestIndex.getManifestBySymbolicName(fragmentHost, project).values
      manifests.sortedByDescending { it.bundleVersion }.firstOrNull { it.bundleVersion in range }
    }
    val exporter = exporterHost ?: exporterFragment.value
    val exporterName = exporter.bundleSymbolicName?.key
    if (exporterName == null) {
      holder.problem(element, InspectionBundle.message("inspection.hint.nonBundle", packageName)).register()
      return
    }


    val exportPackage =
      exporter.exportPackage?.attributes?.entries?.firstOrNull { it.key.removeSuffix(".*") == packageName }?.value
    if (exportPackage == null) {
      holder
        .problem(element, InspectionBundle.message("inspection.hint.packageNonExport", packageName, exporterName))
        .register()
      return
    }

    val uses = exportPackage.directive[USES_DIRECTIVE]
      ?.replace("\\s", "")
      ?.unquote()
      ?.split(',')
      ?.map { it.removeSuffix(".*").trim() }
    if (uses != null && packageName !in uses) {
      holder
        .problem(element, InspectionBundle.message("inspection.hint.packageNonUses", packageName, exporterName))
        .register()
      return
    }
    val internal = exportPackage.directive[X_INTERNAL_DIRECTIVE] == "true"
    if (internal) {
      holder
        .problem(element, InspectionBundle.message("inspection.hint.packageInternal", packageName, exporterName))
        .register()
      return
    }
    val friends =
      exportPackage.directive[X_FRIENDS_DIRECTIVE]?.replace("\\s", "")?.unquote()?.split(',')?.map { it.trim() }
    if (friends != null && symbolicName !in friends) {
      holder.problem(
        element, InspectionBundle.message("inspection.hint.packageFriends", packageName, exporterName, symbolicName)
      ).register()
      return
    }


    val exportVersion = exportPackage.attribute[VERSION_ATTRIBUTE]?.replace("\\s", "")?.unquote()?.parseVersion()
    val importPackage =
      importer.importPackage?.attributes?.entries?.firstOrNull { it.key.removeSuffix(".*") == packageName }?.value
    if (importPackage != null) {
      val importSymbolicName = importPackage.attribute[BUNDLE_SYMBOLICNAME_ATTRIBUTE]?.replace("\\s", "")
      if (importSymbolicName != null && importSymbolicName != exporterName) {
        holder
          .problem(element, InspectionBundle.message("inspection.hint.bundleNotImport", packageName, exporterName))
          .register()
        return
      }

      val range = importPackage.attribute[VERSION_ATTRIBUTE].parseVersionRange()
      if (exportVersion != null && exportVersion !in range) {
        holder.problem(
          element,
          InspectionBundle.message("inspection.hint.packageNotInRange", packageName, exporterName, exportVersion, range)
        ).register()
        return
      }
    }


    val requireBundle = importer.requireBundle?.attributes
    if (requireBundle != null) {
      if (exporterName in requireBundle) return

      val checked = CollectionFactory.createSmallMemoryFootprintSet<String>()
      val next = mutableListOf<Pair<String, VersionRange>>()

      next += requireBundle.map { it.key to it.value.attribute[BUNDLE_VERSION_ATTRIBUTE].parseVersionRange() }

      while (next.isNotEmpty()) {
        val (name, range) = next.removeFirst()
        if (!checked.add(name)) continue

        val manifest = BundleManifestIndex.getManifestBySymbolicName(name, project).values
          .sortedByDescending { it.bundleVersion }
          .firstOrNull { it.bundleVersion in range } ?: continue

        val visibilityRequires =
          manifest.requireBundle?.attributes?.filterValues { it.directive[VISIBILITY_DIRECTIVE] == VISIBILITY_REEXPORT }
            ?: continue

        if (exporterName in visibilityRequires) return

        val elements =
          visibilityRequires.map { it.key to it.value.attribute[BUNDLE_VERSION_ATTRIBUTE].parseVersionRange() }
        next.addAll(0, elements)
      }
    }

    holder.problem(element, InspectionBundle.message("inspection.hint.packageAccessibility", packageName)).register()
    holder.problem(element, InspectionBundle.message("inspection.hint.bundleNotRequired", exporterName)).register()
  }
}