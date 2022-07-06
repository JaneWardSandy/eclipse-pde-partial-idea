package cn.varsa.idea.pde.partial.launcher.control

import ch.qos.logback.classic.*
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.encoder.*
import ch.qos.logback.classic.spi.*
import ch.qos.logback.core.*
import org.slf4j.*
import tornadofx.*

class LoggerControl : Controller() {
  private val configControl: ConfigControl by inject()
  private val context = LoggerFactory.getILoggerFactory() as LoggerContext
  val logger: Logger = context.getLogger("original-output")

  var layoutEncoder: PatternLayoutEncoder? = null
  var fileAppender: FileAppender<ILoggingEvent>? = null

  fun initialAppender() {
    destroyAppender()

    layoutEncoder = PatternLayoutEncoder().apply {
      pattern = "%m"
      context = this@LoggerControl.context
      charset = configControl.ideaCharset

      start()
    }

    fileAppender = FileAppender<ILoggingEvent>().apply {
      context = this@LoggerControl.context
      isAppend = true
      file = "${configControl.projectRoot}/out/log/partial.log"
      encoder = layoutEncoder

      start()
    }

    logger.addAppender(fileAppender)
  }

  fun destroyAppender() {
    fileAppender?.also {
      logger.detachAppender(it)
      it.stop()
    }
    fileAppender = null

    layoutEncoder?.stop()
    layoutEncoder = null
  }
}
