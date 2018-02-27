package com.wix.bazel.migrator.workspace

import scala.sys.process.ProcessLogger


private [workspace] class StringProcessLogger extends ProcessLogger {
  private val messages = new StringBuilder

  def lines = messages.toString

  def buffer[T](f: => T): T = {
    clear()
    f
  }

  def out(s: => String) = {
    System.out.println(s)
    messages.append(s + System.lineSeparator)
  }

  def err(s: => String) = {
    System.err.println(s)
    messages.append(s + System.lineSeparator)
  }

  def clear() = messages.clear
}
