package cn.varsa.idea.pde.partial.plugin.dom

import org.jdom.*
import org.jdom.input.sax.*
import org.xml.sax.*

class NameSpaceCleanerFactory : SAXHandlerFactory {
    override fun createSAXHandler(factory: JDOMFactory?): SAXHandler = NoNameSpaceHandler(factory)

    class NoNameSpaceHandler(factory: JDOMFactory?) : SAXHandler(factory) {
        override fun startElement(namespaceURI: String?, localName: String?, qName: String?, atts: Attributes?) {
            super.startElement("", localName, qName, atts)
        }

        override fun startPrefixMapping(prefix: String?, uri: String?) {}
    }
}
