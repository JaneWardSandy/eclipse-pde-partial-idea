package cn.varsa.idea.pde.partial.plugin.inspection

import cn.varsa.idea.pde.partial.plugin.cache.*
import cn.varsa.idea.pde.partial.plugin.config.*
import cn.varsa.idea.pde.partial.plugin.facet.*
import cn.varsa.idea.pde.partial.plugin.helper.*
import cn.varsa.idea.pde.partial.plugin.i18n.EclipsePDEPartialBundles.message
import cn.varsa.idea.pde.partial.plugin.manifest.psi.*
import cn.varsa.idea.pde.partial.plugin.support.*
import com.intellij.codeInspection.*
import com.intellij.openapi.roots.*
import com.intellij.psi.*
import org.jetbrains.lang.manifest.psi.*
import org.osgi.framework.*

class WrongImportPackageInspection : AbstractOsgiVisitor() {
    override fun buildVisitor(facet: PDEFacet, holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (PsiHelper.isHeader(element, Constants.IMPORT_PACKAGE)) {
                    val project = facet.module.project

                    val cacheService = BundleManifestCacheService.getInstance(project)
                    val managementService = BundleManagementService.getInstance(project)
                    val index = ProjectFileIndex.getInstance(project)

                    nextValue@ for (value in (element as Header).headerValues) {
                        if (value is Clause) {
                            val valuePart = value.getValue()
                            if (valuePart != null) {
                                val packageName = valuePart.unwrappedText.substringBeforeLast(".*")
                                if (packageName.isBlank()) continue

                                if (project.allPDEModules().filterNot { facet.module == it }
                                        .mapNotNull { cacheService.getManifest(it) }
                                        .any { it.getExportedPackage(packageName) != null }) {
                                    continue@nextValue
                                }

                                val directories = PsiHelper.resolvePackage(element, packageName)
                                if (directories.isEmpty()) continue

                                for (directory in directories) {
                                    val jarFile = index.getClassRootForFile(directory.virtualFile)
                                    val containerBundle = managementService.jarPathInnerBundle[jarFile?.presentableUrl]

                                    if (containerBundle?.manifest?.getExportedPackage(packageName) != null) {
                                        continue@nextValue
                                    }
                                }

                                val range = valuePart.highlightingRange.shiftRight(-valuePart.textOffset)
                                holder.registerProblem(valuePart, range, message("inspection.hint.wrongImportPackage"))
                            }
                        }
                    }
                }
            }
        }
}
