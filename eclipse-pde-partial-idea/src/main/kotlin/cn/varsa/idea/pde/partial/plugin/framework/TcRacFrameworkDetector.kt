package cn.varsa.idea.pde.partial.plugin.framework

import cn.varsa.idea.pde.partial.common.*
import cn.varsa.idea.pde.partial.plugin.facet.*
import com.intellij.facet.*
import com.intellij.framework.detection.*
import com.intellij.openapi.fileTypes.*
import com.intellij.patterns.*
import com.intellij.util.*
import com.intellij.util.indexing.*
import org.jetbrains.lang.manifest.*
import org.osgi.framework.*

class TcRacFrameworkDetector : FacetBasedFrameworkDetector<PDEFacet, PDEFacetConfiguration>("Eclipse PDE") {

    override fun getFileType(): FileType = ManifestFileType.INSTANCE
    override fun getFacetType(): FacetType<PDEFacet, PDEFacetConfiguration> = PDEFacetType.getInstance()

    override fun createSuitableFilePattern(): ElementPattern<FileContent> =
        FileContentPattern.fileContent().inDirectory(MetaInf).withName(ManifestMf)
            .with(object : PatternCondition<FileContent>("OSGI manifest file") {
                override fun accepts(t: FileContent, context: ProcessingContext?): Boolean =
                    t.contentAsText.splitToSequence('\r', '\n').any { it.startsWith(Constants.BUNDLE_SYMBOLICNAME) }
            })
}
