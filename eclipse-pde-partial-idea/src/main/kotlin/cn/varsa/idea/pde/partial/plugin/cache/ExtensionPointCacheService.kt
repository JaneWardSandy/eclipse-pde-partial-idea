package cn.varsa.idea.pde.partial.plugin.cache

import com.intellij.openapi.project.*
import com.intellij.psi.util.*
import com.jetbrains.rd.util.*

class ExtensionPointCacheService(private val project: Project) {
    private val caches = ConcurrentHashMap<String, CachedValue<String>>()

    companion object {
        fun getInstance(project: Project): ExtensionPointCacheService =
            project.getService(ExtensionPointCacheService::class.java)
    }

    fun clearCache() {
        caches.clear()
    }

    // TODO: 2021/6/23
}
