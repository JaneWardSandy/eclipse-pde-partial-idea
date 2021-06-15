package cn.varsa.idea.pde.partial.plugin.listener

import com.intellij.openapi.application.*
import com.intellij.openapi.project.*
import com.intellij.util.messages.*
import java.util.*

interface TargetDefinitionChangeListener : EventListener {
    fun locationsChanged(project: Project)

    companion object {
        private val topic = Topic("Eclipse Target Definition Changes", TargetDefinitionChangeListener::class.java)

        fun notifyLocationsChanged(project: Project) =
            ApplicationManager.getApplication().messageBus.syncPublisher(topic).locationsChanged(project)
    }
}
