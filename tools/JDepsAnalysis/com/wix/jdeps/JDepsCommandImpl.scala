package com.wix.jdeps

import java.nio.file.{Files, Path, Paths}

class JDepsCommandImpl(repoRoot: Path) extends JDepsCommand {

  override def analyzeClassesDependenciesPerJar(jarPath: String, classPath: List[String]): ClassDependencies = {
    val fileName = Paths.get(jarPath).getFileName.toString
    val dotDirectory = Files.createTempDirectory("dot")
    val classpath = classPath.mkString(":")
    val process = (new ProcessBuilder).directory(repoRoot.toFile).command(
      "jdeps",
      "-dotoutput",
      dotDirectory.toString,
      "-v",
      "-cp",
      classpath,
      jarPath
    )
    process.start().waitFor()
    ClassDependencies(dotDirectory.resolve(fileName + ".dot"))
  }
}