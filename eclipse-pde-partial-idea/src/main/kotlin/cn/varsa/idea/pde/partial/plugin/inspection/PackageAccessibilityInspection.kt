package cn.varsa.idea.pde.partial.plugin.inspection

import cn.varsa.idea.pde.partial.common.*
import cn.varsa.idea.pde.partial.common.domain.*
import cn.varsa.idea.pde.partial.common.support.*
import cn.varsa.idea.pde.partial.plugin.cache.*
import cn.varsa.idea.pde.partial.plugin.config.*
import cn.varsa.idea.pde.partial.plugin.facet.*
import cn.varsa.idea.pde.partial.plugin.helper.*
import cn.varsa.idea.pde.partial.plugin.i18n.EclipsePDEPartialBundles.message
import cn.varsa.idea.pde.partial.plugin.support.*
import com.intellij.codeInspection.*
import com.intellij.codeInspection.util.*
import com.intellij.ide.projectView.impl.*
import com.intellij.openapi.module.*
import com.intellij.openapi.project.*
import com.intellij.openapi.roots.*
import com.intellij.openapi.roots.libraries.*
import com.intellij.packageDependencies.*
import com.intellij.psi.*
import org.osgi.framework.*
import org.osgi.framework.Constants.*

abstract class PackageAccessibilityInspection : AbstractBaseJavaLocalInspectionTool() {

  override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
    if (file !is PsiClassOwner || ProjectRootsUtil.isInTestSource(file)) return null

    val projectFileIndex = ProjectFileIndex.getInstance(file.project)
    if (file.virtualFile?.let(projectFileIndex::isInLibrary) == true) return null

    val facet = file.module?.let { PDEFacet.getInstance(it) } ?: return null

    val problems = hashSetOf<ProblemDescriptor>()
    val addMessage: (Problem, PsiElement) -> Unit = { problem, place ->
      problems.add(
        manager.createProblemDescriptor(place, problem.message, isOnTheFly, problem.fixes, problem.type)
      )
    }

    DependenciesBuilder.analyzeFileDependencies(file, { place, dependency ->
      checkElement(place, dependency, facet, addMessage)
    }, DependencyVisitorFactory.VisitorOptions.SKIP_IMPORTS)

    return problems.takeIf { it.isNotEmpty() }?.toTypedArray()
  }

  abstract fun checkElement(
    place: PsiElement, dependency: PsiElement, facet: PDEFacet, occurProblem: (Problem, PsiElement) -> Unit
  )

  companion object {
    fun checkAccessibility(
      item: PsiFileSystemItem, packageName: String, qualifiedName: String, requesterModule: Module
    ): List<Problem> {
      val project = requesterModule.project
      val cacheService = BundleManifestCacheService.getInstance(project)
      val managementService = BundleManagementService.getInstance(project)
      val index = ProjectFileIndex.getInstance(project)

      val importer = cacheService.getManifest(requesterModule) ?: return emptyList()
      var hostImporter: BundleManifest? = null
      val ownerFile = item.virtualFile?.let(index::getClassRootForFile)

      // In bundleClassPath
      val library = requesterModule.findLibrary { it.name == ModuleLibraryName }
      if (library?.let { LibraryUtil.isClassAvailableInLibrary(it, qualifiedName) } == true) return emptyList()


      // In Java JDK?
      item.virtualFile?.isBelongJDK(index)?.ifTrue { return emptyList() }

      // In Fragment Host?
      importer.fragmentHostAndVersionRange()?.also { (fragmentHostBSN, fragmentHostVersion) ->
        hostImporter = project.allPDEModules(requesterModule).mapNotNull { cacheService.getManifest(it) }
          .firstOrNull { it.isFragmentHost(fragmentHostBSN, fragmentHostVersion) }
          ?: BundleManagementService.getInstance(project)
            .getBundlesByBSN(fragmentHostBSN, fragmentHostVersion)?.manifest

        ownerFile?.presentableUrl?.let { managementService.getBundleByInnerJarPath(it)?.manifest }
          ?.isFragmentHost(fragmentHostBSN, fragmentHostVersion)?.ifTrue { return emptyList() }

        project.allPDEModules(requesterModule).any { module ->
          cacheService.getManifest(module)?.let { manifest ->
            manifest.isFragmentHost(
              fragmentHostBSN, fragmentHostVersion
            ) && (ownerFile?.presentableUrl == module.getModuleDir() || manifest.bundleClassPath?.keys?.filterNot { it == "." }
              ?.mapNotNull { module.getModuleDir().toFile(it).canonicalPath }
              ?.any { ownerFile?.presentableUrl == it } == true)
          } == true
        }.ifTrue { return emptyList() }

        cacheService.getManifest(item)?.isFragmentHost(fragmentHostBSN, fragmentHostVersion)
          ?.ifTrue { return emptyList() }
      }

      // In bundle class path?
      val containers = arrayListOf<BundleManifest>()
      ownerFile?.presentableUrl?.let { managementService.getBundleByInnerJarPath(it)?.manifest }
        ?.also { containers += it }
      containers += project.allPDEModules(requesterModule).filter { module ->
        ownerFile?.presentableUrl == module.getModuleDir() || cacheService.getManifest(module)?.bundleClassPath?.keys?.filterNot { it == "." }
          ?.mapNotNull { module.getModuleDir().toFile(it).canonicalPath }
          ?.any { ownerFile?.presentableUrl == it } == true
      }.mapNotNull { cacheService.getManifest(it) }
      if (containers.isEmpty()) cacheService.getManifest(item)?.also { containers += it }

      val problems = arrayListOf<Problem>()
      if (containers.isEmpty()) problems += Problem.weak(message("inspection.hint.nonBundle", packageName))
      containers.forEach { exporter ->
        val exporterSymbolic = exporter.bundleSymbolicName
        if (exporterSymbolic == null) {
          problems += Problem.weak(message("inspection.hint.nonBundle", packageName))
          return@forEach
        }

        val exporterHost = exporter.fragmentHostAndVersionRange()?.let { (fragmentHostBSN, fragmentHostVersion) ->
          project.allPDEModules(requesterModule).mapNotNull { cacheService.getManifest(it) }
            .firstOrNull { it.isFragmentHost(fragmentHostBSN, fragmentHostVersion) }
            ?: BundleManagementService.getInstance(project)
              .getBundlesByBSN(fragmentHostBSN, fragmentHostVersion)?.manifest
        }

        val exporterBSN = exporterSymbolic.key
        val exporterVersions = hashSetOf(exporter.bundleVersion)

        val exporterHostBSN = exporterHost?.bundleSymbolicName?.key
        val exporterHostVersions = hashSetOf<Version>().apply {
          exporterHost?.bundleVersion?.let { this += it }
        }

        val exportedPackageVersions = hashSetOf<Version>()

        exporter.exportedPackageAndVersion()[packageName]?.also { exportedPackageVersions += it }
        exporterHost?.exportedPackageAndVersion()?.get(packageName)?.also { exportedPackageVersions += it }

        managementService.getBundlesByBSN(exporterBSN)?.mapValues { it.value.manifest }
          ?.mapValues { it.value?.exportedPackageAndVersion()?.get(packageName) }?.also {
            exporterVersions += it.keys
            exportedPackageVersions += it.values.filterNotNull()
          }
        exporterHostBSN?.let { managementService.getBundlesByBSN(it) }?.mapValues { it.value.manifest }
          ?.mapValues { it.value?.exportedPackageAndVersion()?.get(packageName) }?.also {
            exporterHostVersions += it.keys
            exportedPackageVersions += it.values.filterNotNull()
          }

        if (exportedPackageVersions.isEmpty()) {
          problems += Problem.warning(message("inspection.hint.packageNoExport",
                                              packageName,
                                              exporterHostBSN?.let { "Fragment: $exporterBSN(Host: $exporterHostBSN)" }
                                              ?: exporterBSN))
          return@forEach
        }


        val bsn = exporterHostBSN ?: exporterBSN
        val versions = exporterHostVersions.takeIf { it.isNotEmpty() } ?: exporterVersions

        importer.isPackageImported(packageName, exportedPackageVersions).ifTrue { return emptyList() }
        importer.isBundleRequired(bsn, versions).ifTrue { return emptyList() }
        requesterModule.isBundleRequiredOrFromReExport(bsn, versions).ifTrue { return emptyList() }


        hostImporter?.isPackageImported(packageName, exportedPackageVersions)?.ifTrue { return emptyList() }
        hostImporter?.isBundleRequired(bsn, versions)?.ifTrue { return emptyList() }
        hostImporter?.isBundleRequiredOrFromReExport(project, requesterModule, bsn, versions)
          ?.ifTrue { return emptyList() }


        val fixes = mutableListOf<AccessibilityFix>()
        fixes += versions.map { it.toString() }.map { ver -> AccessibilityFix.requireBundleFix(bsn, ver) }
        fixes += AccessibilityFix.requireBundleFix(bsn)

        problems += Problem.warning(message("inspection.hint.packageAccessibility",
                                            packageName,
                                            exporterHostBSN?.let { "Fragment: $exporterBSN(Host: $exporterHostBSN)" }
                                            ?: exporterBSN),
                                    AccessibilityFix.importPackageFix(packageName),
                                    *fixes.toTypedArray())
      }

      return problems
    }
  }
}

class Problem(
  val type: ProblemHighlightType, val message: @InspectionMessage String, vararg val fixes: LocalQuickFix
) {
  companion object {
    fun weak(message: @InspectionMessage String, vararg fixes: LocalQuickFix): Problem =
      Problem(ProblemHighlightType.WEAK_WARNING, message, *fixes)

    fun warning(message: @InspectionMessage String, vararg fixes: LocalQuickFix): Problem =
      Problem(ProblemHighlightType.WARNING, message, *fixes)
  }
}

class AccessibilityFix private constructor(
  private val displayName: String, private val headerName: String, private val headerValue: String
) : AbstractOsgiQuickFix() {
  override fun getName(): String = displayName
  override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
    getVerifiedManifestFile(descriptor.psiElement)?.also {
      writeCompute { PsiHelper.appendToHeader(it, headerName, headerValue) }
    }
  }

  companion object Factory {
    fun importPackageFix(importPackage: String): AccessibilityFix = AccessibilityFix(
      message("inspection.fix.addPackageToImport", importPackage), IMPORT_PACKAGE, importPackage
    )

    fun requireBundleFix(requireBundle: String, version: String? = null): AccessibilityFix {
      val headerValue =
        "$requireBundle${if (version.isNullOrBlank()) "" else ";$BUNDLE_VERSION_ATTRIBUTE=\"$version\""}"

      return AccessibilityFix(
        message("inspection.fix.addBundleToRequired", headerValue), REQUIRE_BUNDLE, headerValue
      )
    }
  }
}
