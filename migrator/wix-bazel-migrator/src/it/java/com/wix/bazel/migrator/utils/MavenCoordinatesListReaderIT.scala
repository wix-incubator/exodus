package com.wix.bazel.migrator.utils

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, NoSuchFileException, Path}

import com.github.marschall.memoryfilesystem.MemoryFileSystemBuilder
import com.wixpress.build.maven.MavenMakers.someCoordinates
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope

//noinspection TypeAnnotation
class MavenCoordinatesListReaderIT extends SpecificationWithJUnit{
  "MavenCoordinatesListReader" should {
    "read file with coordinates" in new Ctx{
      val coordinatesA = someCoordinates("a")
      val coordinatesB = someCoordinates("b")
      val fileContent = s"""${coordinatesA.serialized}
                            |${coordinatesB.serialized}""".stripMargin
      val filePath:Path = fileWithContent(fileContent)

      MavenCoordinatesListReader.coordinatesIn(filePath) mustEqual Set(coordinatesA,coordinatesB)
    }

    "ignore empty line" in new Ctx{
      val coordinatesA = someCoordinates("a")
      val coordinatesB = someCoordinates("b")
      val fileContent = s"""${coordinatesA.serialized}
                           |
                           |${coordinatesB.serialized}""".stripMargin
      val filePath:Path = fileWithContent(fileContent)

      MavenCoordinatesListReader.coordinatesIn(filePath) mustEqual Set(coordinatesA,coordinatesB)
    }

    "ignore preceding and trailing spaces" in new Ctx{
      val coordinatesA = someCoordinates("a")
      val coordinatesB = someCoordinates("b")
      val fileContent = s"    ${coordinatesA.serialized}   "
      val filePath:Path = fileWithContent(fileContent)

      MavenCoordinatesListReader.coordinatesIn(filePath) mustEqual Set(coordinatesA)
    }

    "ignore lines that starts with #" in new Ctx{
      val coordinatesA = someCoordinates("a")
      val coordinatesB = someCoordinates("b")
      val fileContent = s"""${coordinatesA.serialized}
                            |#${coordinatesB.serialized}""".stripMargin
      val filePath:Path = fileWithContent(fileContent)

      MavenCoordinatesListReader.coordinatesIn(filePath) mustEqual Set(coordinatesA)
    }

    "throw exception in case file is missing" in new Ctx{
      MavenCoordinatesListReader.coordinatesIn(fs.getPath("non-existing-file")) must throwA[NoSuchFileException]
    }
  }

  trait Ctx extends Scope{
    val fs = MemoryFileSystemBuilder.newLinux().build()
    def fileWithContent(content:String):Path = {
      val path = Files.createTempFile(fs.getPath("/"),"",".txt")
      Files.write(path, content.getBytes(StandardCharsets.UTF_8))
    }
  }

}
