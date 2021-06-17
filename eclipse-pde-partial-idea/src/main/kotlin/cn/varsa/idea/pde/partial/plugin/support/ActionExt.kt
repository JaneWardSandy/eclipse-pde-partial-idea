package cn.varsa.idea.pde.partial.plugin.support

import com.intellij.openapi.application.*
import com.intellij.openapi.util.*
import com.intellij.util.*

fun <T> readCompute(action: ThrowableComputable<T, out Exception>): T = ReadAction.compute(action)
fun readRun(action: ThrowableRunnable<out Exception>) = ReadAction.run(action)

fun <T> writeCompute(action: ThrowableComputable<T, out Exception>): T = WriteAction.compute(action)
fun writeRun(action: ThrowableRunnable<out Exception>) = WriteAction.run(action)

fun applicationInvokeAndWait(runnable: Runnable) = ApplicationManager.getApplication().invokeAndWait(runnable)
fun applicationInvokeLater(runnable: Runnable) = ApplicationManager.getApplication().invokeLater(runnable)
