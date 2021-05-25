package cn.varsa.idea.pde.partial.plugin.support

import com.intellij.openapi.module.*
import com.intellij.openapi.project.*
import org.jetbrains.kotlin.idea.util.projectStructure.allModules as allModulesIdea
import org.jetbrains.kotlin.idea.util.projectStructure.getModuleDir as getModuleDirIdea

// HACK for IDEA 2021.1.1 BUG Cannot access class
fun Project.allModules(): List<Module> = allModulesIdea()
fun Module.getModuleDir(): String = getModuleDirIdea()
