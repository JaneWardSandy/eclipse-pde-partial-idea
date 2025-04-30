package cn.varsa.idea.pde.partial.plugin.run

/**
 * Constants for message IDs used in a test communication protocol.
 * Converted from org.eclipse.jdt.internal.junit.runner.MessageIds
 */
class MessageIds private constructor() { // Private constructor to prevent instantiation

  companion object {
    /**
     * The header length of a message, all messages
     * have a fixed header length
     */
    const val MSG_HEADER_LENGTH = 8

    /**
     * Notification that a test trace has started.
     * The end of the trace is signaled by a TRACE_END
     * message. In between the TRACE_START and TRACE_END
     * the stack trace is submitted as multiple lines.
     */
    const val TRACE_START = "%TRACES "
    /**
     * Notification that a trace ends.
     */
    const val TRACE_END = "%TRACEE "
    /**
     * Notification that the expected result has started.
     * The end of the expected result is signaled by a Trace_END.
     */
    const val EXPECTED_START = "%EXPECTS"
    /**
     * Notification that an expected result ends.
     */
    const val EXPECTED_END = "%EXPECTE"
    /**
     * Notification that the actual result has started.
     * The end of the actual result is signaled by a Trace_END.
     */
    const val ACTUAL_START = "%ACTUALS"
    /**
     * Notification that an actual result ends.
     */
    const val ACTUAL_END = "%ACTUALE"
    /**
     * Notification that a trace for a reran test has started.
     * The end of the trace is signaled by a RTrace_END
     * message.
     */
    const val RTRACE_START = "%RTRACES"
    /**
     * Notification that a trace of a reran trace ends.
     */
    const val RTRACE_END = "%RTRACEE"
    /**
     * Notification that a test run has started.
     * MessageIds.TEST_RUN_START + testCount.toString + " " + version
     */
    const val TEST_RUN_START = "%TESTC  "
    /**
     * Notification that a test has started.
     * MessageIds.TEST_START + testID + "," + testName
     */
    const val TEST_START = "%TESTS  "
    /**
     * Notification that a test has ended.
     * TEST_END + testID + "," + testName
     */
    const val TEST_END = "%TESTE  "
    /**
     * Notification that a test had an error.
     * TEST_ERROR + testID + "," + testName.
     * After the notification follows the stack trace.
     */
    const val TEST_ERROR = "%ERROR  "
    /**
     * Notification that a test had a failure.
     * TEST_FAILED + testID + "," + testName.
     * After the notification follows the stack trace.
     */
    const val TEST_FAILED = "%FAILED "
    /**
     * Notification that a test run has ended.
     * TEST_RUN_END + elapsedTime.toString().
     */
    const val TEST_RUN_END = "%RUNTIME"
    /**
     * Notification that a test run was successfully stopped.
     */
    const val TEST_STOPPED = "%TSTSTP "
    /**
     * Notification that a test was reran.
     * TEST_RERAN + testId + " " + testClass + " " + testName + STATUS.
     * Status = "OK" or "FAILURE".
     */
    const val TEST_RERAN = "%TSTRERN"

    /**
     * Notification about a test inside the test suite. <br>
     * TEST_TREE + testId + "," + testName + "," + isSuite + "," + testcount + "," + isDynamicTest +
     * "," + parentId + "," + displayName + "," + parameterTypes + "," + uniqueId <br>
     * isSuite = "true" or "false" <br>
     * isDynamicTest = "true" or "false" <br>
     * parentId = the unique id of its parent if it is a dynamic test, otherwise can be "-1" <br>
     * displayName = the display name of the test <br>
     * parameterTypes = comma-separated list of method parameter types if applicable, otherwise an
     * empty string <br>
     * uniqueId = the unique ID of the test provided by JUnit launcher, otherwise an empty string
     * <br>
     * See: ITestRunListener2#testTreeEntry
     */
    const val TEST_TREE = "%TSTTREE"
    /**
     * Request to stop the current test run.
     */
    const val TEST_STOP = ">STOP    "
    /**
     * Request to rerun a test.
     * TEST_RERUN + testId + " " + testClass + " "+testName
     */
    const val TEST_RERUN = ">RERUN  "

    /**
     * MessageFormat to encode test method identifiers:
     * testMethod(testClass)
     */
    const val TEST_IDENTIFIER_MESSAGE_FORMAT = "{0}({1})"

    /**
     * Test identifier prefix for ignored tests.
     */
    const val IGNORED_TEST_PREFIX = "@Ignore: "

    /**
     * Test identifier prefix for tests with assumption failures.
     */
    const val ASSUMPTION_FAILED_TEST_PREFIX = "@AssumptionFailure: "
  }
}