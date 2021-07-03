package cn.varsa.idea.pde.partial.plugin.dom.inspection

import cn.varsa.idea.pde.partial.plugin.dom.plugin.*
import cn.varsa.idea.pde.partial.plugin.i18n.EclipsePDEPartialBundles.message
import com.intellij.lang.annotation.*
import com.intellij.util.xml.*
import com.intellij.util.xml.highlighting.*

class ElementOccurLimitAnnotator : DomElementsAnnotator {
    override fun annotate(element: DomElement, holder: DomElementAnnotationHolder) {
        if (element !is ExtensionElement) return
        val limits = element.getUserData(ExtensionElement.occursLimitKey) ?: return

        val childrenCounts =
            DomUtil.getChildrenOfType(element, ExtensionElement::class.java).groupBy { it.xmlElementName }
                .mapValues { it.value.size }

        limits.forEach {
            val tagName = it.tagName
            val count = childrenCounts[tagName] ?: 0

            if (it.minOccurs > 0 && count < it.minOccurs) {
                holder.createProblem(
                    element,
                    HighlightSeverity.ERROR,
                    message("inspection.dom.plugin.minSubTag", count, tagName, it.minOccurs)
                )
            }
            if (it.maxOccurs > ExtensionElement.OccursLimit.unbounded && count > it.maxOccurs) {
                holder.createProblem(
                    element,
                    HighlightSeverity.ERROR,
                    message("inspection.dom.plugin.maxSubTag", count, tagName, it.maxOccurs)
                )
            }
        }
    }
}
