package cn.varsa.idea.pde.partial.launcher.command

import cn.varsa.idea.pde.partial.common.domain.*
import cn.varsa.idea.pde.partial.launcher.support.*
import java.io.*
import java.nio.charset.*
import java.util.regex.*

class JavaCommandLineSetup {
  val commandLine = CommandLine()

  fun setupJavaExePath(javaPath: String) {
    commandLine.exePath = javaPath
  }

  fun setupCommandLine(parameters: JavaCommandParameters) {
    commandLine.environment.clear()
    commandLine.parameters.clear()

    commandLine.workingDirectory = parameters.workingDirectory
    commandLine.environment += parameters.env
    commandLine.parameters += parameters.vmParameters.list
    commandLine.passParentEnvs = parameters.passParentEnvs

    val encoding = parameters.vmParameters.getPropertyValue("file.encoding")
    if (encoding == null) {
      commandLine.parameters += "-Dfile.encoding=${parameters.charset}"
      commandLine.charset = Charset.forName(parameters.charset)
    } else {
      try {
        commandLine.charset = Charset.forName(encoding)
      } catch (_: Exception) {
      }
    }

    if (parameters.classPath.isEmpty.not()) {
      commandLine.parameters += "-classpath"
      commandLine.parameters += parameters.classPath.pathList.joinToString(File.pathSeparator)
    }

    commandLine.parameters += parameters.mainClass
    commandLine.parameters += parameters.programParameters.list
  }
}

class CommandLine {
  private val inescapableQuote = '\uEFEF'

  private val winBackslashesPrecedingQuote = Pattern.compile("(\\\\+)(?=\"|$)")
  private val winQuoteSpecial = Pattern.compile("[ \t\"*?\\[{}~()']") // + glob [*?] + Cygwin glob [*?\[{}~] + [()']

  private val quote = '\"'
  private val quoteQuote = "\"\""

  var exePath = ""
  var workingDirectory = ""
  var charset: Charset = Charset.defaultCharset()
  var passParentEnvs = true
  val environment = mutableMapOf<String, String>()
  val parameters = mutableListOf<String>()

  private val workingDir: File
    get() = File(workingDirectory)

  fun createProcess(): Process = ProcessBuilder(validateAndPrepareCommandLine()).apply {
    setupEnvironment(environment())
    directory(workingDir)
    redirectError()
  }.start()

  private fun validateAndPrepareCommandLine(): List<String> {
    if (workingDir.exists().not()) {
      throw FileNotFoundException("Cannot start process, the working directory '$workingDirectory' does not exist")
    }
    if (workingDir.isDirectory.not()) {
      throw IOException("Cannot start process, the working directory '$workingDirectory' is not a directory")
    }
    if (exePath.isBlank()) {
      throw IllegalStateException("Executable is not specified")
    }

    environment.forEach { (name, value) ->
      if (!isValidName(name)) throw IllegalArgumentException("Illegal name of environment variable: '$name'")
      if (!isValidValue(value)) throw IllegalArgumentException("Illegal value of environment variable value '$name': '$value'")
    }

    val commandLine = mutableListOf<String>()
    commandLine += exePath.replace('/', File.separatorChar).replace('\\', File.separatorChar)

    for (param in parameters) {
      val parameter = unquoteString(param)

      commandLine += if (parameter.isBlank()) {
        quoteQuote
      } else {
        backslashEscapeQuotes(parameter)
      }
    }

    thisLogger().info(commandLine.joinToString(" "))
    return commandLine
  }

  private fun isValidName(name: String): Boolean =
    name.isNotEmpty() && name.indexOf('\u0000') == -1 && name.indexOf('=', 1) == -1

  private fun isValidValue(value: String): Boolean = value.indexOf('\u0000') == -1

  private fun unquoteString(s: String): String {
    val quoted = s.length > 1 && inescapableQuote == s[0] && inescapableQuote == s[s.length - 1]
    return if (quoted) s.substring(1, s.length - 1) else s
  }

  private fun backslashEscapeQuotes(s: String): String {
    val result = winBackslashesPrecedingQuote.matcher(s).replaceAll("$1$1").replace("\"", "\\\"")
    return if (result != s || winQuoteSpecial.matcher(s).find()) quote(result) else result
  }

  private fun quote(s: String): String =
    if (s.length >= 2 && s[0] == quote && s[s.length - 1] == quote) s else "$quote$s$quote"

  private fun setupEnvironment(environment: MutableMap<String, String>) {
    environment.clear()

    if (passParentEnvs) environment += System.getenv()
    environment += this.environment
  }
}
