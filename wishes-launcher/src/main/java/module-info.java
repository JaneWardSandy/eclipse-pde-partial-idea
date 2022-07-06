module Wishes {
  requires kotlin.reflect;

  requires javafx.controls;
  requires tornadofx;
  requires org.slf4j;
  requires java.logging;
  requires ch.qos.logback.classic;
  requires ch.qos.logback.core;
  requires jul.to.slf4j;
  requires jdk.charsets;
  requires java.rmi;
  requires java.naming;

  uses org.slf4j.spi.SLF4JServiceProvider;

  exports cn.varsa.idea.pde.partial.launcher;
  exports cn.varsa.idea.pde.partial.launcher.command;
  exports cn.varsa.idea.pde.partial.launcher.control;
  exports cn.varsa.idea.pde.partial.launcher.io;
  exports cn.varsa.idea.pde.partial.launcher.service;
  exports cn.varsa.idea.pde.partial.launcher.support;
  exports cn.varsa.idea.pde.partial.launcher.view;
}
