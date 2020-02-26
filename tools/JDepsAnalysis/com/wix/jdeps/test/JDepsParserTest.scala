package com.wix.jdeps.test

import java.nio.file.Files

import com.wix.jdeps.{ClassDependencies, JDepsParser, JVMClass}
import org.specs2.mutable.SpecificationWithJUnit

class JDepsParserTest extends SpecificationWithJUnit {
  "JDepsParser" should {
    "convert successfully the following file" in {
      val jdepsOutput = Files.createTempFile("jdeps-output", ".txt")



      val parser = new JDepsParser {
        override def convert(deps: ClassDependencies): Map[JVMClass, Set[JVMClass]] = {
          Map.empty
        }
      }
      parser.convert(ClassDependencies(jdepsOutput)) mustEqual
    }
  }
}
