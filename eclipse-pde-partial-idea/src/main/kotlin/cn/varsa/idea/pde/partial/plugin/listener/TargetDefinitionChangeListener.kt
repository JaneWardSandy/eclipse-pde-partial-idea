package cn.varsa.idea.pde.partial.plugin.listener

import cn.varsa.idea.pde.partial.plugin.config.*
import com.intellij.openapi.application.*
import com.intellij.openapi.project.*
import com.intellij.util.messages.*

interface TargetDefinitionChangeListener {
    fun locationsChanged(project: Project, changes: Set<Pair<TargetLocationDefinition?, TargetLocationDefinition?>>) {}

    companion object {
        private val topic = Topic("Eclipse Target Definition Changes", TargetDefinitionChangeListener::class.java)

        fun notifyLocationsChanged(
            project: Project,
            changes: Set<Pair<TargetLocationDefinition?, TargetLocationDefinition?>>,
        ) = ApplicationManager.getApplication().messageBus.syncPublisher(topic).locationsChanged(project, changes)
    }
}
