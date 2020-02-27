package com.wix.jdeps

import java.nio.file.Path

trait ProcessRunner {
  def run(runningDirectory: Path, command: String, args: List[String]): RunResult
}

class JavaProcessRunner extends ProcessRunner {
  override def run(runningDirectory: Path, command: String, args: List[String]): RunResult = {
    val process = (new ProcessBuilder).directory(runningDirectory.toFile).command((command :: args):_*)
    val process1 = process.start()
    process1.waitFor()
    val stdOut = scala.io.Source.fromInputStream(process1.getInputStream).mkString
    val stdErr = scala.io.Source.fromInputStream(process1.getErrorStream).mkString
    RunResult(
      exitCode =  process1.exitValue(),
      stdOut = stdOut,
      stdErr = stdErr
    )
  }
}

case class RunResult(exitCode: Int, stdOut: String, stdErr: String)