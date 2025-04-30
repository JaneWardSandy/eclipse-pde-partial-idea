package cn.varsa.idea.pde.partial.plugin.run

import cn.varsa.idea.pde.partial.plugin.i18n.EclipsePDEPartialBundles.message
import com.intellij.execution.ExecutionResult
import com.intellij.openapi.util.Key
import java.net.ServerSocket
import java.net.Socket
import java.util.ArrayList
import java.util.Scanner
import kotlin.concurrent.thread
import kotlin.text.startsWith
import kotlin.text.substring

/**
 * Client for remote plugin test runner
 */
class RemoteTestRunnerClient {
  private var serverSocket: ServerSocket? = null

  private var testExecuted = 0
  private var testErrors = 0
  private var testFailures = 0
  private val expectedBuffer = StringBuilder()
  private val actualBuffer = StringBuilder()
  private val traceBuffer = StringBuilder()
  private var currentTestIdentifier = ""
  private var currentTestError = false
  private var currentTestFail = false
  private var elapsedTime = 0

  /**
   * Create server socket
   * @return local port
   */
  fun createServerSocket(): Int {
    if (serverSocket == null)
      serverSocket = ServerSocket(0)
    return serverSocket!!.localPort
  }

  private fun convertTestIdentifier(identifier: String): String {
    // 1. Split the string by the first comma
    val parts = identifier.split(',', limit = 2)

    // Check if we got exactly two parts (id and method(class))
    if (parts.size != 2) {
      // Throw an exception instead of returning null
      throw IllegalArgumentException(message("remote.test.runner.client.invalidTestIdentifier", identifier))
    }

    val methodAndClass = parts[1] // This is "methodName(className)"

    // 2. Find the parentheses to extract method name and class name
    val openParenIndex = methodAndClass.indexOf('(')
    val closeParenIndex = methodAndClass.indexOf(')')

    // Check if parentheses are present and in the correct order
    if (openParenIndex == -1 || closeParenIndex == -1 || openParenIndex >= closeParenIndex) {
      // Throw an exception instead of returning null
      throw IllegalArgumentException(message("remote.test.runner.client.invalidMethodClassFormat", methodAndClass, identifier))
    }

    // 3. Extract method name and class name using substring
    val methodName = methodAndClass.substring(0, openParenIndex)
    val className = methodAndClass.substring(openParenIndex + 1, closeParenIndex)

    // 4. Construct the desired output string
    return "$className.$methodName()"
  }

  /**
   * Start background monitoring of remote test runner
   */
  fun start(executionResult : ExecutionResult) {
    if (serverSocket != null) {
      val key = Key.create<String>("RemoteTestRunnerClient")
      thread {
        val failures = ArrayList<Map<String, String>>()
        val errors = ArrayList<Map<String, String>>()

        val client = serverSocket?.accept()
        if (client != null) {
          val reader = Scanner(client.getInputStream())
          while (serverSocket != null) {
            val message = reader.nextLine()
            if (message == null || message == "") {
              continue
            } else {
              if (message.startsWith(MessageIds.TEST_START)) {
                onTestStart(message, executionResult, key)
              } else if (message.startsWith(MessageIds.TEST_END)) {
                onTestEnd(failures, errors)
              } else if (message.startsWith(MessageIds.TEST_ERROR)) {
                onTestError()
              } else if (message.startsWith(MessageIds.TEST_FAILED)) {
                onTestFailed()
              } else if (message.startsWith(MessageIds.EXPECTED_START)) {
                onExpectedStart(reader)
              } else if (message.startsWith(MessageIds.ACTUAL_START)) {
                onActualStart(reader)
              } else if (message.startsWith(MessageIds.TRACE_START)) {
                onTraceStart(reader)
              } else if (message.startsWith(MessageIds.TEST_RUN_END)) {
                onTestRunEnd(message, executionResult, key, errors, failures, client)
              }
            }
          }
        }
      }
    }
  }

  private fun onTestStart(
    message: String, executionResult: ExecutionResult, key: Key<String>
  ) {
    currentTestIdentifier = convertTestIdentifier(message.substring(MessageIds.MSG_HEADER_LENGTH))
    executionResult.processHandler.notifyTextAvailable(
      "${message("remote.test.runner.client.testStart", currentTestIdentifier)} \n", key
    )
  }

  private fun onTestEnd(
    failures: ArrayList<Map<String, String>>, errors: ArrayList<Map<String, String>>
  ) {
    testExecuted++
    if (currentTestFail) {
      val map = mapOf(
        "testIdentifier" to currentTestIdentifier,
        "expected" to expectedBuffer.toString(),
        "actual" to actualBuffer.toString(),
        "trace" to traceBuffer.toString()
      )
      failures.add(map)
    } else if (currentTestError) {
      val map = mapOf(
        "testIdentifier" to currentTestIdentifier,
        "expected" to expectedBuffer.toString(),
        "actual" to actualBuffer.toString(),
        "trace" to traceBuffer.toString()
      )
      errors.add(map)
    }
    currentTestIdentifier = ""
    currentTestFail = false
    currentTestError = false
    expectedBuffer.clear()
    actualBuffer.clear()
    traceBuffer.clear()
  }

  private fun onTestFailed() {
    currentTestFail = true
    testFailures++
  }

  private fun onTestError() {
    currentTestError = true
    testErrors++
  }

  private fun onActualStart(reader: Scanner) {
    actualBuffer.clear()
    while(true) {
      val message = reader.nextLine()
      if (message == null)
        break
      if (message.startsWith(MessageIds.ACTUAL_END))
        break
      if (actualBuffer.isNotEmpty()) actualBuffer.append("\n")
      actualBuffer.append(message)
    }
  }

  private fun onExpectedStart(reader: Scanner) {
    expectedBuffer.clear()
    while(true) {
      val message = reader.nextLine()
      if (message == null)
        break
      if (message.startsWith(MessageIds.EXPECTED_END))
        break
      if (expectedBuffer.isNotEmpty()) expectedBuffer.append("\n")
      expectedBuffer.append(message)
    }
  }

  private fun onTraceStart(reader: Scanner) {
    traceBuffer.clear()
    while (true) {
      val message = reader.nextLine()
      if (message == null)
        break
      if (message.startsWith(MessageIds.TRACE_END))
        break
      if (traceBuffer.isNotEmpty()) traceBuffer.append("\n")
      traceBuffer.append(message)
    }
  }

  private fun onTestRunEnd(
    message: String,
    executionResult: ExecutionResult,
    key: Key<String>,
    errors: ArrayList<Map<String, String>>,
    failures: ArrayList<Map<String, String>>,
    client: Socket
  ) {
    try {
      val elapsedText = message.substring(MessageIds.MSG_HEADER_LENGTH)
      elapsedTime = Integer.parseInt(elapsedText)
      executionResult.processHandler.notifyTextAvailable(
        "\n${message("remote.test.runner.client.summary", testExecuted, testErrors, testFailures)} \n", key
      )
      executionResult.processHandler.notifyTextAvailable("${message("remote.test.runner.client.elapsedTime", elapsedTime.toString())} \n", key)
      if (errors.isNotEmpty()) {
        executionResult.processHandler.notifyTextAvailable(
          "\nErrors:\n", key
        )
        errors.forEach {
          executionResult.processHandler.notifyTextAvailable(
            "* TestIdentifier=${it["testIdentifier"]}\n", key
          )
          executionResult.processHandler.notifyTextAvailable(
            "** Expected:\n${it["expected"]}\n", key
          )
          executionResult.processHandler.notifyTextAvailable(
            "** Actual:\n${it["actual"]}\n", key
          )
          executionResult.processHandler.notifyTextAvailable(
            "** Trace:\n${it["trace"]}\n\n", key
          )
        }
      }
      if (failures.isNotEmpty()) {
        executionResult.processHandler.notifyTextAvailable(
          "\nFailures:\n", key
        )
        failures.forEach {
          executionResult.processHandler.notifyTextAvailable(
            "* TestIdentifier=${it["testIdentifier"]}\n", key
          )
          executionResult.processHandler.notifyTextAvailable(
            "** Expected:\n${it["expected"]}\n", key
          )
          executionResult.processHandler.notifyTextAvailable(
            "** Actual:\n${it["actual"]}\n", key
          )
          executionResult.processHandler.notifyTextAvailable(
            "** Trace:\n${it["trace"]}\n\n", key
          )
        }
      }
    } finally {
      serverSocket = null
      client.close()
    }
  }
}