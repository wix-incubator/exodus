package com.wix.bazel.migrator.overrides

case class OverrideParsingException(message:String,cause:Throwable) extends RuntimeException(message,cause)
