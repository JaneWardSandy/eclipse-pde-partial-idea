package cn.varsa.idea.pde.partial.common.domain

import java.io.*
import java.nio.charset.*


class JavaCommandParameters : Serializable {
    val vmParameters = ParametersList()
    val programParameters = ParametersList()
    val classPath = PathsList()
    val env = mutableMapOf<String, String>()
    var passParentEnvs = true

    var mainClass: String = ""
    var workingDirectory: String = ""
    var charset: String = Charset.defaultCharset().name()
}

class ParametersList : Serializable {
    private val parameters = mutableListOf<String>()
    val list: List<String>
        get() = parameters

    fun add(parameter: String) {
        parameters += parameter
    }

    fun addAll(vararg parameters: String) {
        this.parameters += parameters
    }

    fun addAll(parameters: Collection<String>) {
        this.parameters += parameters
    }

    fun getPropertyValue(propertyName: String): String? =
        parameters.indexOfLast { it == "-D$propertyName" || it == "-D$propertyName=" }.takeIf { it > -1 }
            ?.let { parameters[it] }?.substringAfter("-D$propertyName=")
}

class PathsList : Serializable {
    private val paths = mutableListOf<String>()
    private val pathsTail = mutableListOf<String>()
    private val pathsSet = mutableSetOf<String>()

    val isEmpty: Boolean
        get() = pathsSet.isEmpty()

    val pathList: List<String>
        get() = paths + pathsTail

    fun add(path: String) = addAllLast(chooseFirstTimeItems(path), paths)
    fun addTail(path: String) = addAllLast(chooseFirstTimeItems(path), pathsTail)

    private fun chooseFirstTimeItems(path: String): Iterable<String> =
        path.split(File.pathSeparator).map(String::trim).filter(String::isNotBlank).filterNot(pathsSet::contains)

    private fun addAllLast(elements: Iterable<String>, target: MutableList<String>) {
        target += elements
        pathsSet += elements
    }
}
