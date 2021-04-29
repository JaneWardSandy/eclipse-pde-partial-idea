package cn.varsa.idea.pde.partial.launcher.support

import org.slf4j.*

inline fun <reified T : Any> T.thisLogger(): Logger = LoggerFactory.getLogger(T::class.java)
inline fun <reified T : Any> logger(): Logger = LoggerFactory.getLogger(T::class.java)
