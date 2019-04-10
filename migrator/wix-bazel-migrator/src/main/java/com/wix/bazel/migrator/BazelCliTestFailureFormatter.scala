package com.wix.bazel.migrator

import scala.collection.JavaConverters._

object BazelCliTestFailureFormatter extends App {

  case class Accumulator(mergedLines: List[String] = Nil, remainder: String = "", index: Int = 0)

  private val lines = java.nio.file.Files.readAllLines(java.nio.file.Paths.get("/Users/ittaiz/failures.log")).asScala.toList
  val accumulated = lines.foldLeft(Accumulator()) { case (accumulator, currentLine) =>
    if (accumulator.index % 2 == 1)
      accumulator.copy(accumulator.mergedLines :+ accumulator.remainder + "," + currentLine, "", accumulator.index + 1)
    else
      accumulator.copy(remainder = currentLine, index = accumulator.index + 1)
  }
  java.nio.file.Files.write(java.nio.file.Paths.get("/Users/ittaiz/failuresMerged.log"), accumulated.mergedLines.sorted.asJava)
}
