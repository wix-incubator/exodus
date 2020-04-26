package com.wix.bazel.migrator.analyze.jdk

import java.nio.file.{Files, Path, Paths}


class JDepsCommandImpl(repoRoot: Path) extends JDepsCommand {

  override def analyzeClassesDependenciesPerJar(jarPath: String, classPath: List[String]): Option[ClassDependencies] = {
    val fileName = Paths.get(jarPath).getFileName.toString
    val dotDirectory = Files.createTempDirectory("dot")
    val classpath = classPath.mkString(":")
    val cmdArgs = List("jdeps",
      "-dotoutput",
      dotDirectory.toString,
      "-v",
      "-cp",
      classpath,
      jarPath)
    val process = (new ProcessBuilder).directory(repoRoot.toFile).command(cmdArgs:_*)
    process.redirectOutput()
    val process1 = process.start()
    val stream = process1.getInputStream
    process1.waitFor()
    val path = dotDirectory.resolve(fileName + ".dot")
    if (Files.exists(path)) Some(ClassDependencies(path)) else None
  }
}