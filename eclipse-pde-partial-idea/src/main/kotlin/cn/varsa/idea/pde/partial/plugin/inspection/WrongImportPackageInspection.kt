package cn.varsa.idea.pde.partial.plugin.inspection

import cn.varsa.idea.pde.partial.plugin.cache.*
import cn.varsa.idea.pde.partial.plugin.facet.*
import cn.varsa.idea.pde.partial.plugin.helper.*
import cn.varsa.idea.pde.partial.plugin.i18n.EclipsePDEPartialBundles.message
import cn.varsa.idea.pde.partial.plugin.manifest.psi.*
import cn.varsa.idea.pde.partial.plugin.support.*
import com.intellij.codeInspection.*
import com.intellij.psi.*
import org.jetbrains.lang.manifest.psi.*
import org.osgi.framework.*

class WrongImportPackageInspection : AbstractOsgiVisitor() {
    override fun buildVisitor(facet: PDEFacet, holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (PsiHelper.isHeader(element, Constants.IMPORT_PACKAGE)) {
                    val cacheService = BundleManifestCacheService.getInstance(facet.module.project)

                    nextValue@ for (value in (element as Header).headerValues) {
                        if (value is Clause) {
                            val valuePart = value.getValue()
                            if (valuePart != null) {
                                val packageName = valuePart.unwrappedText.substringBeforeLast(".*")
                                if (packageName.isBlank()) continue

                                val directories = PsiHelper.resolvePackage(element, packageName)
                                if (directories.isEmpty()) continue

                                for (directory in directories) {
                                    val manifest = cacheService.getManifest(directory)
                                    if (manifest?.getExportedPackage(packageName) != null) {
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
