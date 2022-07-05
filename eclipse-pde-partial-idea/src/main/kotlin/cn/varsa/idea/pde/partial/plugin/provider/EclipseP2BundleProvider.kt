package cn.varsa.idea.pde.partial.plugin.provider

import cn.varsa.idea.pde.partial.common.*
import cn.varsa.idea.pde.partial.common.support.*
import java.io.*
import java.net.*
import java.util.*
import java.util.zip.*
import javax.xml.stream.*

class EclipseP2BundleProvider : EclipseSDKBundleProvider() {
    override val type: String = "Eclipse Oomph"
    override fun resolveDirectory(
        rootDirectory: File, processFeature: (File) -> Unit, processBundle: (File) -> Unit
    ): Boolean {
        val configIni =
            File(rootDirectory, "configuration/config.ini").takeIf { it.exists() && it.isFile }?.inputStream()
                ?.use { Properties().apply { load(it) } } ?: return false

        val p2Area = configIni.getProperty("eclipse.p2.data.area") ?: return false
        val profileName = configIni.getProperty("eclipse.p2.profile") ?: return false

        val p2Directory = try {
            URL(p2Area).toURI().toFile().takeIf { it.exists() } ?: return false
        } catch (e: Exception) {
            return false
        }

        val pluginsDirectory = File(p2Directory, "pool/$Plugins").takeIf { it.exists() } ?: return false
        val featureDirectory = File(p2Directory, "pool/$Features")

        val profileFile = File(
            p2Directory, "org.eclipse.equinox.p2.engine/profileRegistry/$profileName.profile"
        ).takeIf { it.exists() }
            ?.listFiles { file -> file.isFile && file.name.let { it.endsWith(".profile.gz") || it.endsWith(".profile") } }
            ?.maxByOrNull { it.name.substringBefore('.').toLongOrNull() ?: Long.MIN_VALUE } ?: return false

        val processProfileXml: (InputStream) -> Unit = {
            val reader = XMLInputFactory.newInstance().createXMLStreamReader(it)
            try {
                while (reader.hasNext()) {
                    if (reader.next() == XMLStreamConstants.START_ELEMENT && reader.localName == "artifact") {
                        when (reader.getAttributeValue("", "classifier")) {
                            "osgi.bundle" -> {
                                val id = reader.getAttributeValue("", "id") ?: continue
                                val version = reader.getAttributeValue("", "version") ?: continue

                                processBundle(File(pluginsDirectory, "${id}_$version.jar"))
                                processBundle(File(pluginsDirectory, "${id}_$version"))
                            }
                            "org.eclipse.update.feature" -> {
                                val id = reader.getAttributeValue("", "id") ?: continue
                                val version = reader.getAttributeValue("", "version") ?: continue

                                processFeature(File(featureDirectory, "${id}_$version.jar"))
                                processFeature(File(featureDirectory, "${id}_$version"))
                            }
                        }
                    }
                }
            } finally {
                reader.close()
            }
        }
        profileFile.inputStream().use { fileInputStream ->
            if (profileFile.name.endsWith(".profile.gz")) GZIPInputStream(fileInputStream).use(processProfileXml)
            else processProfileXml(fileInputStream)
        }

        super.resolveDirectory(rootDirectory, processFeature, processBundle)
        return true
    }
}
