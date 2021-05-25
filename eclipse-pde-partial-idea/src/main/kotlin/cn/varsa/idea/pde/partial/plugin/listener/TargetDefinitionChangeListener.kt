package cn.varsa.idea.pde.partial.plugin.listener

import cn.varsa.idea.pde.partial.plugin.config.*
import com.intellij.openapi.application.*
import com.intellij.openapi.project.*
import com.intellij.util.messages.*

interface TargetDefinitionChangeListener {
    fun locationsChanged(project: Project, locations: List<TargetLocationDefinition>)

    companion object {
        private val topic = Topic("Eclipse Target Definition Changes", TargetDefinitionChangeListener::class.java)

        fun notifyLocationsChanged(project: Project, locations: List<TargetLocationDefinition>) =
            ApplicationManager.getApplication().messageBus.syncPublisher(topic).locationsChanged(project, locations)
    }
}
